package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.acme.inventory.api.InventoryApi;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import io.macroservice.demo.scenarios.OrderScenarios;
import io.macroservice.platform.rpc.RpcException;
import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.testing.SharedPostgres;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The whole domain as eight processes, with the network between them misbehaving: a callee that dies after doing its
 * work, a callee whose answer is garbage, a callee process that is killed and restarted, and an orders process that
 * is restarted with work pending. Whatever happens, repeating a request must leave exactly one of every side effect.
 */
class DomainFailureTests {

    private static final List<OrderLine> LINES = List.of(new OrderLine("sku-widget", 2));

    private final TestApps apps = new TestApps();
    private final Map<String, Integer> ports = new LinkedHashMap<>();
    private final Map<String, FaultProxy> proxies = new LinkedHashMap<>();
    private final Map<String, String[]> databases = new LinkedHashMap<>();
    private final Map<String, ConfigurableApplicationContext> hosts = new LinkedHashMap<>();
    private Map<String, String> urls;
    private ConfigurableApplicationContext caller;

    @BeforeEach
    void startTheDomain() throws IOException {
        for (String module : TestApps.MODULES) {
            ports.put(module, freePort());
        }
        urls = new LinkedHashMap<>();
        for (String module : TestApps.MODULES) {
            boolean damaged = module.equals("payments") || module.equals("shipping") || module.equals("notifications");
            if (damaged) {
                int proxyPort = freePort();
                proxies.put(module, new FaultProxy(proxyPort, ports.get(module)));
                urls.put(module, "http://127.0.0.1:" + proxyPort + "/rpc/" + module);
            } else {
                urls.put(module, "http://127.0.0.1:" + ports.get(module) + "/rpc/" + module);
            }
            databases.put(module, SharedPostgres.args(module));
        }
        for (String module : TestApps.MODULES) {
            host(module);
        }
        caller = apps.start(false, TestApps.demo(), TestApps.topology(urls));
    }

    @AfterEach
    void stopTheDomain() {
        apps.close();
        proxies.values().forEach(FaultProxy::close);
    }

