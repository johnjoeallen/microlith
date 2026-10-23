package io.macroservice.demo.scenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.Product;
import com.acme.catalog.api.ProductNotFoundException;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomerNotFoundException;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.PaymentMethod;
import com.acme.customers.api.Tier;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationNotFoundException;
import com.acme.inventory.api.ReserveLine;
import com.acme.notifications.api.Notification;
import com.acme.notifications.api.NotificationsApi;
import com.acme.payments.api.Authorization;
import com.acme.payments.api.AuthorizationNotFoundException;
import com.acme.payments.api.AuthorizationStatus;
import com.acme.payments.api.InvalidPaymentStateException;
import com.acme.payments.api.PaymentDeclinedException;
import com.acme.payments.api.PaymentsApi;
import com.acme.pricing.api.PricingApi;
import com.acme.pricing.api.Quote;
import com.acme.pricing.api.QuoteLine;
import com.acme.shipping.api.ShipLine;
import com.acme.shipping.api.Shipment;
import com.acme.shipping.api.ShippingApi;
import com.acme.shipping.api.ShippingQuote;
import com.acme.shipping.api.UndeliverableException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Results and exception types of the simpler APIs; identical in every binding. */
public final class ApiContracts {

    private final CatalogApi catalog;
    private final CustomersApi customers;
    private final PricingApi pricing;
    private final InventoryApi inventory;
    private final PaymentsApi payments;
    private final ShippingApi shipping;
    private final NotificationsApi notifications;

    public ApiContracts(CatalogApi catalog, CustomersApi customers, PricingApi pricing, InventoryApi inventory,
            PaymentsApi payments, ShippingApi shipping, NotificationsApi notifications) {
        this.catalog = catalog;
        this.customers = customers;
        this.pricing = pricing;
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
        this.notifications = notifications;
    }

    public void runAll() {
        catalogContract();
        customersContract();
        pricingContract();
        paymentsContract();
        shippingContract();
        notificationsContract();
    }

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public void catalogContract() {
        assertThat(catalog.getProduct("sku-widget")).isEqualTo(new Product("sku-widget", "Widget", 1000));
        assertThat(catalog.productExists("sku-gadget")).isTrue();
        assertThat(catalog.productExists("sku-nope")).isFalse();
        assertThatThrownBy(() -> catalog.getProduct("sku-nope")).isExactlyInstanceOf(ProductNotFoundException.class).hasMessage("Unknown product: sku-nope");
    }

    public void customersContract() {
        Customer gold = customers.getCustomer("cust-gold");
        assertThat(gold.tier()).isEqualTo(Tier.GOLD);
        assertThat(gold.address().city()).isEqualTo("Cork");
        assertThat(customers.getPaymentMethod("cust-declined")).isEqualTo(new PaymentMethod("cust-declined", "tok-declined", "VISA"));
        assertThatThrownBy(() -> customers.getCustomer("nobody")).isExactlyInstanceOf(CustomerNotFoundException.class).hasMessage("Unknown customer: nobody");
    }

    public void pricingContract() {
        Quote quote = pricing.quote("cust-gold", List.of(new QuoteLine("sku-widget", 2), new QuoteLine("sku-gadget", 1)));
        assertThat(quote.subtotalCents()).isEqualTo(4500);
        assertThat(quote.discountCents()).isEqualTo(450);
        assertThat(quote.totalCents()).isEqualTo(4050);
        assertThat(quote.lines()).extracting(l -> l.unitPriceCents()).containsExactly(1000L, 2500L);
        assertThat(pricing.quote("cust-standard", List.of(new QuoteLine("sku-widget", 1))).discountCents()).isZero();
        assertThatThrownBy(() -> pricing.quote("cust-gold", List.of(new QuoteLine("sku-nope", 1)))).isExactlyInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> pricing.quote("nobody", List.of(new QuoteLine("sku-widget", 1)))).isExactlyInstanceOf(CustomerNotFoundException.class);
    }

    public void paymentsContract() {
        String request = id("pay");
        Authorization auth = payments.authorize(request, "cust-standard", 1234);
        assertThat(auth.status()).isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(auth.amountCents()).isEqualTo(1234);
        assertThat(payments.authorize(request, "cust-standard", 1234)).isEqualTo(auth);
        assertThat(payments.capture(id("cap"), auth.authId()).status()).isEqualTo(AuthorizationStatus.CAPTURED);
        assertThatThrownBy(() -> payments.voidAuthorization(id("void"), auth.authId())).isExactlyInstanceOf(InvalidPaymentStateException.class);
        Authorization second = payments.authorize(id("pay"), "cust-gold", 99);
        assertThat(payments.voidAuthorization(id("void"), second.authId()).status()).isEqualTo(AuthorizationStatus.VOIDED);
        assertThatThrownBy(() -> payments.capture(id("cap"), second.authId())).isExactlyInstanceOf(InvalidPaymentStateException.class);
        assertThatThrownBy(() -> payments.authorize(id("pay"), "cust-declined", 1)).isExactlyInstanceOf(PaymentDeclinedException.class);
        assertThatThrownBy(() -> payments.capture(id("cap"), "nope")).isExactlyInstanceOf(AuthorizationNotFoundException.class);
    }

    public void shippingContract() {
        assertThat(shipping.quote("cust-standard", List.of(new ShipLine("sku-widget", 3)))).isEqualTo(new ShippingQuote("cust-standard", "wh-1", 800));
        Reservation reservation = inventory.reserve(id("ship-res"), "order-x", List.of(new ReserveLine("sku-widget", 1)));
        String request = id("ship");
        Shipment shipment = shipping.createShipment(request, "order-x", "cust-standard", reservation.reservationId());
        assertThat(shipment.destination()).isEqualTo("1 Main Street, Dublin, IE");
        assertThat(shipping.createShipment(request, "order-x", "cust-standard", reservation.reservationId())).isEqualTo(shipment);
        Reservation bad = inventory.reserve(id("ship-res"), "order-y", List.of(new ReserveLine("sku-undeliverable", 1)));
        assertThatThrownBy(() -> shipping.createShipment(id("ship"), "order-y", "cust-standard", bad.reservationId()))
                .isExactlyInstanceOf(UndeliverableException.class);
        assertThatThrownBy(() -> shipping.createShipment(id("ship"), "order-z", "cust-standard", "nope")).isExactlyInstanceOf(ReservationNotFoundException.class);
        inventory.release(id("rel"), reservation.reservationId());
        inventory.release(id("rel"), bad.reservationId());
    }

    public void notificationsContract() {
        String request = id("note");
        Notification sent = notifications.send(request, "cust-standard", "welcome", Map.of("k", "v"));
        assertThat(sent.email()).isEqualTo("anna@example.com");
        assertThat(sent.template()).isEqualTo("welcome");
        assertThat(notifications.send(request, "cust-standard", "welcome", Map.of("k", "v"))).isEqualTo(sent);
        assertThatThrownBy(() -> notifications.send(id("note"), "nobody", "welcome", Map.of())).isExactlyInstanceOf(CustomerNotFoundException.class);
    }
}
