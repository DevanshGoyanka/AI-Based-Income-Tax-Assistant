package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TIS (Taxpayer Information Summary) data structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TISData {
    private long dividendIncome;
    private long interestFromDeposit;
    private long securitiesSaleConsideration;
    private long securitiesPurchaseAmount;
    private long interestOnSecurities;
    private long salaryAmount;
    private long rentIncome;
}
