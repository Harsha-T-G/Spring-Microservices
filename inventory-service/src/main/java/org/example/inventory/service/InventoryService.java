package org.example.inventory.service;

import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.example.inventory.exception.InventoryException;
import org.example.inventory.model.ProductStock;
import org.example.inventory.model.Reservation;
import org.example.inventory.store.InventoryStore;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryStore store;

    public ProductStock stock(String sku) {
        return store.find(sku.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new InventoryException("SKU_NOT_FOUND", "The SKU does not exist"));
    }

    public Reservation reserve(String sku, UUID orderId, int quantity, String key) {
        Reservation reservation = store.reserve(sku.trim().toUpperCase(Locale.ROOT), orderId, quantity, key);
        log.info("orderId={} reservationId={} status=RESERVED", orderId, reservation.getReservationId());
        return reservation;
    }
}
