package com.itr.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * ITR-2 Output DTO for ITD JSON Export
 */
@Data
public class ITR2Output {

    private CreationInfo creationInfo;
    private PersonalInfo personalInfo;
    private FilingStatus filingStatus;
    private ScheduleS scheduleS;
    private ScheduleHP scheduleHP;
    private ScheduleCG scheduleCG;
    private Schedule112A schedule112A;
    private ScheduleVDA scheduleVDA;
    private ScheduleCYLA scheduleCYLA;
    private ScheduleBFLA scheduleBFLA;
    private ScheduleVIA scheduleVIA;
    private Schedule80G schedule80G;
    private ScheduleAL scheduleAL;
    private ScheduleSI scheduleSI;
    private TaxComputation taxComputation;
    private TaxPaid taxPaid;
    private Verification verification;

    @Data
    public static class CreationInfo {
        private String sWVersionNo;
        private String sWCreatedBy;
        private String xMLCreationDate;
        private String intermediaryCity;
        private String digest;
    }

    @Data
    public static class PersonalInfo {
        private String assesseeName;
        private String pAN;
        private String dOB;
        private String aadhaarNumber;
        private String address;
        private String employerCategory;
        private String residentialStatus;
    }

    @Data
    public static class FilingStatus {
        private String returnFileSec;
        private String returnType;
        private boolean isRevised;
        private String taxRegime;
    }

    @Data
    public static class ScheduleS {
        private int salary17_1;
        private int perquisites17_2;
        private int profitsInLieu17_3;
        private int grossSalary;
        private int exemptAllowances;
        private int netSalary;
        private int standardDeduction;
        private int professionalTax;
        private int incomeFromSalary;
    }

    @Data
    public static class ScheduleHP {
        private List<PropertyDetail> properties = new ArrayList<>();
        private int totalHPIncome;
    }

    @Data
    public static class PropertyDetail {
        private String propertyAddress;
        private String propertyType;
        private int grossAnnualValue;
        private int municipalTax;
        private int netAnnualValue;
        private int standardDeduction;
        private int interestOnLoan;
        private int arrearRent;
        private int incomeFromHP;
        private boolean coOwned;
        private double assesseeSharePct;
    }

    @Data
    public static class ScheduleCG {
        private int stcg15Pct;
        private int stcg20Pct;
        private int stcgNormal;
        private int totalSTCG;
        private int ltcg112A;
        private int ltcg10Pct;
        private int ltcg20Pct;
        private int totalLTCG;
        private int incomeUnderCapitalGains;
    }

    @Data
    public static class Schedule112A {
        private List<LTCG112ARow> rows = new ArrayList<>();
        private int totalLTCG112A;
    }

    @Data
    public static class LTCG112ARow {
        private String securityName;
        private String iSIN;
        private String dateOfAcquisition;
        private String dateOfSale;
        private double units;
        private int salePricePerUnit;
        private int totalSaleValue;
        private int actualCost;
        private int fmvJan312018;
        private int fmvPerUnitJan312018;
        private int totalFMVJan312018;
        private int costWithoutIndexation;
        private int transferExpenses;
        private int totalDeductions;
        private int balance;
    }

    @Data
    public static class ScheduleVDA {
        private List<VDADetail> vdaDetails = new ArrayList<>();
        private int totalIncomeFromVDA;
        private int tdsUs194S;
    }

    @Data
    public static class VDADetail {
        private String dateOfAcquisition;
        private String dateOfTransfer;
        private String headUnderWhichIncomeTaxable;
        private int costOfAcquisition;
        private int considerationReceived;
        private int incomeFromVDA;
    }

    @Data
    public static class ScheduleCYLA {
        private int hpLossSetOff;
        private int speculativeLoss;
        private int speculativeIncome;
        private int ltcgLoss;
        private int ltcgIncome;
    }

    @Data
    public static class ScheduleBFLA {
        private List<BFLARow> rows = new ArrayList<>();
    }

    @Data
    public static class BFLARow {
        private String lossType;
        private String assessmentYear;
        private int broughtForward;
        private int setOff;
        private int remaining;
    }

    @Data
    public static class ScheduleVIA {
        private int deduction80C;
        private int deduction80CCC;
        private int deduction80CCD1;
        private int deduction80CCD1B;
        private int deduction80CCD2;
        private int deduction80D;
        private int deduction80DD;
        private int deduction80DDB;
        private int deduction80E;
        private int deduction80G;
        private int deduction80GG;
        private int deduction80GGA;
        private int deduction80U;
        private int totalDeductions;
    }

    @Data
    public static class Schedule80G {
        private List<Donation80GDetail> donations = new ArrayList<>();
        private int totalEligibleDeduction;
    }

    @Data
    public static class Donation80GDetail {
        private String doneeName;
        private String doneePAN;
        private int donationAmount;
        private String modeOfPayment;
        private int eligibleAmount;
    }

    @Data
    public static class ScheduleAL {
        private List<AssetDetail> assets = new ArrayList<>();
        private List<LiabilityDetail> liabilities = new ArrayList<>();
    }

    @Data
    public static class AssetDetail {
        private String assetType;
        private String description;
        private int value;
    }

    @Data
    public static class LiabilityDetail {
        private String liabilityType;
        private String description;
        private int amount;
    }

    @Data
    public static class ScheduleSI {
        private int stcg111A;
        private int taxOnStcg111A;
        private int ltcg112A;
        private int taxOnLtcg112A;
        private int vdaIncome;
        private int taxOnVDA;
        private int lotteryIncome;
        private int taxOnLottery;
    }

    @Data
    public static class TaxComputation {
        private int totalIncome;
        private int taxOnTotalIncome;
        private int surcharge;
        private int healthEducationCess;
        private int grossTaxLiability;
        private int relief89;
        private int netTaxPayable;
    }

    @Data
    public static class TaxPaid {
        private int totalTDS;
        private int totalTCS;
        private int totalAdvanceTax;
        private int totalSelfAssessmentTax;
    }

    @Data
    public static class Verification {
        private String declaration;
        private String place;
        private String date;
    }
}
