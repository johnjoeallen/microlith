package io.macroservice.demo.scenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * The scenarios of section 16.5. They talk only to the @RpcApi interfaces, so they run unchanged against a monolith,
 * the loopback bindings and a fully split deployment, and must give identical results and exception types.
 */
public final class OrderScenarios {

    /** Looks into the data a module owns (test-only; reads, never writes). */
    public interface Probe {
        /** A single value of a query against the schema of {@code module}, with one bind parameter. */
        String scalar(String module, String sql, String argument);
    }

    /** Switches the demo faults on every process of the deployment. */
    public interface Faults {
        void notificationsDown(boolean down);
    }

    private static final Duration COMPENSATION_TIMEOUT = Duration.ofSeconds(30);

    private final OrdersApi orders;
    private final InventoryApi inventory;
    private final Probe probe;
    private final Faults faults;

    public OrderScenarios(OrdersApi orders, InventoryApi inventory, Probe probe, Faults faults) {
        this.orders = orders;
        this.inventory = inventory;
        this.probe = probe;
        this.faults = faults;
    }

    public void runAll() {
        happyPath();
        tierDiscountThroughTheChain();
        outOfStock();
        paymentDeclinedWithCompensation();
        shippingFailureWithTwoCompensations();
        dependencyDown();
        idempotentRetry();
    }

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static List<OrderLine> lines(String sku, int quantity) {
        return List.of(new OrderLine(sku, quantity));
    }

    private int widgetStock() {
        return inventory.getStock("sku-widget").available();
    }

    /** Standard customer, in-stock SKU: CONFIRMED at full price (2 x 1000, shipping 500 + 2 x 100). */
    public void happyPath() {
        Order order = orders.placeOrder(id("happy"), "cust-standard", lines("sku-widget", 2));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.subtotalCents()).isEqualTo(2000);
        assertThat(order.discountCents()).isZero();
        assertThat(order.shippingCents()).isEqualTo(700);
        assertThat(order.totalCents()).isEqualTo(2700);
        assertThat(orders.getOrder(order.orderId())).isEqualTo(order);
    }

    /** Gold customer: 10% off, priced through orders -> pricing -> customers/catalog. */
    public void tierDiscountThroughTheChain() {
        Order order = orders.placeOrder(id("gold"), "cust-gold", lines("sku-widget", 2));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.subtotalCents()).isEqualTo(2000);
        assertThat(order.discountCents()).isEqualTo(200);
        assertThat(order.totalCents()).isEqualTo(2000 - 200 + 700);
    }

    /** No stock: OutOfStockException, nothing reserved, no payment authorization made. */
    public void outOfStock() {
        String requestId = id("oos");
        assertThatThrownBy(() -> orders.placeOrder(requestId, "cust-standard", lines("sku-oos", 1)))
                .isExactlyInstanceOf(OutOfStockException.class).hasMessage("Out of stock: sku-oos");
        assertThatThrownBy(() -> orders.placeOrder(requestId, "cust-standard", lines("sku-oos", 1)))
                .as("a repeated request gets the same outcome").isExactlyInstanceOf(OutOfStockException.class);
        assertThat(probe.scalar("payments", "select count(*) from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("0");
    }

    /** Declined card: REJECTED, and the reservation is released by the outbox relay. */
    public void paymentDeclinedWithCompensation() {
        int before = widgetStock();
        String requestId = id("declined");
        Order order = orders.placeOrder(requestId, "cust-declined", lines("sku-widget", 3));
        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.failureReason()).contains("declined");
        await().atMost(COMPENSATION_TIMEOUT).untilAsserted(() -> assertThat(widgetStock()).isEqualTo(before));
        assertThat(probe.scalar("inventory", "select status from reservation where request_id = ?", requestId + ":reserve")).isEqualTo("RELEASED");
        assertThat(probe.scalar("shipping", "select count(*) from shipment where request_id = ?", requestId + ":ship")).isEqualTo("0");
    }

    /** Unshippable SKU: FAILED, payment voided and stock released (two compensations). */
    public void shippingFailureWithTwoCompensations() {
        int before = inventory.getStock("sku-undeliverable").available();
        String requestId = id("undeliverable");
        Order order = orders.placeOrder(requestId, "cust-standard", lines("sku-undeliverable", 1));
        assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.failureReason()).contains("Cannot deliver");
        await().atMost(COMPENSATION_TIMEOUT).untilAsserted(() -> {
            assertThat(probe.scalar("payments", "select status from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("VOIDED");
            assertThat(inventory.getStock("sku-undeliverable").available()).isEqualTo(before);
        });
    }

    /** Notifications down: the order is still CONFIRMED; the message arrives after the outage ends. */
    public void dependencyDown() {
        String requestId = id("notify");
        faults.notificationsDown(true);
        try {
            Order order = orders.placeOrder(requestId, "cust-standard", lines("sku-widget", 1));
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(probe.scalar("notifications", "select count(*) from message where request_id = ?", requestId + ":notify")).isEqualTo("0");
        } finally {
            faults.notificationsDown(false);
        }
        await().atMost(COMPENSATION_TIMEOUT).untilAsserted(() ->
                assertThat(probe.scalar("notifications", "select count(*) from message where request_id = ?", requestId + ":notify")).isEqualTo("1"));
    }

    /** The same request twice: one order, one reservation, one authorization, one shipment. */
    public void idempotentRetry() {
        int before = widgetStock();
        String requestId = id("retry");
        Order first = orders.placeOrder(requestId, "cust-standard", lines("sku-widget", 2));
        Order second = orders.placeOrder(requestId, "cust-standard", lines("sku-widget", 2));
        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(widgetStock()).isEqualTo(before - 2);
        assertThat(probe.scalar("orders", "select count(*) from orders where request_id = ?", requestId)).isEqualTo("1");
        assertThat(probe.scalar("inventory", "select count(*) from reservation where request_id = ?", requestId + ":reserve")).isEqualTo("1");
        assertThat(probe.scalar("payments", "select count(*) from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("1");
        assertThat(probe.scalar("shipping", "select count(*) from shipment where request_id = ?", requestId + ":ship")).isEqualTo("1");
    }
}
