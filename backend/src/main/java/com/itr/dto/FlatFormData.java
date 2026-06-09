package com.itr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itr.service.reconciliation.EmployerReconciliationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat form data structure matching frontend computation fields
 * CBDT Compliant - Multi-entry support for TDS, Capital Gains, Bank Accounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatFormData {
    // Personal Info
    private String name;
    private String pan;
    private String email;
    private String mobile;
    private String aadhaar;
    private String dob;
    private String fatherName;
    private int age;
    
    // Address fields
    private String flatDoorNo;
    private String premisesName;
    private String roadStreet;
    private String area;
    private String townCity;
    private String state;
    private String pinCode;
    
    // Salary
    private double basic;
    private double da;
    private double hra;
    private double bonus;
    private double allowances;
    private double perquisites;
    private double hraRent;
    private boolean hraMetro;
    private double profTax;
    
    // Employer details (for prefill extraction - legacy single employer)
    private String employerName;
    private String employerTAN;
    
    // Multi-Employer Support - CBDT Compliant
    @Builder.Default
    private List<EmployerEntry> employerEntries = new ArrayList<>();
    
    // Reconciliation result (transient - not persisted, only for API response)
    @JsonIgnoreProperties
    private EmployerReconciliationService.EmployerReconciliationResult reconciliationResult;
    
    // House Property - CBDT Compliant Multi-Property Support (AY 2026-27 allows up to 2 properties in ITR-1)
    // Uses the comprehensive HouseProperty DTO for consistency across all ITR forms
    @Builder.Default
    private List<HouseProperty> houseProperties = new ArrayList<>();
    
    // Legacy single-property fields (backward compatibility)
    private String hpType;
    private double grossRent;
    private double munTax;
    private double homeLoanInt;
    private double sopLoanInt;
    private String hpAddress;
    private String hpCity;
    private String hpState;
    private String hpPinCode;
    
    // Capital Gains - CBDT Compliant Structure (AY 2025-26)
    // STCG on Listed Equity/MF (Section 111A)
    private double stcgEquityPre;   // Transferred before 23-Jul-2024 → 15%
    private double stcgEquityPost;  // Transferred on/after 23-Jul-2024 → 20%
    private double stcgOtherSlab;   // STCG on debt/gold/unlisted → Added to GTI (slab rate)
    
    // LTCG on Listed Equity/MF (Section 112A)
    private double ltcg112APre;     // Transferred before 23-Jul-2024 → 10% (₹1L exempt)
    private double ltcg112APost;    // Transferred on/after 23-Jul-2024 → 12.5% (₹1.25L exempt)
    
    // LTCG on Other Assets (Section 112)
    private double ltcgOtherPre;    // Purchased before 23-Jul-2024 → 20% with indexation
    private double ltcgOtherPost;   // Purchased on/after 23-Jul-2024 → 12.5% without indexation
    
    // Business
    private String bizPresumptive;
    private double bizTurnover;
    private double bizDeclared;
    private double bpNetProfit;
    
    // Other Sources
    private double interestSB;
    private double interestFD;
    private double dividends;
    private double familyPension;
    private double otherMisc;
    
    // VDA
    private double vdaGains;
    
    // Deductions
    private double s80C_epf;
    private double s80C_ppf;
    private double s80C_elss;
    private double s80C_lic;
    private double s80C_home;
    private double s80CCD1B;
    private double s80CCD2;
    private double s80D_self;
    private double s80D_parent;
    private double s80E;
    private double s80TTA;
    private double s80G;
    
    // Losses
    private double bfLossHP;
    private double bfLossBusiness;
    private double bfLossSTCG;
    private double bfLossLTCG;
    private double bfLossSpeculation;
    
    // TDS - CBDT Compliant Multi-Entry Structure
    @Builder.Default
    private List<TDSEntry> tdsEntries = new ArrayList<>();
    
    // Legacy single-value fields (computed totals for backward compatibility)
    private double tdsS192;
    private double tds194A;
    private double tdsOther;
    
    // Tax Payments - CBDT Compliant Multi-Entry Structure
    @Builder.Default
    private List<AdvanceTaxEntry> advanceTaxEntries = new ArrayList<>();
    @Builder.Default
    private List<SelfAssessmentTaxEntry> selfAssessmentTaxEntries = new ArrayList<>();
    
    // Capital Gains - CBDT Compliant Multi-Entry Structure
    @Builder.Default
    private List<CapitalGainTransaction> capitalGainTransactions = new ArrayList<>();

    // Bank Interest Entries - CBDT Compliant Multi-Entry Structure
    @Builder.Default
    private List<BankInterestEntry> bankInterestEntries = new ArrayList<>();

    // Bank Accounts - CBDT Compliant Multi-Entry Structure
    @Builder.Default
    private List<BankAccountDetail> bankAccountDetails = new ArrayList<>();
    
    // Legacy single-value fields (computed totals for backward compatibility)
    private double adv15Jun;
    private double adv15Sep;
    private double adv15Dec;
    private double adv15Mar;
    private double selfTax;
    
    // CBDT Mandatory Fields
    private String gender; // MANDATORY - MALE/FEMALE/TRANSGENDER
    private String maritalStatus; // MANDATORY - SINGLE/MARRIED/DIVORCED/WIDOWED
    private String nationality; // MANDATORY - default "INDIA"
    private String residentialStatus; // MANDATORY - ROR/RNOR/NR
    
    // CBDT Nested Classes for Multi-Entry Support
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSEntry {
        private String section; // 192, 194A, 194C, 194J, etc. - MANDATORY
        private String deductorName; // MANDATORY
        private String deductorTAN; // MANDATORY - 10 chars
        private String deductorPAN; // MANDATORY for certain deductors
        @Builder.Default
        private double incomeAmount = 0; // MANDATORY
        @Builder.Default
        private double tdsDeducted = 0; // MANDATORY
        private String certificateNo; // MANDATORY
        private LocalDate deductionDate; // MANDATORY
        private String uniqueTransactionNo; // MANDATORY from AY 2024-25
        private String financialYear; // MANDATORY
        @Builder.Default
        private boolean verified26AS = false;
        @Builder.Default
        private boolean claimedInReturn = true;
    }

    /**
     * Bank Interest Entry — maps to AIS Part B2 SFT interest data.
     * Used for CBDT-compliant Other Sources multi-entry bank accounts.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInterestEntry {
        private String bankName; // MANDATORY
        private String accountType; // SAVINGS | FD | RD | CURRENT
        private String accountNumber;
        private String ifscCode;
        @Builder.Default
        private double interestEarned = 0; // MANDATORY
        @Builder.Default
        private double tdsDeducted = 0;
        private String section; // 194A or 194I
        private String sourceId; // TAN or source entity ID from AIS
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvanceTaxEntry {
        private String bsrCode; // MANDATORY - 7 digits
        private String challanNo; // MANDATORY
        private LocalDate depositDate; // MANDATORY
        @Builder.Default
        private double amount = 0; // MANDATORY
        private String cin; // Challan Identification Number - MANDATORY
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelfAssessmentTaxEntry {
        private String bsrCode; // MANDATORY - 7 digits
        private String challanNo; // MANDATORY
        private LocalDate depositDate; // MANDATORY
        @Builder.Default
        private double amount = 0; // MANDATORY
        private String cin; // Challan Identification Number - MANDATORY
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankAccountDetail {
        private String bankName; // MANDATORY
        private String ifscCode; // MANDATORY
        private String accountNo; // MANDATORY (masked)
        private String accountType; // SAVINGS/FD/RD - MANDATORY
        @Builder.Default
        private double interestEarned = 0; // MANDATORY
        @Builder.Default
        private double tdsDeducted = 0;
        private String deductorTAN;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapitalGainTransaction {
        private String securityName; // MANDATORY
        private String isin; // MANDATORY
        private LocalDate purchaseDate; // MANDATORY
        private LocalDate saleDate; // MANDATORY
        @Builder.Default
        private double quantity = 0; // MANDATORY
        @Builder.Default
        private double purchasePrice = 0; // MANDATORY
        @Builder.Default
        private double salePrice = 0; // MANDATORY
        @Builder.Default
        private double brokerageExpenses = 0;
        @Builder.Default
        private double sttPaid = 0;
        private String brokerName;
        private String brokerPAN;
        @Builder.Default
        private double fmvAsOn31Jan2018 = 0; // For grandfathering
        private String assetType; // STCG/LTCG
        private String gainType; // EQUITY_PRE/EQUITY_POST/OTHER
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployerEntry {
        private String employerName; // MANDATORY
        private String employerTAN; // MANDATORY - 10 chars
        private String employerPAN;
        @Builder.Default
        private double basic = 0;
        @Builder.Default
        private double da = 0;
        @Builder.Default
        private double hra = 0;
        @Builder.Default
        private double bonus = 0;
        @Builder.Default
        private double allowances = 0;
        @Builder.Default
        private double perquisites = 0;
        @Builder.Default
        private double hraExempt = 0;
        @Builder.Default
        private double professionalTax = 0;
        @Builder.Default
        private double tdsDeducted = 0;
        @Builder.Default
        private double grossSalary = 0;
        @Builder.Default
        private double netSalary = 0;
    }
    
    // ============================================================
    // HOUSE PROPERTY ENTRY - AY 2026-27 CBDT Compliant
    // Supports up to 2 properties in ITR-1
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HousePropertyEntry {
        
        // ===== Property Identification =====
        @Builder.Default
        private int propertySequenceNo = 1; // HPSNo (1 or 2)
        
        // Property Type - SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT
        private String propertyType;
        
        // Address Details
        private String address;           // AddrDetail
        private String city;              // CityOrTownOrDistrict
        private String state;             // StateCode
        private String pinCode;           // PinCode
        @Builder.Default
        private String countryCode = "91"; // India - MANDATORY
        
        // Property Identification (Survey/Plot/Khasra No)
        private String propertyIdentificationNo;
        
        // ===== Ownership Details =====
        // PropertyOwner: SE (Sole Owner), MI (Minor), SP (Self + Spouse), OT (Others)
        @Builder.Default
        private String propertyOwner = "SE";
        
        @Builder.Default
        private boolean isCoOwned = false; // PropCoOwnedFlg (YES/NO)
        
        // Co-owner details - MANDATORY if isCoOwned = true
        @Builder.Default
        private List<CoOwnerDetail> coOwners = new ArrayList<>();
        
        // Ownership share percentage (0-100)
        @Builder.Default
        private double ownershipShare = 100.0;
        
        // ===== Let-Out Property Income =====
        @Builder.Default
        private double annualRent = 0;              // Gross Rent Received
        @Builder.Default
        private double municipalRateableValue = 0;   // Municipal/Corporation Rateable Value
        @Builder.Default
        private double fairRentValue = 0;            // Fair Rent (as per Rent Control Act)
        @Builder.Default
        private double standardRent = 0;             // Standard Rent (if applicable)
        
        // ===== Vacancy & Unrealized Rent =====
        @Builder.Default
        private double vacancyPeriodMonths = 0;       // Months property was vacant
        @Builder.Default
        private double unrealizedRent = 0;           // Rent not realized due to vacancy
        @Builder.Default
        private double arrearsOfRent = 0;             // Arrears of rent received
        
        // ===== Deductions =====
        @Builder.Default
        private double municipalTaxesPaid = 0;       // Local Taxes paid
        
        // ===== Computed Values (Set by Backend) =====
        @Builder.Default
        private double grossAnnualValue = 0;         // GAV = Annual Rent - Unrealized Rent - Municipal Taxes
        @Builder.Default
        private double netAnnualValue = 0;            // NAV = GAV - 30% Standard Deduction
        @Builder.Default
        private double standardDeduction30Pct = 0;    // 30% of NAV
        @Builder.Default
        private double interestOnLoan = 0;            // Interest on housing loan
        @Builder.Default
        private double incomeFromHP = 0;              // Final Income from House Property
        
        // ===== Section 24B Loan Details (NEW for AY 2026-27) =====
        // Only if interestOnLoan > 0
        @Builder.Default
        private List<HomeLoanDetail> homeLoanDetails = new ArrayList<>();
        
        // ===== Tenant Details (AY 2026-27 EXPANSION) =====
        @Builder.Default
        private List<TenantDetail> tenantDetails = new ArrayList<>();
    }
    
    // Co-Owner Detail - NEW AY 2026-27
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoOwnerDetail {
        @Builder.Default
        private int coOwnerSNo = 1;           // CoOwnersSNo
        private String name;                  // NameCoOwner - MANDATORY
        private String pan;                   // PANCoOwner - MANDATORY
        private String aadhaar;              // AadhaarCoOwner - NEW
        @Builder.Default
        private double sharePercentage = 0;   // PercentShareProperty - MANDATORY
    }
    
    // Home Loan Detail - NEW AY 2026-27 Section 24B
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeLoanDetail {
        private String lenderName;                     // BankOrInstnName
        private String lenderPAN;                      // Lender PAN (if institutional)
        private String lenderType;                     // LoanTknFrom: B (Bank), I (Institution), L (Lenders)
        private String loanAccountNo;                  // LoanAccNoOfBankOrInstnRefNo
        private LocalDate dateOfLoan;                  // DateofLoan
        @Builder.Default
        private double totalLoanAmount = 0;           // TotalLoanAmt
        @Builder.Default
        private double loanOutstandingAmount = 0;      // LoanOutstndngAmt (at year end)
        @Builder.Default
        private double interestUs24B = 0;             // InterestUs24B - MANDATORY
        private LocalDate constructionCompletionDate;    // Property completion date
        @Builder.Default
        private boolean completedWithin5Years = true;   // Pre-construction interest eligibility
    }
    
    // Tenant Detail - NEW AY 2026-27
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantDetail {
        @Builder.Default
        private int tenantSNo = 1;           // TenantSNo
        private String tenantName;             // NameOfTenant - MANDATORY if rent >= ₹50,000
        private String tenantPAN;              // PANOfTenant - MANDATORY if rent >= ₹50L
        private String tenantAadhaar;         // AadhaarOfTenant - NEW
    }
}
