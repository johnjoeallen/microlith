package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import io.macroservice.platform.support.ModuleSettings;
import io.macroservice.platform.support.testing.SharedPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The same module code runs on one instance with eight schemas, on two instances, and with the production role model. */
class PersistenceLayoutTests {

    private final TestApps apps = new TestApps();

    @AfterEach
    void close() {
        apps.close();
    }

    private static int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int tables(PostgreSQLContainer container, String schema, String table) throws SQLException {
        try (Connection c = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword())) {
            return count(c, "select count(*) from information_schema.tables where table_schema = '%s' and table_name = '%s'".formatted(schema, table));
        }
    }

    private static String schemaOf(ConfigurableApplicationContext context, String module) {
        return context.getBean(module + "Settings", ModuleSettings.class).datasource().schema();
    }

    private static void placeAnOrder(ConfigurableApplicationContext context) {
        OrdersApi orders = context.getBean(OrdersApi.class);
        List<OrderLine> lines = List.of(new OrderLine("sku-widget", 2));
        Order order = orders.placeOrder("req-1", "cust-standard", lines);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.placeOrder("req-1", "cust-standard", lines)).isEqualTo(order);
        assertThat(orders.getOrder(order.orderId())).isEqualTo(order);
    }

    @Test
    void oneInstanceEightSchemas() throws Exception {
        ConfigurableApplicationContext context = apps.start(false, TestApps.allLocal());
        placeAnOrder(context);
        PostgreSQLContainer db = SharedPostgres.INSTANCE;
        assertThat(TestApps.MODULES.stream().map(m -> schemaOf(context, m)).distinct()).hasSize(8);
        assertThat(tables(db, schemaOf(context, "orders"), "orders")).isEqualTo(1);
        assertThat(tables(db, schemaOf(context, "orders"), "outbox")).isEqualTo(1);
        assertThat(tables(db, schemaOf(context, "orders"), "stock")).isZero();
        assertThat(tables(db, schemaOf(context, "inventory"), "stock")).isEqualTo(1);
        assertThat(tables(db, schemaOf(context, "inventory"), "orders")).isZero();
        assertThat(tables(db, schemaOf(context, "catalog"), "product")).isEqualTo(1);
        for (String module : TestApps.MODULES) {
            assertThat(tables(db, schemaOf(context, module), "flyway_schema_history")).as(module).isEqualTo(1);
        }
    }

    @Test
    void twoInstancesOnlyConfigurationChanges() throws Exception {
        PostgreSQLContainer first = SharedPostgres.newContainer();
        PostgreSQLContainer second = SharedPostgres.newContainer();
        try {
            ConfigurableApplicationContext context = apps.start(false, TestApps.demo(), TestApps.topology(Map.of(), TestApps.MODULES.toArray(String[]::new)),
                    SharedPostgres.args(first, "orders", "pricing", "payments", "notifications"),
                    SharedPostgres.args(second, "catalog", "customers", "inventory", "shipping"));
            placeAnOrder(context);
            assertThat(tables(first, schemaOf(context, "orders"), "orders")).isEqualTo(1);
            assertThat(tables(first, schemaOf(context, "inventory"), "stock")).isZero();
            assertThat(tables(second, schemaOf(context, "inventory"), "stock")).isEqualTo(1);
            assertThat(tables(second, schemaOf(context, "orders"), "orders")).isZero();
        } finally {
            apps.close();
            first.stop();
            second.stop();
        }
    }

    @Test
    void runtimeRolesSeeOnlyTheirOwnSchema() throws Exception {
        PostgreSQLContainer db = SharedPostgres.newContainer("db/bootstrap.sql");
        try {
            List<String> args = new ArrayList<>();
            for (String m : TestApps.MODULES) {
                String p = "--modules." + m + ".datasource.";
                args.addAll(List.of(p + "url=" + db.getJdbcUrl(), p + "username=" + m + "_app", p + "password=" + m + "_app",
                        p + "migration-username=" + m + "_migrator", p + "migration-password=" + m + "_migrator"));
            }
            ConfigurableApplicationContext context = apps.start(false, TestApps.demo(), TestApps.topology(Map.of(), TestApps.MODULES.toArray(String[]::new)),
                    args.toArray(String[]::new));
            placeAnOrder(context);

            // every runtime role reads its own schema and none of the others'
            for (String own : TestApps.MODULES) {
                try (Connection c = DriverManager.getConnection(db.getJdbcUrl(), own + "_app", own + "_app")) {
                    for (String other : TestApps.MODULES) {
                        if (!other.equals(own)) {
                            assertThatThrownBy(() -> c.createStatement().executeQuery("select * from " + other + ".flyway_schema_history"))
                                    .as("%s_app reading %s", own, other).isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
                        }
                    }
                    assertThat(count(c, "select count(*) from flyway_schema_history")).isPositive();
                    assertThatThrownBy(() -> c.createStatement().execute("create table " + own + ".extra (id int)"))
                            .isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
                }
            }
        } finally {
            apps.close();
            db.stop();
        }
    }
}
