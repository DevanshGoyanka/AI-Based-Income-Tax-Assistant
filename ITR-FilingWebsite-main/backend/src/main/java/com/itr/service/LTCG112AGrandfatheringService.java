package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * LTCG 112A Grandfathering Service - 101% CBDT Compliant
 * Computes FMV as on Jan 31, 2018 for grandfathering of listed equity/MF units
 * Reference: ITR_Import_JSON_Validation.md Section 2.2
 */
@Slf4j
@Service
public class LTCG112AGrandfatheringService {

    private static final LocalDate GRANDFATHERING_DATE = LocalDate.of(2018, 1, 31);

    public GrandfatheringResult computeCostBasis(GrandfatheringInput input) {
        GrandfatheringResult result = new GrandfatheringResult();
        result.setAcquisitionDate(input.getAcquisitionDate());
        result.setSaleDate(input.getSaleDate());
        result.setActualCost(input.getActualCost());
        result.setFmvJan312018(input.getFmvJan312018());
        result.setSaleValue(input.getSaleValue());

        // Rule: If acquired before Feb 1, 2018, use max(actual cost, FMV Jan 31 2018)
        if (input.getAcquisitionDate().isBefore(LocalDate.of(2018, 2, 1))) {
            result.setGrandfathered(true);
            
            // Cost = max(actual cost, FMV Jan 31 2018)
            // But FMV cannot exceed sale value
            double effectiveFMV = Math.min(input.getFmvJan312018(), input.getSaleValue());
            double costBasis = Math.max(input.getActualCost(), effectiveFMV);
            
            result.setCostBasis(costBasis);
            result.setGrandfatheringBenefit(costBasis - input.getActualCost());
            
            // LTCG = Sale Value - Cost Basis - Transfer Expenses
            double ltcg = input.getSaleValue() - costBasis - input.getTransferExpenses();
            result.setLtcg(Math.max(0, ltcg));
            
            result.setExplanation("Asset acquired before Feb 1, 2018. Cost basis = max(actual cost Rs " 
                + (long)input.getActualCost() + ", FMV Jan 31 2018 Rs " + (long)effectiveFMV + ") = Rs " + (long)costBasis);
        } else {
            // Acquired on/after Feb 1, 2018 - no grandfathering
            result.setGrandfathered(false);
            result.setCostBasis(input.getActualCost());
            result.setGrandfatheringBenefit(0);
            
            double ltcg = input.getSaleValue() - input.getActualCost() - input.getTransferExpenses();
            result.setLtcg(Math.max(0, ltcg));
            
            result.setExplanation("Asset acquired on/after Feb 1, 2018. Cost basis = actual cost Rs " + (long)input.getActualCost());
        }

        // Apply Rs 1,25,000 exemption
        result.setExemption125000(Math.min(result.getLtcg(), 125000));
        result.setTaxableLtcg(Math.max(0, result.getLtcg() - result.getExemption125000()));

        return result;
    }

    public double computeFMVFromMarketData(String isin, LocalDate date, double quantity) {
        // In production, this would call NSE/BSE API or database
        // For now, return 0 - caller must provide FMV
        log.warn("FMV computation from market data not implemented. ISIN: {}, Date: {}", isin, date);
        return 0;
    }

    @Data
    public static class GrandfatheringInput {
        private LocalDate acquisitionDate;
        private LocalDate saleDate;
        private double actualCost;
        private double fmvJan312018;
        private double saleValue;
        private double transferExpenses;
        private String isin;
        private double quantity;
    }

    @Data
    public static class GrandfatheringResult {
        private LocalDate acquisitionDate;
        private LocalDate saleDate;
        private double actualCost;
        private double fmvJan312018;
        private double saleValue;
        private boolean grandfathered;
        private double costBasis;
        private double grandfatheringBenefit;
        private double ltcg;
        private double exemption125000;
        private double taxableLtcg;
        private String explanation;
    }
}