    private void host(String module, String... extra) {
        hosts.put(module, apps.start(true, TestApps.demo(), TestApps.topology(urls, module), databases.get(module),
                TestApps.args("--server.port=" + ports.get(module), "--modules.orders.events.retry-interval=200ms",
                        "--modules.orders.events.backoff-base=200ms", "--modules.orders.events.backoff-max=1s", "--modules.orders.events.startup-scan-delay=1s"),
                TestApps.args(extra)));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String requestId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private OrderScenarios.Probe probe() {
        return ScenarioSupport.probe(new ArrayList<>(hosts.values()));
    }

    private String scalar(String module, String sql, String argument) {
        return probe().scalar(module, sql, argument);
    }

    private int stock() {
        return caller.getBean(InventoryApi.class).getStock("sku-widget").available();
    }

    /** Exactly one of every side effect of one order, reached through any amount of retrying. */
    private void assertExactlyOneOfEverything(String requestId) {
        assertThat(scalar("orders", "select count(*) from orders where request_id = ?", requestId)).isEqualTo("1");
        assertThat(scalar("inventory", "select count(*) from reservation where request_id = ?", requestId + ":reserve")).isEqualTo("1");
        assertThat(scalar("payments", "select count(*) from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("1");
        assertThat(scalar("shipping", "select count(*) from shipment where request_id = ?", requestId + ":ship")).isEqualTo("1");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(scalar("notifications", "select count(*) from message where request_id = ?", requestId + ":notify")).isEqualTo("1"));
        assertThat(scalar("payments", "select status from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("CAPTURED");
    }

    @Test
    void aCalleeThatDiesAfterDoingItsWorkIsNotRepeatedWhenTheRequestIsRetried() {
        OrdersApi orders = caller.getBean(OrdersApi.class);
        int before = stock();
        String requestId = requestId("killed-payments");
        proxies.get("payments").damageNext(FaultProxy.Damage.CUT_AFTER_FORWARD, 1);

        assertThatThrownBy(() -> orders.placeOrder(requestId, "cust-standard", LINES)).isInstanceOf(RpcException.class);

        // the payment was authorized although the answer never arrived; the order waits in RESERVED
        assertThat(scalar("orders", "select status from orders where request_id = ?", requestId)).isEqualTo("RESERVED");
        assertThat(scalar("payments", "select count(*) from payment_authorization where request_id = ?", requestId + ":authorize")).isEqualTo("1");

        Order order = orders.placeOrder(requestId, "cust-standard", LINES);

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertExactlyOneOfEverything(requestId);
        assertThat(stock()).isEqualTo(before - 2);
    }

    @Test
    void aMalformedAnswerIsAProtocolFailureAndTheRetryDoesNotShipTwice() {
        OrdersApi orders = caller.getBean(OrdersApi.class);
        int before = stock();
        String requestId = requestId("garbage-shipping");
        // shipping is asked twice in a row: the quote (passes) and then createShipment (the answer is garbage)
        proxies.get("shipping").damageAfter(1, FaultProxy.Damage.MALFORMED_AFTER_FORWARD, 1);

        assertThatThrownBy(() -> orders.placeOrder(requestId, "cust-standard", LINES)).isInstanceOf(RpcException.class);

        assertThat(scalar("shipping", "select count(*) from shipment where request_id = ?", requestId + ":ship")).isEqualTo("1");
        assertThat(scalar("orders", "select status from orders where request_id = ?", requestId)).isEqualTo("AUTHORIZED");

        assertThat(orders.placeOrder(requestId, "cust-standard", LINES).status()).isEqualTo(OrderStatus.CONFIRMED);
        assertExactlyOneOfEverything(requestId);
        assertThat(stock()).isEqualTo(before - 2);
    }

    @Test
    void aKilledCalleeProcessThatComesBackLetsTheOrderResume() {
        OrdersApi orders = caller.getBean(OrdersApi.class);
        int before = stock();
        String requestId = requestId("restart-payments");
        hosts.get("payments").close();

        assertThatThrownBy(() -> orders.placeOrder(requestId, "cust-standard", LINES)).isInstanceOf(RpcException.class);
        assertThat(scalar("orders", "select status from orders where request_id = ?", requestId)).isEqualTo("RESERVED");
        assertThat(scalar("inventory", "select count(*) from reservation where request_id = ?", requestId + ":reserve")).isEqualTo("1");

        hosts.remove("payments");
        host("payments");
        assertThat(orders.placeOrder(requestId, "cust-standard", LINES).status()).isEqualTo(OrderStatus.CONFIRMED);
        assertExactlyOneOfEverything(requestId);
        assertThat(stock()).isEqualTo(before - 2);
    }

    @Test
    void aNotificationLostAfterDeliveryIsNotDeliveredTwice() {
        OrdersApi orders = caller.getBean(OrdersApi.class);
        String requestId = requestId("lost-notification");
        proxies.get("notifications").damageNext(FaultProxy.Damage.CUT_AFTER_FORWARD, 1);

        Order order = orders.placeOrder(requestId, "cust-standard", LINES);

        // the order does not wait for the message; the registry delivers it, the first (cut) delivery included, only once
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertExactlyOneOfEverything(requestId);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(scalar("orders", "select status from event_publication where serialized_event like ?", notifyEvent(requestId))).isEqualTo("COMPLETED"));
    }

    @Test
    void aRelayRestartedWithWorkPendingFinishesItWithoutDuplicates() {
        OrdersApi orders = caller.getBean(OrdersApi.class);
        String requestId = requestId("restart-relay");
        hosts.values().forEach(h -> h.getBeanProvider(FaultControl.class).ifAvailable(f -> f.setFlag("notifications.down", true)));

        assertThat(orders.placeOrder(requestId, "cust-standard", LINES).status()).isEqualTo(OrderStatus.CONFIRMED);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(scalar("orders", "select count(*) from event_publication where completion_date is null and serialized_event like ?", notifyEvent(requestId))).isEqualTo("1"));
        assertThat(scalar("notifications", "select count(*) from message where request_id = ?", requestId + ":notify")).isEqualTo("0");

        // the orders process dies with the message undelivered and comes back with the same database
        hosts.get("orders").close();
        hosts.remove("orders");
        hosts.values().forEach(h -> h.getBeanProvider(FaultControl.class).ifAvailable(f -> f.setFlag("notifications.down", false)));
        host("orders");

        assertExactlyOneOfEverything(requestId);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(scalar("orders", "select status from event_publication where serialized_event like ?", notifyEvent(requestId))).isEqualTo("COMPLETED"));
    }

    /** LIKE pattern for the stored notification event of an order (Spring Modulith's event_publication table). */
    private static String notifyEvent(String requestId) {
        return "%\"requestId\":\"" + requestId + ":notify\"%";
    }
}
