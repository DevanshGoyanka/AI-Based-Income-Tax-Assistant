package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * ITR-3 Form Data - 101% CBDT Compliant
 * For individuals/HUFs with business or professional income
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Itr3FormData {

    private CommonFormData.PartA partA;
    private CommonFormData.ScheduleSalary scheduleSalary;
    @Builder.Default
    private List<Itr2FormData.HouseProperty> houseProperties = new ArrayList<>();
    private CommonFormData.ScheduleOtherSources scheduleOS;
    private Itr2FormData.ScheduleCapitalGains scheduleCG;
    private ScheduleBusinessProfession scheduleBP;
    private ScheduleDepreciation scheduleDPM;
    private ScheduleGST scheduleGST;
    private ScheduleProfitLoss schedulePL;
    private ScheduleBalanceSheet scheduleBS;
    private Itr2FormData.ScheduleVDA scheduleVDA;
    private Itr2FormData.ScheduleAL scheduleAL;
    private Itr2FormData.ScheduleFA scheduleFA;
    private Itr2FormData.ScheduleFSI scheduleFSI;
    private Itr2FormData.ScheduleAMT scheduleAMT;
    private Itr2FormData.ScheduleLosses scheduleLosses;
    private com.itr.dto.ScheduleCYLA scheduleCYLA;
    private com.itr.dto.ScheduleBFLA scheduleBFLA;
    private com.itr.dto.ScheduleCFL scheduleCFL;
    private CommonFormData.ScheduleExemptIncome scheduleEI;
    private CommonFormData.ScheduleTDS scheduleTDS;
    private CommonFormData.ScheduleIT scheduleIT;
    private CommonFormData.DeductionsVIA deductions;
    private Itr2FormData.TaxComputation computation;
    private AuditDetails auditDetails;
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    @Builder.Default
    private List<String> validationWarnings = new ArrayList<>();
    
    // Phase 6 additions
    private String taxRegime;
    private String assessmentYear;
    @Builder.Default
    private List<ITRSharedDtos.LossLedgerEntry> broughtForwardLosses = new ArrayList<>();
    private ITRBusinessDtos.FandODetail fandoDetail;
    @Builder.Default
    private List<ITRBusinessDtos.MSMEPayment> msmePayments = new ArrayList<>();
    @Builder.Default
    private List<ITRBusinessDtos.Section43BItem> section43BItems = new ArrayList<>();
    @Builder.Default
    private List<ITRBusinessDtos.EPFContribution> employeePFContributions = new ArrayList<>();
    @Builder.Default
    private int bookProfitForPartnerRemuneration = 0;
    @Builder.Default
    private int partnerRemunerationClaimed = 0;
    private boolean isSubjectToTaxAudit;
    private String auditReportFormNo;
    private java.time.LocalDate auditReportDate;
    private ITRBusinessDtos.BalanceSheetData balanceSheet;
    private ITRBusinessDtos.ProfitAndLossData profitAndLoss;
    @Builder.Default
    private List<ITRBusinessDtos.GSTRegistration> gstRegistrations = new ArrayList<>();
    @Builder.Default
    private int speculativeBusinessIncome = 0;
    @Builder.Default
    private int speculativeBusinessLoss = 0;
    private boolean sec80JJAAEligible;
    @Builder.Default
    private int newEmployeesCount = 0;
    @Builder.Default
    private int sec80JJAADeduction = 0;
    private boolean amtApplicable;
    @Builder.Default
    private int adjustedTotalIncomeForAMT = 0;
    @Builder.Default
    private int amtTax = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleBusinessProfession {
        @Builder.Default
        private double netProfitAsPerBooks = 0;
        @Builder.Default
        private double addPersonalExpenses = 0;
        @Builder.Default
        private double addCapitalExpenses = 0;
        @Builder.Default
        private double addDisallowance40Aia = 0;
        @Builder.Default
        private double addDisallowance40A2 = 0;
        @Builder.Default
        private double addDisallowance40A3 = 0;
        @Builder.Default
        private double addDisallowance43B = 0;
        @Builder.Default
        private double addDisallowance43Bh = 0;
        @Builder.Default
        private double addDisallowance14A = 0;
        @Builder.Default
        private double lessITActDepreciation = 0;
        @Builder.Default
        private double lessAdditionalDepreciation = 0;
        @Builder.Default
        private double lessOtherDeductions = 0;
        @Builder.Default
        private double profitFromBusiness = 0;
        @Builder.Default
        private boolean isFandO = false;
        @Builder.Default
        private double fandOTurnover = 0;
        @Builder.Default
        private boolean isIntraday = false;
        @Builder.Default
        private double intradayTurnover = 0;
        @Builder.Default
        private boolean isSpeculative = false;
        @Builder.Default
        private double speculativeTurnover = 0;
        @Builder.Default
        private int msmeDisallowance = 0;
        @Builder.Default
        private double deemedCapitalGains = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDepreciation {
        @Builder.Default
        private List<DepreciationBlock> blocks = new ArrayList<>();
        @Builder.Default
        private double totalDepreciation = 0;
        @Builder.Default
        private double additionalDepreciation = 0;
        @Builder.Default
        private double goodwillDepreciation = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepreciationBlock {
        private String assetClass;
        private String description;
        @Builder.Default
        private double openingWDV = 0;
        @Builder.Default
        private double additionsFirstHalf = 0;
        @Builder.Default
        private double additionsSecondHalf = 0;
        @Builder.Default
        private double saleProceeds = 0;
        @Builder.Default
        private double closingWDV = 0;
        @Builder.Default
        private double depreciationRate = 0;
        @Builder.Default
        private double depreciationAmount = 0;
        @Builder.Default
        private double additionalDepreciationRate = 0;
        @Builder.Default
        private double additionalDepreciationAmount = 0;
        @Builder.Default
        private double totalDepreciation = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleGST {
        @Builder.Default
        private double turnoverAsPerBooks = 0;
        @Builder.Default
        private double turnoverAsPerGSTR1 = 0;
        @Builder.Default
        private double difference = 0;
        private String reasonForDifference;
        private String gstin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleProfitLoss {
        @Builder.Default
        private double grossReceipts = 0;
        @Builder.Default
        private double otherIncome = 0;
        @Builder.Default
        private double openingStock = 0;
        @Builder.Default
        private double purchases = 0;
        @Builder.Default
        private double closingStock = 0;
        @Builder.Default
        private double directExpenses = 0;
        @Builder.Default
        private double salariesWages = 0;
        @Builder.Default
        private double rent = 0;
        @Builder.Default
        private double repairsMaintenance = 0;
        @Builder.Default
        private double depreciation = 0;
        @Builder.Default
        private double interestOnLoan = 0;
        @Builder.Default
        private double professionalFees = 0;
        @Builder.Default
        private double officeExpenses = 0;
        @Builder.Default
        private double travelConveyance = 0;
        @Builder.Default
        private double advertisingMarketing = 0;
        @Builder.Default
        private double insurancePremium = 0;
        @Builder.Default
        private double badDebts = 0;
        @Builder.Default
        private double otherExpenses = 0;
        @Builder.Default
        private double totalExpenses = 0;
        @Builder.Default
        private double netProfitLoss = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleBalanceSheet {
        @Builder.Default
        private double fixedAssetsWDV = 0;
        @Builder.Default
        private double investments = 0;
        @Builder.Default
        private double sundryDebtors = 0;
        @Builder.Default
        private double stockInTrade = 0;
        @Builder.Default
        private double cashAndBank = 0;
        @Builder.Default
        private double loansAdvances = 0;
        @Builder.Default
        private double otherCurrentAssets = 0;
        @Builder.Default
        private double totalAssets = 0;
        @Builder.Default
        private double openingCapital = 0;
        @Builder.Default
        private double addedCapital = 0;
        @Builder.Default
        private double drawings = 0;
        @Builder.Default
        private double closingCapital = 0;
        @Builder.Default
        private double securedLoans = 0;
        @Builder.Default
        private double unsecuredLoans = 0;
        @Builder.Default
        private double sundryCreditors = 0;
        @Builder.Default
        private double otherLiabilities = 0;
        @Builder.Default
        private double totalLiabilities = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditDetails {
        @Builder.Default
        private boolean auditRequired = false;
        private String auditorName;
        private String auditorPAN;
        private String auditorMembershipNo;
        private String auditDate;
        private String formType;
        private String auditReportDate;
    }
}
