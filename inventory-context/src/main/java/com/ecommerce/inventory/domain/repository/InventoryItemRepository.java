package com.ecommerce.inventory.domain.repository;

import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.model.InventoryItemId;

import java.util.Optional;

public interface InventoryItemRepository {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findById(InventoryItemId id);

    Optional<InventoryItem> findBySkuId(String skuId);
}
