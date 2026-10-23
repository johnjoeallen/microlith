package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.api.Order;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.orders.api.PlaceOrderRequest;
import io.macroservice.platform.support.ModuleSettings;
import io.macroservice.platform.support.testing.SharedPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The same module code runs on one instance with two schemas, on two instances, and with the production role model. */
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
        Order order = orders.placeOrder(new PlaceOrderRequest("req-1", "cust-1", "sku-1", 2));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.placeOrder(new PlaceOrderRequest("req-1", "cust-1", "sku-1", 2))).isEqualTo(order);
        assertThat(orders.getOrder(order.orderId())).isEqualTo(order);
    }

    @Test
    void oneInstanceTwoSchemas() throws Exception {
        ConfigurableApplicationContext context = apps.start(false, TestApps.ALL_LOCAL, SharedPostgres.args("orders", "inventory"));
        placeAnOrder(context);
        PostgreSQLContainer db = SharedPostgres.INSTANCE;
        String orders = schemaOf(context, "orders");
        String inventory = schemaOf(context, "inventory");
        assertThat(orders).isNotEqualTo(inventory);
        assertThat(tables(db, orders, "orders")).isEqualTo(1);
        assertThat(tables(db, orders, "stock")).isZero();
        assertThat(tables(db, inventory, "stock")).isEqualTo(1);
        assertThat(tables(db, inventory, "orders")).isZero();
        assertThat(tables(db, orders, "flyway_schema_history")).isEqualTo(1);
        assertThat(tables(db, inventory, "flyway_schema_history")).isEqualTo(1);
    }

    @Test
    void twoInstancesOnlyConfigurationChanges() throws Exception {
        PostgreSQLContainer first = SharedPostgres.newContainer();
        PostgreSQLContainer second = SharedPostgres.newContainer();
        try {
            ConfigurableApplicationContext context = apps.start(false, TestApps.ALL_LOCAL,
                    SharedPostgres.args(first, "orders"), SharedPostgres.args(second, "inventory"));
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
            String url = db.getJdbcUrl();
            ConfigurableApplicationContext context = apps.start(false, TestApps.ALL_LOCAL, TestApps.args(
                    "--modules.orders.datasource.url=" + url, "--modules.orders.datasource.username=orders_app",
                    "--modules.orders.datasource.password=orders_app", "--modules.orders.datasource.migration-username=orders_migrator",
                    "--modules.orders.datasource.migration-password=orders_migrator",
                    "--modules.inventory.datasource.url=" + url, "--modules.inventory.datasource.username=inventory_app",
                    "--modules.inventory.datasource.password=inventory_app", "--modules.inventory.datasource.migration-username=inventory_migrator",
                    "--modules.inventory.datasource.migration-password=inventory_migrator"));
            placeAnOrder(context);

            try (Connection orders = DriverManager.getConnection(url, "orders_app", "orders_app")) {
                assertThat(count(orders, "select count(*) from orders")).isEqualTo(1);
                assertThatThrownBy(() -> count(orders, "select count(*) from inventory.stock"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
                assertThatThrownBy(() -> orders.createStatement().execute("create table orders.extra (id int)"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
            }
            try (Connection inventory = DriverManager.getConnection(url, "inventory_app", "inventory_app")) {
                assertThat(count(inventory, "select count(*) from reservation")).isEqualTo(1);
                assertThatThrownBy(() -> count(inventory, "select count(*) from orders.orders"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
            }
        } finally {
            apps.close();
            db.stop();
        }
    }
}
