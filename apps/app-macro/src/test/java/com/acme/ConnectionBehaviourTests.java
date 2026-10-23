package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.StockLevel;
import com.acme.orders.api.OrderNotFoundException;
import java.util.Map;
import com.acme.orders.api.OrdersApi;
import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcException;
import io.macroservice.platform.support.ModulesHealthIndicator;
import io.macroservice.platform.support.testing.SharedPostgres;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Connection budget behaviour: lazy pools, nothing held while idle, tolerance of a missing database, bulkhead, metrics. */
class ConnectionBehaviourTests {

    private final TestApps apps = new TestApps();

    @AfterEach
    void close() {
        apps.close();
    }

    private static int connectionsOf(PostgreSQLContainer db, String applicationName) throws SQLException {
        try (Connection c = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
                Statement statement = c.createStatement();
                ResultSet rs = statement.executeQuery("select count(*) from pg_stat_activity where application_name = '" + applicationName + "'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void aDisabledModuleOpensNoConnectionsAndHasNoPool() throws Exception {
        PostgreSQLContainer db = SharedPostgres.newContainer();
        try {
            String[] others = TestApps.MODULES.stream().filter(m -> !m.equals("inventory")).toArray(String[]::new);
            ConfigurableApplicationContext context = apps.start(false, TestApps.demo(), TestApps.topology(Map.of(), others),
                    SharedPostgres.args(db, others));
            assertThat(context.containsBean("inventoryDataSource")).isFalse();
            assertThat(context.containsBean("inventoryMigration")).isFalse();
            assertThat(context.containsBean("ordersDataSource")).isTrue();
            assertThat(connectionsOf(db, "macro-inventory")).isZero();
            assertThat(connectionsOf(db, "macro-inventory-migration")).isZero();
            assertThatThrownBy(() -> context.getBean(OrdersApi.class).getOrder("nope")).isInstanceOf(OrderNotFoundException.class);
            assertThat(connectionsOf(db, "macro-inventory")).isZero();
        } finally {
            apps.close();
            db.stop();
        }
    }

    @Test
    void anIdleModuleGivesItsConnectionsBack() throws Exception {
        PostgreSQLContainer db = SharedPostgres.newContainer();
        try {
            ConfigurableApplicationContext context = apps.start(false, TestApps.demo(), TestApps.topology(Map.of(), TestApps.MODULES.toArray(String[]::new)),
                    SharedPostgres.args(db, TestApps.MODULES.toArray(String[]::new)),
                    TestApps.args("--modules.orders.datasource.idle-timeout=10s", "--modules.inventory.datasource.idle-timeout=10s"));
            // migrations are finished and closed; the runtime pools (minimum-idle 0) have not opened anything yet
            assertThat(connectionsOf(db, "macro-orders")).isZero();
            assertThat(connectionsOf(db, "macro-inventory")).isZero();

            assertThatThrownBy(() -> context.getBean(OrdersApi.class).getOrder("nope")).isInstanceOf(OrderNotFoundException.class);
            assertThat(context.getBean(InventoryApi.class).getStock("sku-widget")).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
            assertThat(connectionsOf(db, "macro-orders") + connectionsOf(db, "macro-inventory")).isPositive();

            await().atMost(Duration.ofSeconds(40)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                    assertThat(connectionsOf(db, "macro-orders") + connectionsOf(db, "macro-inventory")).isZero());
        } finally {
            apps.close();
            db.stop();
        }
    }

    @Test
    void anUnreachableDatabaseDoesNotStopStartupAndRecoversWhenItAppears() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        PostgreSQLContainer db = new PostgreSQLContainer(SharedPostgres.IMAGE);
        try {
            ConfigurableApplicationContext context = apps.start(false, TestApps.demo(), TestApps.topology(Map.of(), "orders"), TestApps.args(
                    "--modules.orders.datasource.url=jdbc:postgresql://localhost:" + port + "/test",
                    "--modules.orders.datasource.username=test", "--modules.orders.datasource.password=test",
                    "--modules.orders.datasource.migration-username=test", "--modules.orders.datasource.migration-password=test",
                    "--platform.db.migration-retry-interval=500ms"));
            ModulesHealthIndicator health = context.getBean(ModulesHealthIndicator.class);
            assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
            assertThat(health.health().getDetails().get("orders").toString()).startsWith("not ready");

            db.setPortBindings(List.of(port + ":5432"));
            db.start();
            await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                    assertThat(health.health().getStatus().getCode()).isEqualTo("UP"));
            assertThatThrownBy(() -> context.getBean(OrdersApi.class).getOrder("nope")).isInstanceOf(OrderNotFoundException.class);
        } finally {
            apps.close();
            db.stop();
        }
    }

