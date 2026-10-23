package com.acme.shipping.internal;

import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveLine;
import com.acme.inventory.api.StockLevel;
import com.acme.shipping.api.ShipLine;
import com.acme.shipping.api.Shipment;
import com.acme.shipping.api.ShippingApi;
import com.acme.shipping.api.ShippingQuote;
import com.acme.shipping.api.UndeliverableException;
import io.macroservice.platform.support.ModuleService;
import java.util.List;
import java.util.UUID;

/** Calls customers and inventory, so nothing here holds a database transaction open. */
@ModuleService(module = "shipping")
class ShippingService implements ShippingApi {

    private static final long BASE_COST_CENTS = 500;
    private static final long PER_ITEM_CENTS = 100;
    private static final long INTERNATIONAL_SURCHARGE_CENTS = 1000;

    private final ShipmentRepository shipments;
    private final UndeliverableSkuRepository undeliverable;
    private final CustomersApi customers;
    private final InventoryApi inventory;

    ShippingService(ShipmentRepository shipments, UndeliverableSkuRepository undeliverable, CustomersApi customers, InventoryApi inventory) {
        this.shipments = shipments;
        this.undeliverable = undeliverable;
        this.customers = customers;
        this.inventory = inventory;
    }

    @Override
    public ShippingQuote quote(String customerId, List<ShipLine> lines) {
        Customer customer = customers.getCustomer(customerId);
        String warehouse = null;
        int items = 0;
        for (ShipLine line : lines) {
            StockLevel stock = inventory.getStock(line.sku());
            warehouse = warehouse == null ? stock.warehouse() : warehouse;
            items += line.quantity();
        }
        long cost = BASE_COST_CENTS + PER_ITEM_CENTS * items + ("IE".equals(customer.address().country()) ? 0 : INTERNATIONAL_SURCHARGE_CENTS);
        return new ShippingQuote(customerId, warehouse, cost);
    }

    @Override
    public Shipment createShipment(String requestId, String orderId, String customerId, String reservationId) {
        var existing = shipments.findByRequestId(requestId);
        if (existing.isPresent()) {
            return toApi(existing.get());
        }
        Customer customer = customers.getCustomer(customerId);
        Reservation reservation = inventory.getReservation(reservationId);
        for (ReserveLine line : reservation.lines()) {
            if (undeliverable.existsById(line.sku())) {
                throw new UndeliverableException("Cannot deliver " + line.sku() + " to " + customer.address().country());
            }
        }
        String destination = customer.address().line1() + ", " + customer.address().city() + ", " + customer.address().country();
        return toApi(shipments.save(new ShipmentEntity(UUID.randomUUID().toString(), requestId, orderId, customerId, reservationId, destination)));
    }

    private static Shipment toApi(ShipmentEntity e) {
        return new Shipment(e.getShipmentId(), e.getOrderId(), e.getCustomerId(), e.getReservationId(), e.getDestination());
    }
}
