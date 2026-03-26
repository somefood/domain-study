package com.ecommerce.settlement.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 정산 기간 Value Object.
 *
 * 예: 2026-03-01 ~ 2026-03-07 (1주일 단위 정산)
 */
public class SettlementPeriod {

    private final LocalDate startDate;
    private final LocalDate endDate;

    public SettlementPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일이 시작일보다 앞설 수 없습니다");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettlementPeriod that = (SettlementPeriod) o;
        return startDate.equals(that.startDate) && endDate.equals(that.endDate);
    }

    @Override
    public int hashCode() { return Objects.hash(startDate, endDate); }

    @Override
    public String toString() {
        return startDate + " ~ " + endDate;
    }
}
