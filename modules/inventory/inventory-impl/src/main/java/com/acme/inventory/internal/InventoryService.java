package com.acme.inventory.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveRequest;
import com.acme.inventory.api.StockLevel;
import io.macroservice.platform.support.ModuleService;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@ModuleService(module = "inventory")
@Transactional("inventoryTm")
class InventoryService implements InventoryApi {

    private final StockRepository stock;
    private final ReservationRepository reservations;

    InventoryService(StockRepository stock, ReservationRepository reservations) {
        this.stock = stock;
        this.reservations = reservations;
    }

    @Override
    @Transactional(value = "inventoryTm", readOnly = true)
    public StockLevel getStock(String sku) {
        return new StockLevel(sku, stock.findById(sku).map(StockEntity::getAvailable).orElse(0));
    }

    @Override
    public Reservation reserve(ReserveRequest request) {
        return reservations.findByRequestId(request.requestId()).map(InventoryService::toApi).orElseGet(() -> {
            StockEntity entry = stock.findForUpdate(request.sku()).filter(s -> s.getAvailable() >= request.quantity())
                    .orElseThrow(() -> new OutOfStockException("Out of stock: " + request.sku()));
            entry.setAvailable(entry.getAvailable() - request.quantity());
            return toApi(reservations.save(new ReservationEntity(UUID.randomUUID().toString(), request.requestId(),
                    request.orderId(), request.sku(), request.quantity())));
        });
    }

    private static Reservation toApi(ReservationEntity entity) {
        return new Reservation(entity.getReservationId(), entity.getOrderId(), entity.getSku(), entity.getQuantity());
    }
}
