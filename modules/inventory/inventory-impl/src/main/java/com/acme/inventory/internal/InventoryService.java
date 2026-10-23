package com.acme.inventory.internal;

import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.ProductNotFoundException;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationNotFoundException;
import com.acme.inventory.api.ReservationStatus;
import com.acme.inventory.api.ReserveLine;
import com.acme.inventory.api.StockLevel;
import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.ModuleService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** Methods that call another module (getStock -> catalog) are deliberately not transactional. */
@ModuleService(module = "inventory")
class InventoryService implements InventoryApi {

    private final StockRepository stock;
    private final ReservationRepository reservations;
    private final CatalogApi catalog;
    private final ObjectProvider<FaultControl> faults;

    InventoryService(StockRepository stock, ReservationRepository reservations, CatalogApi catalog, ObjectProvider<FaultControl> faults) {
        this.stock = stock;
        this.reservations = reservations;
        this.catalog = catalog;
        this.faults = faults;
    }

    @Override
    public StockLevel getStock(String sku) {
        faults.ifAvailable(f -> f.sleep("inventory.latency"));
        if (!catalog.productExists(sku)) {
            throw new ProductNotFoundException("Unknown product: " + sku);
        }
        return stock.findById(sku).map(s -> new StockLevel(sku, s.getAvailable(), s.getWarehouse())).orElse(new StockLevel(sku, 0, "wh-1"));
    }

    @Override
    @Transactional("inventoryTm")
    public Reservation reserve(String requestId, String orderId, List<ReserveLine> lines) {
        faults.ifAvailable(f -> f.sleep("inventory.latency"));
        return reservations.findByRequestId(requestId).map(InventoryService::toApi).orElseGet(() -> {
            // lock rows in a fixed order so that concurrent reservations cannot deadlock
            for (ReserveLine line : lines.stream().sorted(Comparator.comparing(ReserveLine::sku)).toList()) {
                StockEntity entry = stock.findForUpdate(line.sku()).filter(s -> s.getAvailable() >= line.quantity())
                        .orElseThrow(() -> new OutOfStockException("Out of stock: " + line.sku()));
                entry.setAvailable(entry.getAvailable() - line.quantity());
            }
            return toApi(reservations.save(new ReservationEntity(UUID.randomUUID().toString(), requestId, orderId,
                    lines.stream().map(l -> new ReservedLineEmbeddable(l.sku(), l.quantity())).toList())));
        });
    }

    @Override
    @Transactional("inventoryTm")
    public void release(String requestId, String reservationId) {
        ReservationEntity reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Unknown reservation: " + reservationId));
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return;
        }
        for (ReservedLineEmbeddable line : reservation.getLines().stream().sorted(Comparator.comparing(ReservedLineEmbeddable::getSku)).toList()) {
            StockEntity entry = stock.findForUpdate(line.getSku()).orElseThrow();
            entry.setAvailable(entry.getAvailable() + line.getQuantity());
        }
        reservation.setStatus(ReservationStatus.RELEASED);
    }

    @Override
    @Transactional(value = "inventoryTm", readOnly = true)
    public Reservation getReservation(String reservationId) {
        return reservations.findById(reservationId).map(InventoryService::toApi)
                .orElseThrow(() -> new ReservationNotFoundException("Unknown reservation: " + reservationId));
    }

    private static Reservation toApi(ReservationEntity e) {
        return new Reservation(e.getReservationId(), e.getOrderId(), e.getStatus(),
                e.getLines().stream().map(l -> new ReserveLine(l.getSku(), l.getQuantity())).toList());
    }
}
