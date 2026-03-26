package com.ecommerce.settlement.domain.repository;

import com.ecommerce.settlement.domain.model.Settlement;
import com.ecommerce.settlement.domain.model.SettlementId;

import java.util.Optional;

public interface SettlementRepository {

    Settlement save(Settlement settlement);

    Optional<Settlement> findById(SettlementId id);
}
