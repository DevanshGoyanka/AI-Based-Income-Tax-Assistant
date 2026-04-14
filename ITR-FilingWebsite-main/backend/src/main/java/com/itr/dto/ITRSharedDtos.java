package com.itr.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared DTOs for ITR-2/3/4 Forms
 * Phase 6 Implementation
 */
public class ITRSharedDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyDetail {
        public String address;
        public String pinCode;
        public PropertyType type;
        public LocalDate dateOfAcquisition;
        @Builder.Default
        public int ownershipSharePct = 100;
        public boolean isCoOwned;
        public String coOwnerPAN;
        @Builder.Default
        public int coOwnerSharePct = 0;
        @Builder.Default
        public int grossAnnualValue = 0;
        @Builder.Default
        public int municipalTax = 0;
        @Builder.Default
        public int netAnnualValue = 0;
        @Builder.Default
        public int standardDeduction = 0;
        @Builder.Default
        public int interestOnBorrowedCapital = 0;
        @Builder.Default
        public int preConstructionInterestInstallment = 0;
        @Builder.Default
        public int arrearUnrealisedRent = 0;
        @Builder.Default
        public int hpIncome = 0;
        public boolean hasPassThroughIncome;
        @Builder.Default
        public int passThroughIncome = 0;
        public String loanAccountNumber;
        public String lenderPAN;
        public String lenderName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleCGData {
        @Builder.Default
        public int stcg111A_preJul23 = 0;
        @Builder.Default
        public int stcg111A_postJul23 = 0;
        @Builder.Default
        public int stcgAtSlab = 0;
        @Builder.Default
        public int totalSTCG = 0;
        @Builder.Default
        public List<Schedule112ARow> schedule112ARows = new ArrayList<>();
        @Builder.Default
        public int ltcg112A_preJul23 = 0;
        @Builder.Default
        public int ltcg112A_postJul23 = 0;
        @Builder.Default
        public int ltcgPropertyWithIndexation = 0;
        @Builder.Default
        public int ltcgPropertyWithoutIndexation = 0;
        @Builder.Default
        public int totalLTCG = 0;
        @Builder.Default
        public int exemptionUs54 = 0;
        @Builder.Default
        public int exemptionUs54EC = 0;
        @Builder.Default
        public int exemptionUs54F = 0;
        public boolean cgasDepositConfirmed;
        @Builder.Default
        public int actualSalePrice = 0;
        @Builder.Default
        public int stampDutyValue = 0;
        @Builder.Default
        public int fullValueOfConsideration = 0;
        @Builder.Default
        public int totalCGIncome = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule112ARow {
        public String isinCode;
        public String nameOfScrip;
        @Builder.Default
        public int units = 0;
        @Builder.Default
        public int salePricePerUnit = 0;
        @Builder.Default
        public int totalSaleValue = 0;
        public LocalDate dateOfAcquisition;
        public LocalDate dateOfTransfer;
        @Builder.Default
        public int actualCostOfAcquisition = 0;
        @Builder.Default
        public int fmvPerShareOn31Jan2018 = 0;
        @Builder.Default
        public int totalFMV55_2_ac = 0;
        @Builder.Default
        public int fmvOn31Jan2018 = 0;
        @Builder.Default
        public int costWithoutIndexation = 0;
        @Builder.Default
        public int expenditureOnTransfer = 0;
        @Builder.Default
        public int totalDeductions = 0;
        @Builder.Default
        public int balanceLTCG = 0;
        @Builder.Default
        public int exemptionUs54EC = 0;
        @Builder.Default
        public int exemptionUs54F = 0;
        @Builder.Default
        public int netTaxableLTCG = 0;
        public boolean preJuly23Sale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleCYLAData {
        @Builder.Default
        public int hpLossAvailable = 0;
        @Builder.Default
        public int hpLossSetOffAgainstSalary = 0;
        @Builder.Default
        public int hpLossSetOffAgainstOS = 0;
        @Builder.Default
        public int hpLossSetOffAgainstCG = 0;
        @Builder.Default
        public int hpLossUnabsorbed = 0;
        @Builder.Default
        public int businessNSLossAvail = 0;
        @Builder.Default
        public int businessNSLossSetOff = 0;
        @Builder.Default
        public int businessNSLossUnabsorbed = 0;
        @Builder.Default
        public int speculativeLossAvail = 0;
        @Builder.Default
        public int speculativeLossSetOff = 0;
        @Builder.Default
        public int speculativeLossUnabsorbed = 0;
        @Builder.Default
        public int stcgLossAvail = 0;
        @Builder.Default
        public int stcgLossSetOffAgainstSTCG = 0;
        @Builder.Default
        public int stcgLossSetOffAgainstLTCG = 0;
        @Builder.Default
        public int stcgLossUnabsorbed = 0;
        @Builder.Default
        public int ltcgLossAvail = 0;
        @Builder.Default
        public int ltcgLossSetOffAgainstLTCG = 0;
        @Builder.Default
        public int ltcgLossUnabsorbed = 0;
        @Builder.Default
        public int vdaLoss = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleBFLAData {
        @Builder.Default
        public List<LossLedgerEntry> hpLossesAvailable = new ArrayList<>();
        @Builder.Default
        public List<LossLedgerEntry> businessLossesAvailable = new ArrayList<>();
        @Builder.Default
        public List<LossLedgerEntry> speculativeLossesAvailable = new ArrayList<>();
        @Builder.Default
        public List<LossLedgerEntry> stcgLossesAvailable = new ArrayList<>();
        @Builder.Default
        public List<LossLedgerEntry> ltcgLossesAvailable = new ArrayList<>();
        @Builder.Default
        public List<LossLedgerEntry> unabsorbedDepreciationAvailable = new ArrayList<>();
        @Builder.Default
        public int totalBFLossSetOff = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleCFLData {
        @Builder.Default
        public int hpLossToCarryForward = 0;
        @Builder.Default
        public int businessLossToCarryForward = 0;
        @Builder.Default
        public int speculativeLossToCarryForward = 0;
        @Builder.Default
        public int stcgLossToCarryForward = 0;
        @Builder.Default
        public int ltcgLossToCarryForward = 0;
        @Builder.Default
        public int unabsorbedDepToCarryForward = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule80GData {
        @Builder.Default
        public List<DonationEntry> donations = new ArrayList<>();
        @Builder.Default
        public int totalEligibleDeduction = 0;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DonationEntry {
            public String doneeName;
            public String doneePAN;
            public String doneeAddress;
            @Builder.Default
            public int donationAmount = 0;
            public PaymentMode paymentMode;
            public boolean isQualified100Pct;
            @Builder.Default
            public int eligibleAmount = 0;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleALData {
        @Builder.Default
        public List<ImmovablePropertyEntry> immovableProperties = new ArrayList<>();
        @Builder.Default
        public int jewellery = 0;
        @Builder.Default
        public int artworkPaintings = 0;
        @Builder.Default
        public int vehicles = 0;
        @Builder.Default
        public int bullionGold = 0;
        @Builder.Default
        public int otherMovable = 0;
        @Builder.Default
        public int sharesDebentures = 0;
        @Builder.Default
        public int insurancePolicies = 0;
        @Builder.Default
        public int loansGiven = 0;
        @Builder.Default
        public int cashInHandAbove500K = 0;
        @Builder.Default
        public int bankDeposits = 0;
        @Builder.Default
        public int otherFinancial = 0;
        @Builder.Default
        public int totalAssets = 0;
        @Builder.Default
        public int securedLoans = 0;
        @Builder.Default
        public int unsecuredLoans = 0;
        @Builder.Default
        public int otherLiabilities = 0;
        @Builder.Default
        public int totalLiabilities = 0;
        public boolean requiresVerification;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ImmovablePropertyEntry {
            public String address;
            @Builder.Default
            public int costOfAcquisition = 0;
            public String yearOfAcquisition;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VDATransaction {
        public LocalDate dateOfAcquisition;
        public LocalDate dateOfTransfer;
        @Builder.Default
        public int costOfAcquisition = 0;
        @Builder.Default
        public int considerationReceived = 0;
        @Builder.Default
        public int tdsDeductedUnder194S = 0;
        public boolean requiresManualCost;
        public String assetName;
        public String exchangeName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LossLedgerEntry {
        public String incurredAY;
        private LossType lossType;
        @Builder.Default
        public int originalAmount = 0;
        @Builder.Default
        public int utilizedSoFar = 0;
        @Builder.Default
        public int remaining = 0;
        public String expiryAY;
    }

    public enum LossType {
        HP, BUSINESS_NON_SPECULATIVE, SPECULATIVE, STCG, LTCG, UNABSORBED_DEPRECIATION
    }

    public enum TaxRegime { OLD, NEW }
    public enum PropertyType { SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT }
    public enum PaymentMode { CASH, CHEQUE, ONLINE, DD, NEFT, RTGS }
}
