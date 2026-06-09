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
        // STCG - 111A (Equity shares, units) - split by date
        @Builder.Default
        public int stcg111A_preJul23 = 0;
        @Builder.Default
        public int stcg111A_postJul23 = 0;
        @Builder.Default
        public int stcgAtSlab = 0;
        @Builder.Default
        public int stcgOther = 0;  // Debentures, etc.
        @Builder.Default
        public int totalSTCG = 0;
        
        // LTCG - 112A (Equity shares, units with grandfathering) - split by date
        @Builder.Default
        public int ltcg112A_preJul23 = 0;
        @Builder.Default
        public int ltcg112A_postJul23 = 0;
        
        // LTCG - 112 (Property and other assets) - with/without indexation
        @Builder.Default
        public int ltcgPropertyWithIndexation = 0;
        @Builder.Default
        public int ltcgPropertyWithoutIndexation = 0;
        @Builder.Default
        public int ltcgOther = 0;
        @Builder.Default
        public int totalLTCG = 0;
        
        // ===== CRITICAL: Exemptions under Section 54, 54EC, 54F, 54EE =====
        // Section 54 - Sale of residential house
        @Builder.Default
        public int exemptionUs54 = 0;
        @Builder.Default
        public int investment54Made = 0;  // Actual investment made
        @Builder.Default
        public int investment54DueDate = 0;  // Investment due before filing
        
        // Section 54EC - Infrastructure bonds (max ₹50L)
        @Builder.Default
        public int exemptionUs54EC = 0;
        @Builder.Default
        public int investment54ECMade = 0;  // Capital gain invested in bonds
        @Builder.Default
        public boolean cgasDepositConfirmed = false;
        @Builder.Default
        public String bondDetails = "";  // Name of bonds, dates
        
        // Section 54F - Sale of any asset (netconsideration method)
        @Builder.Default
        public int exemptionUs54F = 0;
        @Builder.Default
        public int investment54FMade = 0;
        @Builder.Default
        public int netFullValueConsideration = 0;  // For 54F calculation
        
        // Section 54EE - Investment in startups (new in Finance Act 2023)
        @Builder.Default
        public int exemptionUs54EE = 0;
        @Builder.Default
        public int investment54EEMade = 0;
        @Builder.Default
        public String startupName = "";
        
        // Section 54EB - Specified assets (old - not applicable now)
        @Builder.Default
        public int exemptionUs54EB = 0;
        
        // ===== Sale details for CBDT Schedule =====
        @Builder.Default
        public int actualSalePrice = 0;
        @Builder.Default
        public int stampDutyValue = 0;
        @Builder.Default
        public int fullValueOfConsideration = 0;
        
        // ===== Section 45(5A) - Compulsory acquisition =====
        @Builder.Default
        public int compensationReceived45_5A = 0;
        @Builder.Default
        public int interestReceived45_5A = 0;
        
        // ===== Loss tracking for carry-forward =====
        @Builder.Default
        public int stcgLossBroughtForward = 0;
        @Builder.Default
        public int ltcgLossBroughtForward = 0;
        @Builder.Default
        public int stcgLossCarriedForward = 0;
        @Builder.Default
        public int ltcgLossCarriedForward = 0;
        
        // ===== Aggregate =====
        @Builder.Default
        public int totalCGIncome = 0;
        
        // Detail rows
        @Builder.Default
        public List<Schedule112ARow> schedule112ARows = new ArrayList<>();
        @Builder.Default
        public List<SchedulePropertyRow> schedulePropertyRows = new ArrayList<>();
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

    /**
     * Schedule CG Row for Property/Other Assets (CBDT Schedule CG Part B)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulePropertyRow {
        // Asset identification
        public String descriptionOfAsset;  // Flat, land, gold, etc.
        public String scheduleType;  // CG-A, CG-B, CG-C, CG-D
        
        // Dates
        public LocalDate dateOfAcquisition;
        public LocalDate dateOfTransfer;
        
        // Consideration
        @Builder.Default
        public int fullValueOfConsideration = 0;
        @Builder.Default
        public int StampDutyValue = 0;  // Section 50C
        
        // Cost of acquisition
        @Builder.Default
        public int costOfAcquisition = 0;
        @Builder.Default
        public int costInflationIndex = 0;  // For indexation
        
        // Improvements
        @Builder.Default
        public int costOfImprovement = 0;
        @Builder.Default
        public int improvementIndex = 0;
        
        // Deductions
        @Builder.Default
        public int expenditureOnTransfer = 0;
        @Builder.Default
        public int totalDeductions = 0;
        
        // Gains
        @Builder.Default
        public int shortTermCapitalGain = 0;
        @Builder.Default
        public int longTermCapitalGain = 0;
        
        // Exemptions claimed
        @Builder.Default
        public int exemptionUs54 = 0;
        @Builder.Default
        public int exemptionUs54F = 0;
        @Builder.Default
        public int exemptionUs54EC = 0;
        
        // Net taxable
        @Builder.Default
        public int netShortTermGain = 0;
        @Builder.Default
        public int netLongTermGain = 0;
        
        // Buyer details (CBDT mandatory)
        public String buyerName;
        public String buyerPAN;
        
        // Transfer mode
        public String modeOfTransfer;  // Gift, sale, exchange, etc.
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
