package com.itr.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Business-specific DTOs for ITR-3/4
 * Phase 6 Implementation
 */
public class ITRBusinessDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MSMEPayment {
        public String supplierName;
        public String supplierGSTIN;
        public boolean isMSMERegistered;
        public LocalDate supplyDate;
        public LocalDate paymentDate;
        @Builder.Default
        public int amount = 0;
        public boolean disallowed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section43BItem {
        public String description;
        public String category;
        @Builder.Default
        public int amount = 0;
        public LocalDate paymentDate;
        public boolean paidBeforeFilingDate;
        public boolean disallowed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EPFContribution {
        public String employeeUAN;
        @Builder.Default
        public int amount = 0;
        public LocalDate dueDate;
        public LocalDate depositDate;
        public boolean depositedOnTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FandODetail {
        @Builder.Default
        public int grossContractValue = 0;
        @Builder.Default
        public int absoluteProfitSum = 0;
        @Builder.Default
        public int absoluteLossSum = 0;
        @Builder.Default
        public int fandoTurnover = 0;
        @Builder.Default
        public int netProfitOrLoss = 0;
        public boolean isSpeculative;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GSTRegistration {
        public String gstin;
        public String businessName;
        @Builder.Default
        public int turnoverGSTR1 = 0;
        @Builder.Default
        public int turnoverGSTR3B = 0;
        @Builder.Default
        public int turnoverBooks = 0;
        @Builder.Default
        public int differenceGSTvsBooks = 0;
        public String reasonForDifference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSheetData {
        @Builder.Default
        public int fixedAssetsGross = 0;
        @Builder.Default
        public int accumulatedDepreciation = 0;
        @Builder.Default
        public int fixedAssetsNet = 0;
        @Builder.Default
        public int sundryDebtors = 0;
        @Builder.Default
        public int cashAndBankBalance = 0;
        @Builder.Default
        public int stockInTrade = 0;
        @Builder.Default
        public int loansAndAdvances = 0;
        @Builder.Default
        public int otherCurrentAssets = 0;
        @Builder.Default
        public int totalAssets = 0;
        @Builder.Default
        public int capitalAccount = 0;
        @Builder.Default
        public int reservesAndSurplus = 0;
        @Builder.Default
        public int securedLoans = 0;
        @Builder.Default
        public int unsecuredLoans = 0;
        @Builder.Default
        public int sundryCreditors = 0;
        @Builder.Default
        public int otherLiabilities = 0;
        @Builder.Default
        public int totalLiabilities = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitAndLossData {
        @Builder.Default
        public int grossReceipts = 0;
        @Builder.Default
        public int openingStock = 0;
        @Builder.Default
        public int purchases = 0;
        @Builder.Default
        public int directExpenses = 0;
        @Builder.Default
        public int closingStock = 0;
        @Builder.Default
        public int grossProfit = 0;
        @Builder.Default
        public int otherIncome = 0;
        @Builder.Default
        public int salariesAndWages = 0;
        @Builder.Default
        public int rentRatesAndTaxes = 0;
        @Builder.Default
        public int repairsAndMaintenance = 0;
        @Builder.Default
        public int depreciation = 0;
        @Builder.Default
        public int otherExpenses = 0;
        @Builder.Default
        public int netProfitAsPerPL = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimplifiedBalanceSheet {
        @Builder.Default
        public int grossReceipts = 0;
        @Builder.Default
        public int netProfit = 0;
        @Builder.Default
        public int sundryDebtors = 0;
        @Builder.Default
        public int sundryCreditors = 0;
        @Builder.Default
        public int stockInTrade = 0;
        @Builder.Default
        public int cashBalance = 0;
        @Builder.Default
        public int openingCapital = 0;
        @Builder.Default
        public int drawings = 0;
        @Builder.Default
        public int additionsToCapital = 0;
        @Builder.Default
        public int closingCapital = 0;
        @Builder.Default
        public int securedLoans = 0;
        @Builder.Default
        public int unsecuredLoans = 0;
        @Builder.Default
        public int totalFixedAssetsWDV = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleBPData {
        @Builder.Default
        public int netProfitFromPL = 0;
        @Builder.Default
        public int debitPersonalExpenses = 0;
        @Builder.Default
        public int debitCapitalExpenditure = 0;
        @Builder.Default
        public int disallowance40A3_cash = 0;
        @Builder.Default
        public int disallowance36_1_va_epf = 0;
        @Builder.Default
        public int disallowance43B = 0;
        @Builder.Default
        public int disallowance43Bh_msme = 0;
        @Builder.Default
        public int disallowance14A = 0;
        @Builder.Default
        public int excessBookDepreciation = 0;
        @Builder.Default
        public int totalAddBack = 0;
        @Builder.Default
        public int dividendIncome = 0;
        @Builder.Default
        public int rentalIncome = 0;
        @Builder.Default
        public int itActDepreciation = 0;
        @Builder.Default
        public int additionalDepreciation = 0;
        @Builder.Default
        public int deduction35AD = 0;
        @Builder.Default
        public int otherPermissibleDeductions = 0;
        @Builder.Default
        public int totalDeductions = 0;
        @Builder.Default
        public int profitFromBusiness = 0;
        @Builder.Default
        public int speculativeBusinessIncome = 0;
        @Builder.Default
        public int totalBusinessIncome = 0;
        @Builder.Default
        public int bookProfit = 0;
        @Builder.Default
        public int partnerRemunerationClaimed = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDPMData {
        private DepreciationBlock buildings_5pct;
        private DepreciationBlock buildings_10pct;
        private DepreciationBlock buildings_40pct;
        private DepreciationBlock furniture_10pct;
        private DepreciationBlock pm_15pct;
        private DepreciationBlock computers_40pct;
        private DepreciationBlock vehicles_15pct;
        private DepreciationBlock intangibles_25pct;
        @Builder.Default
        public int totalDepreciation = 0;
        @Builder.Default
        public int stcgOnBlocks = 0;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DepreciationBlock {
            public String assetType;
            @Builder.Default
            public int rate = 0;
            @Builder.Default
            public int openingWDV = 0;
            @Builder.Default
            public int additions = 0;
            @Builder.Default
            public int additionsAfterOct1 = 0;
            @Builder.Default
            public int disposals = 0;
            @Builder.Default
            public int closingWDV = 0;
            @Builder.Default
            public int depreciation = 0;
            @Builder.Default
            public int additionalDepreciation = 0;
            @Builder.Default
            public int stcgOnBlock = 0;
            public boolean hasGoodwill;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDCGData {
        @Builder.Default
        public int row1a = 0;
        @Builder.Default
        public int row1b = 0;
        @Builder.Default
        public int row1c = 0;
        @Builder.Default
        public int row1d = 0;
        @Builder.Default
        public int row1_total = 0;
        @Builder.Default
        public int row2a = 0;
        @Builder.Default
        public int row2b = 0;
        @Builder.Default
        public int row2c = 0;
        @Builder.Default
        public int row2d = 0;
        @Builder.Default
        public int totalDeemedCapitalGains = 0;
    }
}
