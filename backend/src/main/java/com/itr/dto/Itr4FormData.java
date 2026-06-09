package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * ITR-4 (Sugam) Form Data - 101% CBDT Compliant
 * For individuals/HUFs/Firms with presumptive income (44AD/44ADA/44AE)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Itr4FormData {

    private CommonFormData.PartA partA;
    private CommonFormData.ScheduleSalary scheduleSalary;
    private Itr2FormData.HouseProperty scheduleHP;
    private CommonFormData.ScheduleOtherSources scheduleOS;
    private SchedulePresumptive schedulePresumptive;
    private SimplifiedBalanceSheet balanceSheet;
    private CommonFormData.ScheduleExemptIncome scheduleEI;
    private CommonFormData.ScheduleTDS scheduleTDS;
    private CommonFormData.ScheduleIT scheduleIT;
    private CommonFormData.DeductionsVIA deductions;
    private Itr2FormData.TaxComputation computation;
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    @Builder.Default
    private List<String> validationWarnings = new ArrayList<>();
    
    // Phase 6 additions
    private String taxRegime;
    private String assessmentYear;
    private ITRBusinessDtos.SimplifiedBalanceSheet simplifiedBalanceSheet;
    @Builder.Default
    private List<ITRBusinessDtos.GSTRegistration> gstRegistrations = new ArrayList<>();
    private ITRSharedDtos.ScheduleALData scheduleAL;
    private boolean priorYear44ADClaimed;
    private boolean businessLossCarryForwardAttempted;
    private String auditStatus;
    @Builder.Default
    private int ltcg112A = 0;
    private boolean isLLP;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulePresumptive {
        private Business44AD business44AD;
        private Professional44ADA professional44ADA;
        @Builder.Default
        private List<Vehicle44AE> vehicles44AE = new ArrayList<>();
        @Builder.Default
        private double total44AEIncome = 0;
        @Builder.Default
        private double totalPresumptiveIncome = 0;
        @Builder.Default
        private boolean optOutWarningAcknowledged = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Business44AD {
        @Builder.Default
        private boolean applicable = false;
        @Builder.Default
        private double grossTurnoverDigital = 0;
        @Builder.Default
        private double grossTurnoverCash = 0;
        @Builder.Default
        private double grossTurnoverTotal = 0;
        @Builder.Default
        private double presumptiveIncomeDigital = 0;
        @Builder.Default
        private double presumptiveIncomeCash = 0;
        @Builder.Default
        private double totalPresumptiveIncome = 0;
        @Builder.Default
        private double declaredIncome = 0;
        private String natureOfBusiness;
        @Builder.Default
        private boolean cashReceiptsBelow5Pct = false;
        @Builder.Default
        private boolean cashPaymentsBelow5Pct = false;
        @Builder.Default
        private boolean eligibleFor3CroreLimit = false;
        @Builder.Default
        private boolean optedIn = false;
        private String firstOptInYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Professional44ADA {
        @Builder.Default
        private boolean applicable = false;
        @Builder.Default
        private double grossReceipts = 0;
        @Builder.Default
        private double presumptiveIncome = 0;
        @Builder.Default
        private double declaredIncome = 0;
        private String profession;
        @Builder.Default
        private boolean cashReceiptsBelow5Pct = false;
        @Builder.Default
        private boolean cashPaymentsBelow5Pct = false;
        @Builder.Default
        private boolean eligibleFor75LakhLimit = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vehicle44AE {
        private String vehicleRegNo;
        private String vehicleType;
        @Builder.Default
        private double grossVehicleWeight = 0;
        @Builder.Default
        private int monthsOwned = 12;
        @Builder.Default
        private double presumptiveIncome = 0;
        @Builder.Default
        private double declaredIncome = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimplifiedBalanceSheet {
        @Builder.Default
        private double sundryDebtors = 0;
        @Builder.Default
        private double sundryCreditors = 0;
        @Builder.Default
        private double stockInTrade = 0;
        @Builder.Default
        private double cashBalance = 0;
        @Builder.Default
        private double openingCapital = 0;
        @Builder.Default
        private double drawings = 0;
        @Builder.Default
        private double addedCapital = 0;
        @Builder.Default
        private double closingCapital = 0;
        @Builder.Default
        private double securedLoans = 0;
        @Builder.Default
        private double unsecuredLoans = 0;
        @Builder.Default
        private double fixedAssetsWDV = 0;
    }
}
