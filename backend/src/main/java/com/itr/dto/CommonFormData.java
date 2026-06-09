package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Common DTOs shared across all ITR forms
 */
public class CommonFormData {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartA {
        private String assesseeName;
        private String pan;
        private String aadhaar;
        private String dob;
        private Integer age;
        private String ageCategory;
        private String email;
        private String mobile;
        private String address;
        private String city;
        private String state;
        private String pinCode;
        private String assessmentYear;
        private String filingType;
        private String residentialStatus;
        private String regime;
        private String employerName;
        private String employerTAN;
        private String bankName;
        private String bankAccountNo;
        private String bankIFSC;
        @Builder.Default
        private boolean panAadhaarLinked = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSalary {
        @Builder.Default
        private double salary17_1 = 0;
        @Builder.Default
        private double perquisites17_2 = 0;
        @Builder.Default
        private double profitsInLieu17_3 = 0;
        @Builder.Default
        private double grossSalary = 0;
        @Builder.Default
        private double allowancesExempt = 0;
        @Builder.Default
        private double netSalary = 0;
        @Builder.Default
        private double standardDeduction = 0;
        @Builder.Default
        private double professionalTax = 0;
        @Builder.Default
        private double professionalTaxDeduction = 0;
        @Builder.Default
        private double entertainmentAllowance = 0;
        @Builder.Default
        private double entertainmentAllowanceDeduction = 0;
        @Builder.Default
        private double basicSalary = 0;
        @Builder.Default
        private boolean governmentEmployee = false;
        @Builder.Default
        private double incomeFromSalary = 0;
        @Builder.Default
        private List<ExemptAllowance> exemptAllowancesList = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExemptAllowance {
        private String section;
        private String description;
        @Builder.Default
        private double amount = 0;
        @Builder.Default
        private double rentPaid = 0;
        private String landlordName;
        private String landlordPAN;
        private String landlordAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleOtherSources {
        @Builder.Default
        private double savingsInterest = 0;
        @Builder.Default
        private double depositInterest = 0;
        @Builder.Default
        private double otherInterest = 0;
        @Builder.Default
        private double dividendIncome = 0;
        @Builder.Default
        private double familyPension = 0;
        @Builder.Default
        private double familyPensionDeduction = 0;
        @Builder.Default
        private double otherIncome = 0;
        @Builder.Default
        private double giftsFromNonRelatives = 0;
        @Builder.Default
        private double incomeFromOtherSources = 0;
        @Builder.Default
        private List<BankInterest> bankInterestList = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInterest {
        private String bankName;
        private String accountNo;
        @Builder.Default
        private double interestEarned = 0;
        @Builder.Default
        private double tdsDeducted = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleExemptIncome {
        @Builder.Default
        private double agricultureIncome = 0;
        @Builder.Default
        private double ppfInterest = 0;
        @Builder.Default
        private double ltcExempt = 0;
        @Builder.Default
        private double gratuityExempt = 0;
        @Builder.Default
        private double otherExemptIncome = 0;
        @Builder.Default
        private double totalExemptIncome = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleTDS {
        @Builder.Default
        private List<TDSEntry> tdsOnSalary = new ArrayList<>();
        @Builder.Default
        private List<TDSEntry> tdsOther = new ArrayList<>();
        @Builder.Default
        private List<TDSEntry> tcsEntries = new ArrayList<>();
        @Builder.Default
        private double totalTDSSalary = 0;
        @Builder.Default
        private double totalTDSOther = 0;
        @Builder.Default
        private double totalTCS = 0;
        @Builder.Default
        private double totalTDS = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSEntry {
        private String tan;
        private String deductorName;
        private String deductorAddress;
        @Builder.Default
        private double grossAmount = 0;
        @Builder.Default
        private double taxDeducted = 0;
        private String certificateNo;
        private String certificateDate;
        @Builder.Default
        private boolean verified26AS = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleIT {
        @Builder.Default
        private List<AdvanceTaxEntry> advanceTaxEntries = new ArrayList<>();
        @Builder.Default
        private List<AdvanceTaxEntry> selfAssessmentEntries = new ArrayList<>();
        @Builder.Default
        private double totalAdvanceTax = 0;
        @Builder.Default
        private double totalSelfAssessmentTax = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleTCS {
        @Builder.Default
        private List<TCSEntry> tcsEntries = new ArrayList<>();
        @Builder.Default
        private double totalTCS = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TCSEntry {
        private String tan;
        private String collectorName;
        @Builder.Default
        private double grossAmount = 0;
        @Builder.Default
        private double taxCollected = 0;
        private String certificateNo;
        private String certificateDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvanceTaxEntry {
        private String bsrCode;
        private String challanNo;
        private String dateOfDeposit;
        @Builder.Default
        private double amount = 0;
        private String cin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeductionsVIA {
        @Builder.Default
        private double deduction80C = 0;
        @Builder.Default
        private double deduction80CCC = 0;
        @Builder.Default
        private double deduction80CCD1 = 0;
        @Builder.Default
        private double deduction80CCD1B = 0;
        @Builder.Default
        private double deduction80CCD2 = 0;
        @Builder.Default
        private double deduction80D = 0;
        @Builder.Default
        private double deduction80DD = 0;
        @Builder.Default
        private double deduction80DDB = 0;
        @Builder.Default
        private double deduction80E = 0;
        @Builder.Default
        private double deduction80EE = 0;
        @Builder.Default
        private double deduction80EEA = 0;
        @Builder.Default
        private double deduction80EEB = 0;
        @Builder.Default
        private double deduction80G = 0;
        @Builder.Default
        private double deduction80GG = 0;
        @Builder.Default
        private double deduction80GGA = 0;
        @Builder.Default
        private double deduction80GGC = 0;
        @Builder.Default
        private double deduction80TTA = 0;
        @Builder.Default
        private double deduction80TTB = 0;
        @Builder.Default
        private double deduction80U = 0;
        @Builder.Default
        private double deduction80JJAA = 0;
        @Builder.Default
        private boolean parentsSeniorCitizen = false;
        @Builder.Default
        private double totalDeductions = 0;
        @Builder.Default
        private List<Deduction80CItem> deduction80CBreakdown = new ArrayList<>();
        @Builder.Default
        private List<Donation80G> deduction80GBreakdown = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deduction80CItem {
        private String itemType;
        private String description;
        @Builder.Default
        private double amount = 0;
        private String policyNo;
        private String receiptNo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Donation80G {
        private String doneeName;
        private String doneePAN;
        @Builder.Default
        private double donationAmount = 0;
        private String paymentMode;
        private String receiptNo;
        private String donationDate;
        @Builder.Default
        private double eligibleDeduction = 0;
    }
}
