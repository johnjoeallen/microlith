package com.acme.inventory.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveRequest;
import com.acme.inventory.api.StockLevel;
import io.macroservice.platform.support.ModuleService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory stand-in until stage 4 adds real persistence. */
@ModuleService(module = "inventory")
class StubInventoryService implements InventoryApi {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("sku-1", 100, "sku-2", 5));
    private final Map<String, Reservation> reservationsByRequestId = new ConcurrentHashMap<>();

    @Override
    public StockLevel getStock(String sku) {
        return new StockLevel(sku, stock.getOrDefault(sku, 0));
    }

    @Override
    public synchronized Reservation reserve(ReserveRequest request) {
        Reservation existing = reservationsByRequestId.get(request.requestId());
        if (existing != null) {
            return existing;
        }
        int available = stock.getOrDefault(request.sku(), 0);
        if (available < request.quantity()) {
            throw new OutOfStockException("Out of stock: " + request.sku());
        }
        stock.put(request.sku(), available - request.quantity());
        Reservation reservation = new Reservation(UUID.randomUUID().toString(), request.orderId(), request.sku(), request.quantity());
        reservationsByRequestId.put(request.requestId(), reservation);
        return reservation;
    }
}
