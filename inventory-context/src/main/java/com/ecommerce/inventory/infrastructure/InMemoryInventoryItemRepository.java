package com.ecommerce.inventory.infrastructure;

import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.model.InventoryItemId;
import com.ecommerce.inventory.domain.repository.InventoryItemRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInventoryItemRepository implements InventoryItemRepository {

    private final Map<InventoryItemId, InventoryItem> store = new ConcurrentHashMap<>();

    @Override
    public InventoryItem save(InventoryItem item) {
        store.put(item.getInventoryItemId(), item);
        return item;
    }

    @Override
    public Optional<InventoryItem> findById(InventoryItemId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<InventoryItem> findBySkuId(String skuId) {
        return store.values().stream()
                .filter(item -> item.getSkuId().equals(skuId))
                .findFirst();
    }
}
