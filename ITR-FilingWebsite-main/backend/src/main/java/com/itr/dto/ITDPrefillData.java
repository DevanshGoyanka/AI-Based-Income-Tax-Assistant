package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ITD Prefill JSON Data DTO
 * 101% CBDT Compliant - Matches ITD prefill JSON schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ITDPrefillData {
    
    private PersonalInfo personalInfo;
    private FilingStatus filingStatus;
    private List<SalaryIncome> salaryIncome;
    private List<HouseProperty> houseProperty;
    private OtherSources otherSources;
    private TDSDetails tdsDetails;
    private TaxPayments taxPayments;
    private Deductions deductions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String pan;
        private String name;
        private String dateOfBirth;
        private String aadhaar;
        private String mobileNumber;
        private String emailId;
        private String residentialStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilingStatus {
        private String assessmentYear;
        private String financialYear;
        private String returnType;
        private String filingDate;
        private String acknowledgementNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryIncome {
        private String employerName;
        private String employerTAN;
        private Double grossSalary;
        private Double exemptAllowances;
        private Double standardDeduction;
        private Double professionalTax;
        private Double netSalary;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HouseProperty {
        private String address;
        private String propertyType;
        private Double annualValue;
        private Double municipalTax;
        private Double interestOnLoan;
        private String ownershipPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherSources {
        private Double interestIncome;
        private Double dividendIncome;
        private Double familyPension;
        private Double otherIncome;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSDetails {
        private Double tdsSalary;
        private Double tdsOther;
        private Double tcs;
        private Double totalTDS;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPayments {
        private Double advanceTax;
        private Double selfAssessmentTax;
        private Double totalTaxPaid;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deductions {
        private Double section80C;
        private Double section80CCD1B;
        private Double section80CCD2;
        private Double section80D;
        private Double section80E;
        private Double section80G;
        private Double section80TTA;
        private Double section80TTB;
        private Double totalDeductions;
    }
}