    @Test
    void theBulkheadRejectsExcessCallsWhileThePoolIsExhaustedAndNothingDeadlocks() throws Exception {
        ConfigurableApplicationContext context = apps.start(false, TestApps.allLocal(), TestApps.args(
                "--modules.inventory.binding=loopback-codec", "--modules.inventory.datasource.max-pool-size=2",
                "--modules.inventory.datasource.connection-timeout=4s", "--modules.inventory.bulkhead.max-wait=300ms"));
        InventoryApi inventory = context.getBean(InventoryApi.class);
        DataSource pool = context.getBean("inventoryDataSource", DataSource.class);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Connection first = pool.getConnection();
        Connection second = pool.getConnection();
        try {
            Future<StockLevel> one = executor.submit(() -> inventory.getStock("sku-widget"));
            Future<StockLevel> two = executor.submit(() -> inventory.getStock("sku-widget"));
            Thread.sleep(500); // both calls hold a permit and wait for a connection

            long begin = System.nanoTime();
            assertThatThrownBy(() -> inventory.getStock("sku-widget")).isInstanceOf(RpcException.class)
                    .hasMessageContaining("Module 'inventory' is overloaded").extracting(e -> ((RpcException) e).code())
                    .isEqualTo(RpcErrorCodes.OVERLOADED);
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin)).isLessThan(2000);

            first.close();
            second.close();
            assertThat(one.get(5, TimeUnit.SECONDS)).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
            assertThat(two.get(5, TimeUnit.SECONDS)).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
            assertThat(inventory.getStock("sku-widget")).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
        } finally {
            first.close();
            second.close();
            executor.shutdownNow();
        }
    }

    @Test
    void poolMetricsAreTaggedByModule() throws Exception {
        ConfigurableApplicationContext context = apps.start(false, TestApps.allLocal(), TestApps.args(
                "--modules.inventory.binding=loopback-codec", "--modules.inventory.datasource.max-pool-size=1",
                "--modules.inventory.datasource.connection-timeout=500ms"));
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        for (String module : List.of("orders", "inventory")) {
            for (String state : List.of("active", "idle", "pending", "total")) {
                assertThat(registry.find("macro.db.pool.connections").tag("module", module).tag("state", state).gauge())
                        .as("%s %s", module, state).isNotNull();
            }
        }
        DataSource pool = context.getBean("inventoryDataSource", DataSource.class);
        try (Connection held = pool.getConnection()) {
            System.gc();
            assertThat(registry.get("macro.db.pool.connections").tag("module", "inventory").tag("state", "active").gauge().value())
                    .isEqualTo(1.0); // a real number, not NaN from a collected weak reference
            assertThatThrownBy(() -> context.getBean(InventoryApi.class).getStock("sku-widget")).isInstanceOf(RuntimeException.class);
        }
        assertThat(registry.get("macro.db.pool.timeouts").tag("module", "inventory").counter().count()).isGreaterThanOrEqualTo(1);
        assertThat(registry.get("macro.db.pool.timeouts").tag("module", "orders").counter().count()).isZero();
    }
}
