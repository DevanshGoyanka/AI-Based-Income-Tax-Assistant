package com.itr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ITD Prefill JSON Data DTO
 * 101% CBDT Compliant - Matches actual ITD prefill JSON schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ITDPrefillData {
    
    private PersonalInfo personalInfo;
    private FilingStatus filingStatus;
    private Form26AS form26as;
    private Form24Q form24q;
    private Insights insights;
    private List<BankAccountDetail> bankAccountDtls;
    private Verification verification;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonalInfo {
        private String pan;
        private String assesseVerPan;
        private String assesseeVerName;
        private String dob;
        private String aadhaarCardNo;
        private String fatherName;
        private Address address;
        private AssesseeName assesseeName;
        private FilingStatusInfo filingStatus;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Address {
            private String emailAddress;
            private Long mobileNo;
            private String pinCode;
            private String residenceName;
            private String residenceNo;
            private String roadOrStreet;
            private String localityOrArea;
            private String cityOrTownOrDistrict;
            private String stateCode;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AssesseeName {
            private String firstName;
            private String middleName;
            private String surNameOrOrgName;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FilingStatusInfo {
            private String residentialStatus;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilingStatus {
        private String returnFileSec;
        private String origRetFiledDate;
        private String receiptNo;
        private String OptingNewTaxRegimeForm10IF;
        private String SeventhProvisio139;
        private List<ClauseDetail> clauseiv7provisio139iDtls;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ClauseDetail {
            private String clauseiv7provisio139iNature;
            private Double clauseiv7provisio139iAmount;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Form26AS {
        private TdsOnSalaries tdsOnSalaries;
        private ScheduleOS scheduleOS;
        private TaxPayments taxPayments;
        private List<IncomeDeductionsOthersInc> incomeDeductionsOthersInc;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TdsOnSalaries {
            private List<TdsOnSalary> tdsOnSalary;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class TdsOnSalary {
                private Double totalTDSSal;
                private Double incChrgSal;
                private EmployerDetail employerOrDeductorOrCollectDetl;
                
                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class EmployerDetail {
                    private String tan;
                    private String employerOrDeductorOrCollecterName;
                }
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ScheduleOS {
            private IncOthThanOwnRaceHorse incOthThanOwnRaceHorse;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class IncOthThanOwnRaceHorse {
                private Double dividendGross;
                private Double DividendOthThan22e;
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TaxPayments {
            private List<TaxPayment> taxPayment;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class TaxPayment {
                private String dateDep;
                private String bsrCode;
                private Double amt;
                private String receiptNumber;
                private Long srlNoOfChaln;
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IncomeDeductionsOthersInc {
            private Double othSrcOthAmount;
            private String othSrcNatureDesc;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Form24Q {
        private Salaries salaries;
        private IncomeDeductions incomeDeductions;
        private Double intrstFrmSavingBank;
        private UsrDeductUndChapVIAType usrDeductUndChapVIAType;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Salaries {
            private List<Salary> salary;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Salary {
                private String nameOfEmployer;
                private String tanOfEmployer;
                private SalaryDetails salarys;
                
                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class SalaryDetails {
                    private Double salary;
                    private Double valueOfPerquisites;
                    private Double profitsinLieuOfSalary;
                }
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IncomeDeductions {
            private Double salary;
            private Double perquisitesValue;
            private Double profitsInSalary;
            private Double deductionUs16Ia;
            private Double professionalTaxUs16Iii;
            private Double entertainmentAlw16Ii;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UsrDeductUndChapVIAType {
            private Double section80C;
            private Double section80CCC;
            private Double section80CCDEmployeeOrSE;
            private Double section80CCD1B;
            private Double section80CCDEmployer;
            private Double section80D;
            private Double section80E;
            private Double section80TTA;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Insights {
        private CumulativeSalary cumulativeSalary;
        private Salaries salaries;
        private Double intrstFrmSavingBank;
        private Double intrstFrmTermDeposit;
        private ScheduleOS scheduleOS;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CumulativeSalary {
            private Double salary;
            private Double perquisitesValue;
            private Double profitsInSalary;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Salaries {
            private List<Salary> salary;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Salary {
                private String nameOfEmployer;
                private String tanOfEmployer;
                private SalaryDetails salarys;
                
                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class SalaryDetails {
                    private Double salary;
                    private Double valueOfPerquisites;
                    private Double profitsinLieuOfSalary;
                }
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ScheduleOS {
            private IncOthThanOwnRaceHorse incOthThanOwnRaceHorse;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class IncOthThanOwnRaceHorse {
                private Double dividendGross;
            }
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankAccountDetail {
        private List<AdditionalBankDetail> addtnlBankDetails;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AdditionalBankDetail {
            private String bankAccountNo;
            private String bankName;
            private String ifsccode;
            private String AccountType;
            private String useForRefund;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Verification {
        private Declaration declaration;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Declaration {
            private String assesseeVerName;
            private String assesseeVerPAN;
            private String fatherName;
        }
    }
}
