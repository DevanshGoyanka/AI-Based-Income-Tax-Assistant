package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Import the unified HouseProperty DTO for cross-ITR compatibility
import com.itr.dto.HouseProperty;

/**
 * ITR-1 (Sahaj) Form Data - 101% CBDT Compliant
 * For Resident Individuals with:
 * - Salary/Pension income
 * - Up to 2 house properties (AY 2026-27 update)
 * - Other sources (interest, dividend)
 * - Total income ≤ ₹50 lakh
 * - No capital gains, business income, or foreign assets
 * 
 * Uses unified HouseProperty DTO for consistency across all ITR forms
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Itr1FormData {

    // Personal Information
    private PersonalInfo personalInfo;
    
    // Income Schedules
    private SalaryIncome salaryIncome;
    private HousePropertyIncome housePropertyIncome;
    private OtherSourcesIncome otherSourcesIncome;
    
    // ===== HOUSE PROPERTY (AY 2026-27: Up to 2 properties in ITR-1) =====
    // Uses unified HouseProperty DTO for consistency across all ITR forms
    @Builder.Default
    private List<HouseProperty> houseProperties = new ArrayList<>();
    
    // Computed total income from all house properties
    @Builder.Default
    private double totalIncomeFromHouseProperty = 0;
    
    // Deductions
    private Deductions deductions;
    
    // Tax Schedules
    private TaxPayments taxPayments;
    private ExemptIncome exemptIncome;
    
    // Computation
    private TaxComputation taxComputation;
    
    // Brought Forward Losses from previous years
    private double bfLossHP;
    private double bfLossBusiness;
    private double bfLossSTCG;
    private double bfLossLTCG;
    
    // Validation
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    @Builder.Default
    private List<String> validationWarnings = new ArrayList<>();

    // ===== Filing Status - NEW for AY 2026-27 =====
    private String returnFileSec;      // 11/12/13/14/16/17/18/20
    private String optOutNewTaxRegime; // Y/N
    private String seventhProvisio139; // Y/N - NEW in AY 2026-27
    
    // Seventh Proviso fields - NEW in AY 2026-27
    private String incrExpAggAmt2LkTrvFrgnCntryFlg; // Y/N
    private Long amtSeventhProvisio139ii;
    private String incrExpAggAmt1LkElctrctyPrYrFlg; // Y/N
    private Long amtSeventhProvisio139iii;
    private String clauseiv7provisio139i; // Y/N
    
    // Assessee Representative - NEW in AY 2026-27
    private String asseseeRepFlg; // Y/N
    private String asseseeRepName;
    private String asseseeRepEmail;
    private String asseseeRepMobile;
    
    // Original Return Details (for revised)
    private String originalAckNo;
    private LocalDate originalFilingDate;
    private String noticeNo;
    private LocalDate noticeDateUnderSec;
    
    private String itrFilingDueDate; // "2026-07-31"

    /**
     * Part A - Personal Information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String assesseeName;
        private String pan;
        private String aadhaar;
        private LocalDate dateOfBirth;
        private Integer age;
        private String ageCategory; // BELOW_60, SENIOR_60_TO_80, SUPER_SENIOR_80_PLUS
        private String gender; // MALE, FEMALE, TRANSGENDER - CBDT mandatory
        private String fatherName; // CBDT MANDATORY
        private String maritalStatus; // CBDT MANDATORY - SINGLE/MARRIED/DIVORCED/WIDOWED
        private String nationality; // CBDT MANDATORY - default "INDIA"
        private String email;
        private String mobile;
        private String flatDoorNo;
        private String premisesName;
        private String roadStreet;
        private String area;
        private String townCity;
        private String state;
        private String pinCode;
        private String address; // Full address for reports
        private String assessmentYear;
        private String financialYear;
        private String filingType; // ORIGINAL, REVISED, DEFECTIVE, UPDATED
        private String originalAckNo;
        private LocalDate originalFilingDate;
        private String residentialStatus; // ROR only for ITR-1
        private String regime; // OLD, NEW
        private String employerCategory; // GOVT, PSU, PENSIONER, OTHERS
        private String natureOfEmployment;
        @Builder.Default
        private boolean panAadhaarLinked = true;
        
        // CBDT ITR-1 Eligibility Fields
        @Builder.Default
        private boolean isDirector = false; // Director in company - triggers ITR-2
        @Builder.Default
        private boolean holdsUnlistedShares = false; // Holds unlisted equity shares - triggers ITR-2
        @Builder.Default
        private double agriculturalIncome = 0; // Agricultural income (>₹5,000 triggers ITR-2)
        
        // CBDT MANDATORY Foreign Asset Questions
        @Builder.Default
        private boolean bankAccountsOutsideIndia = false; // MANDATORY question
        @Builder.Default
        private boolean signingAuthorityInForeignAccount = false; // MANDATORY question
        @Builder.Default
        private boolean foreignAssets = false; // MANDATORY question
        @Builder.Default
        private boolean beneficiaryOfForeignTrust = false; // MANDATORY question
        
        // Bank details for refund
        private String bankName;
        private String bankAccountNo;
        private String bankIFSC;
        private String bankAccountType; // SAVINGS, CURRENT
        @Builder.Default
        private boolean bankAccountPreValidated = false;
    }

    /**
     * Schedule Salary - Section 17
     * 101% CBDT Compliant - All fields as per IT Act 2025
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryIncome {
        // Gross Salary Components (CBDT Schedule S)
        @Builder.Default
        private double basicSalary = 0;
        @Builder.Default
        private double daAmount = 0; // Dearness Allowance (Section 17(1)(ii))
        @Builder.Default
        private double bonusAmount = 0; // Section 17(1)(ix)
        @Builder.Default
        private double commissionAmount = 0; // Section 17(1)(ix)
        
        // Allowance Components (Section 17(1))
        @Builder.Default
        private double hraReceived = 0; // House Rent Allowance u/s 17(1)(iii)
        @Builder.Default
        private double ltaReceived = 0; // Leave Travel Allowance u/s 17(1)(iv)
        @Builder.Default
        private double ceaReceived = 0; // Children Education Allowance u/s 17(1)(vi)
        @Builder.Default
        private double hostelAllowanceReceived = 0; // Section 17(1)(vi)
        @Builder.Default
        private double transportAllowanceReceived = 0; // Section 17(1)(v) - for disabled
        @Builder.Default
        private double medicalReimbursementReceived = 0; // Section 17(1)(vii)
        @Builder.Default
        private double conveyanceAllowanceReceived = 0; // Section 17(1)(v)
        @Builder.Default
        private double uniformAllowanceReceived = 0; // Section 17(1)(v)
        @Builder.Default
        private double otherAllowance = 0; // Any other allowance u/s 17(1)(v)
        
        // Section 17(1) total
        @Builder.Default
        private double salary17_1 = 0; // Basic + DA + Bonus + Commission
        
        // Perquisites (Section 17(2))
        @Builder.Default
        private double perquisites17_2 = 0; // Rent-free accommodation, car, etc.
        @Builder.Default
        private double rentFreeAccommodationValue = 0; // Section 17(2)(i)
        @Builder.Default
        private double carValue = 0; // Section 17(2)(ii) - valuation as per rules
        @Builder.Default
        private double gasFuelPowerValue = 0; // Section 17(2)(iii)
        @Builder.Default
        private double freeHolidayValue = 0; // Section 17(2)(iv)
        @Builder.Default
        private double freeGoodsValue = 0; // Section 17(2)(v)
        @Builder.Default
        private double freeServicesValue = 0; // Section 17(2)(vi)
        @Builder.Default
        private double stockOptionsValue = 0; // Section 17(2)(vii) - ESOP
        @Builder.Default
        private double professionalTaxValue = 0; // Section 17(2)(viii) - paid by employer
        
        // Profits in Lieu of Salary (Section 17(3))
        @Builder.Default
        private double profitsInLieu17_3 = 0;
        @Builder.Default
        private double gratuityReceived = 0; // Section 17(3)(i) - any excess
        @Builder.Default
        private double leaveEncashmentReceived = 0; // Section 17(3)(ii) - any excess
        @Builder.Default
        private double commutationOfPensionReceived = 0; // Section 17(3)(iii)
        @Builder.Default
        private double retrenchmentCompensation = 0; // Section 17(3)(iv)
        @Builder.Default
        private double vrsCompensation = 0; // Section 17(3)(iva)
        
        // Gross Salary Total
        @Builder.Default
        private double grossSalary = 0;
        
        // Exemption Calculations u/s 10
        // HRA Exemption (Section 10(13A)) - calculated
        @Builder.Default
        private double hraExempt = 0;
        @Builder.Default
        private double hraTaxable = 0;
        
        // LTA Exemption (Section 10(5)) - actual amount claimed
        @Builder.Default
        private double ltaExempt = 0;
        @Builder.Default
        private double ltaTaxable = 0;
        
        // Children Education Allowance (Section 10(14))
        @Builder.Default
        private double ceaExempt = 0; // ₹100/month per child, max 2 children
        @Builder.Default
        private double ceaTaxable = 0;
        
        // Hostel Expenditure Allowance (Section 10(14))
        @Builder.Default
        private double hostelExempt = 0; // ₹300/month per child, max 2 children
        @Builder.Default
        private double hostelTaxable = 0;
        
        // Transport Allowance (Section 10(14)) - for disabled employees
        @Builder.Default
        private double transportAllowanceExempt = 0; // ₹1,600/month
        @Builder.Default
        private double transportAllowanceTaxable = 0;
        
        // Medical Reimbursement (Section 17(1)(vii)) - exempt up to ₹15,000
        @Builder.Default
        private double medicalReimbursementExempt = 0;
        @Builder.Default
        private double medicalReimbursementTaxable = 0;
        
        // Total Exempt Allowances
        @Builder.Default
        private double totalExemptAllowances = 0;
        
        // Net Salary = Gross - Exempt Allowances
        @Builder.Default
        private double netSalary = 0;
        
        // Deductions u/s 16
        @Builder.Default
        private double standardDeduction = 0; // ₹75,000 for AY 2025-26 (both regimes)
        @Builder.Default
        private double entertainmentAllowance = 0; // u/s 16(ii) - Govt employees only
        @Builder.Default
        private double professionalTax = 0; // u/s 16(iii) - Max ₹2,500
        
        // Final Income from Salary
        @Builder.Default
        private double incomeFromSalary = 0;
        
        // Employer details for reports
        private String employerName;
        private String employerTAN;
        private String employerAddress;
        
        // Retirement benefits calculation
        @Builder.Default
        private double daForRetirement = 0; // DA considered for HRA calc on retirement
        @Builder.Default
        private boolean isGovernmentEmployee = false;
        @Builder.Default
        private boolean isPensioner = false;
        
        // Multiple employers support
        @Builder.Default
        private List<EmployerDetails> employers = new ArrayList<>();
        @Builder.Default
        private List<ExemptAllowanceDetail> exemptAllowanceDetails = new ArrayList<>();
        
        // Leave Encashment Details (Section 10(10AA))
        private LocalDate retirementDate;
        @Builder.Default
        private double leaveDaysAtRetirement = 0;
        @Builder.Default
        private double leaveEncashmentEligibility = 0; // Up to 10 months avg salary
        @Builder.Default
        private double leaveEncashmentExempt = 0; // Section 10(10AA) calculation
        @Builder.Default
        private double leaveEncashmentTaxable = 0;
        
        // Gratuity Details (Section 10(10))
        @Builder.Default
        private double gratuityEligibility = 0; // ₹20L or actual, whichever is less
        @Builder.Default
        private double gratuityExempt = 0; // Section 10(10) calculation
        @Builder.Default
        private double gratuityTaxable = 0;
        
        // Pension Details (Section 10(10A))
        @Builder.Default
        private String pensionType = null; // COMMUTED/UNCOMMUTED
        @Builder.Default
        private double pensionCommutationReceived = 0;
        @Builder.Default
        private double pensionCommutationExempt = 0; // Section 10(10A)
        @Builder.Default
        private double pensionCommutationTaxable = 0;
        
        // NPS Details (Section 80CCD)
        @Builder.Default
        private double npsEmployeeContribution = 0; // For 80C
        @Builder.Default
        private double npsEmployerContribution = 0; // 80CCD(2) - separate 10%/14%
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployerDetails {
        private String employerName; // CBDT mandatory
        private String employerTAN; // CBDT mandatory - 10-char alphanumeric
        private String employerPAN; // CBDT mandatory for govt/PSU
        private String employerAddress; // CBDT mandatory
        private String employerCity; // CBDT MANDATORY
        private String employerState; // CBDT MANDATORY
        private String employerPinCode; // CBDT MANDATORY
        private String employerCountry; // CBDT MANDATORY - default "INDIA"
        private String employerCategory; // Govt/PSU/Pensioners/Others - CBDT mandatory
        private String employmentType; // CBDT MANDATORY - REGULAR/CONTRACTUAL/CASUAL
        @Builder.Default
        private boolean pensioner = false; // CBDT MANDATORY flag
        private String pensionType; // If pensioner - FAMILY/COMMUTED/UNCOMMUTED
        @Builder.Default
        private double salaryReceived = 0;
        @Builder.Default
        private double tdsDeducted = 0;
        private LocalDate periodFrom;
        private LocalDate periodTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExemptAllowanceDetail {
        private String allowanceType; // HRA, LTA, EDUCATION, HOSTEL, TRANSPORT
        private String section; // 10(13A), 10(5), 10(14)
        @Builder.Default
        private double amountReceived = 0;
        @Builder.Default
        private double exemptAmount = 0;
        @Builder.Default
        private double taxableAmount = 0;
        
        // HRA specific
        @Builder.Default
        private double rentPaid = 0;
        private String city;
        @Builder.Default
        private boolean metroCity = false;
        private String landlordName;
        private String landlordPAN;
        private String landlordAddress;
    }

    /**
     * Schedule House Property - Up to 2 properties for ITR-1 (AY 2026-27)
     * Now supports detailed fields per CBDT schema including:
     * - AddressDetailWithZipCode
     * - CoOwner array
     * - TenantDetails array
     * - Section24B loan details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HousePropertyIncome {
        // Property Type - SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT
        private String propertyType;
        
        // Address Detail - NEW for AY 2026-27 per AddressDetailWithZipCode
        private String address;         // AddrDetail
        private String city;            // CityOrTownOrDistrict
        private String state;           // StateCode (01-37, 99)
        private String pinCode;         // PinCode (6-digit)
        private String countryCode;     // CountryCode (default 91)
        private String propertyIdentificationNo; // Survey/Plot No
        
        // Property Ownership - NEW for AY 2026-27
        private String propertyOwner;   // SE/MI/SP/OT
        @Builder.Default
        private boolean isPropertyCoOwned = false; // PropCoOwnedFlg (YES/NO)
        @Builder.Default
        private boolean isPropertyInJointOwnership = false;
        private String ownershipType;   // SOLE/JOINT
        @Builder.Default
        private double ownershipShare = 100.0; // Percentage (0-100)
        
        // For Let-Out Property
        @Builder.Default
        private double annualRent = 0;
        @Builder.Default
        private double annualLettingValue = 0;
        @Builder.Default
        private double municipalRateableValue = 0;
        @Builder.Default
        private double grossAnnualValue = 0;
        @Builder.Default
        private double vacancyPeriodMonths = 0;
        @Builder.Default
        private double unrealizedRent = 0;
        @Builder.Default
        private double arrearsOfRent = 0;
        
        // Deductions
        @Builder.Default
        private double municipalTaxesPaid = 0;
        @Builder.Default
        private double netAnnualValue = 0;
        @Builder.Default
        private double standardDeduction30Pct = 0;
        @Builder.Default
        private double interestOnLoan = 0;
        
        // Final Income/Loss
        @Builder.Default
        private double incomeFromHP = 0;
        
        // Loan Details - ENHANCED for AY 2026-27 Section 24B
        private String lenderName;
        private String lenderPAN;
        private String lenderType; // B (Bank) or I (Institution)
        private String loanAccountNo;
        @Builder.Default
        private double principalRepayment = 0;
        @Builder.Default
        private double preConstructionInterest = 0;
        private LocalDate loanDate;
        private LocalDate completionDate;
        @Builder.Default
        private boolean completedWithin5Years = true;
        
        // Section24B Detailed Loan Info - NEW for AY 2026-27
        @Builder.Default
        private List<Section24BLoan> section24BLoans = new ArrayList<>();
        
        // Tenant Details - EXPANDED for AY 2026-27
        @Builder.Default
        private List<TenantDetail> tenantDetails = new ArrayList<>();
        
        // Legacy tenant fields for backward compatibility
        private String tenantName;
        private String tenantPAN;
        
        // Co-owner Details - Already supported
        
        // Co-owner Details - Already supported
        @Builder.Default
        private List<CoOwner> coOwners = new ArrayList<>();
    }

    /**
     * Section24B Loan Detail - NEW for AY 2026-27
     * Stores detailed loan information for interest deduction u/s 24B
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section24BLoan {
        private String loanType;           // B = Bank, I = Other than Bank
        private String bankOrInstnName;   // Name of Bank/Institution
        private String loanAccNo;         // Loan Account Number
        private LocalDate dateOfLoan;      // Date of loan taken
        @Builder.Default
        private double totalLoanAmt = 0;
        @Builder.Default
        private double loanOutstandingAmt = 0;
        @Builder.Default
        private double interestUs24B = 0;  // Interest claimed u/s 24B
    }

    /**
     * Tenant Details - EXPANDED for AY 2026-27
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantDetail {
        @Builder.Default
        private int tenantSNo = 1;
        private String nameOfTenant;
        private String panOfTenant;
        private String aadhaarOfTenant;
        private String panTanOfTenant; // PAN or TAN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoOwner {
        @Builder.Default
        private int coOwnerSNo = 1;
        private String name;
        private String pan;
        private String aadhaar;
        @Builder.Default
        private double ownershipShare = 0;
    }

    /**
     * Schedule Other Sources - CBDT Schedule OS
     * Section 56 - Income from other sources
     * 101% CBDT Compliant for AY 2025-26
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherSourcesIncome {
        // ===== INTEREST INCOME (Section 56) =====
        @Builder.Default
        private double savingsAccountInterest = 0;
        @Builder.Default
        private double fixedDepositInterest = 0;
        @Builder.Default
        private double recurringDepositInterest = 0;
        @Builder.Default
        private double nscInterest = 0;
        @Builder.Default
        private double scssInterest = 0;  // Senior Citizen Savings Scheme
        @Builder.Default
        private double postOfficeInterest = 0;
        @Builder.Default
        private double otherInterest = 0;
        @Builder.Default
        private double totalInterestIncome = 0;
        
        // 80TTA/80TTB Deduction tracking
        @Builder.Default
        private double interestEligibleFor80TTA = 0;  // SB interest max ₹10K
        @Builder.Default
        private double interestEligibleFor80TTB = 0;  // Senior citizen SB/FD interest max ₹50K
        
        // ===== DIVIDEND INCOME (Section 8, 115BBDA) =====
        @Builder.Default
        private double dividendFromShares = 0;
        @Builder.Default
        private double dividendFromMutualFunds = 0;
        @Builder.Default
        private double dividendFromUnits = 0;
        @Builder.Default
        private double totalDividendIncome = 0;
        
        // Section 115BBDA: Dividend > ₹10L @ 10%
        @Builder.Default
        private double dividendTaxableAtNormalRate = 0;  // Up to ₹10L
        @Builder.Default
        private double dividendTaxableAtSpecialRate = 0;  // Above ₹10L @ 10%
        @Builder.Default
        private boolean dividendExceeds10L = false;
        
        // ===== FAMILY PENSION (Section 56) =====
        @Builder.Default
        private double familyPensionReceived = 0;
        @Builder.Default
        private double familyPensionDeduction = 0; // 1/3 or ₹15K/₹25K whichever is lower
        @Builder.Default
        private double familyPensionTaxable = 0;
        
        // ===== WINNINGS FROM LOTTERY/BETTING (Section 115BB) - TAXED @ 30% =====
        @Builder.Default
        private double lotteryIncome = 0;  // Section 115BB - 30%
        @Builder.Default
        private double crosswordPuzzleIncome = 0;  // Section 115BB - 30%
        @Builder.Default
        private double horseRaceIncome = 0;  // Section 115BB - 30%
        @Builder.Default
        private double cardGameIncome = 0;  // Section 115BB - 30%
        @Builder.Default
        private double totalWinningsIncome = 0;
        @Builder.Default
        private double winningsTaxRate = 0.30;  // 30% flat
        
        // ===== INCOME FROM OTHER SOURCES =====
        @Builder.Default
        private double incomeFromITRefund = 0; // u/s 244A interest on IT refund
        @Builder.Default
        private double incomeFromChristmass = 0; // Section 10(3) - tax-exempt
        @Builder.Default
        private double keyManInsuranceBonus = 0; // Section 10(4) - tax-exempt
        @Builder.Default
        private double accumulatedSPF = 0; // Section 57 - Superannuation fund
        @Builder.Default
        private double giftsFromNonRelatives = 0; // > ₹50K taxable u/s 56(2)(x)
        @Builder.Default
        private double giftsFromRelatives = 0; // Section 56(2)(x) - exempt
        @Builder.Default
        private double casualIncome = 0; // Prize money (not lottery)
        @Builder.Default
        private double agriculturalIncome = 0; // Section 10(1)
        @Builder.Default
        private double otherIncome = 0;
        
        // ===== TOTAL =====
        @Builder.Default
        private double totalOtherSourcesIncome = 0;
        
        // ===== Separately tracked for tax computation =====
        @Builder.Default
        private double incomeTaxedAtSpecialRates = 0;  // 115BB, 115BBDA
        @Builder.Default
        private double normalIncome = 0;  // Added to GTI
        
        // Supporting Details - Multi-entry for CBDT compliance
        @Builder.Default
        private List<BankInterestDetail> bankInterestDetails = new ArrayList<>();
        @Builder.Default
        private List<DividendDetail> dividendDetails = new ArrayList<>();
        @Builder.Default
        private List<GiftDetail> giftDetails = new ArrayList<>();
    }
    
    /**
     * Gift Details for CBDT Schedule OS
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiftDetail {
        private String donorName;
        private String donorPAN;
        private String relationship;  // RELATIVE/NON_RELATIVE
        @Builder.Default
        private double amountReceived = 0;
        @Builder.Default
        private double amountTaxable = 0;
        private LocalDate dateOfReceipt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInterestDetail {
        private String bankName; // MANDATORY
        private String ifscCode; // MANDATORY
        private String accountNo; // MANDATORY (can be masked)
        private String accountType; // SAVINGS, FD, RD - MANDATORY
        @Builder.Default
        private double interestEarned = 0; // MANDATORY
        @Builder.Default
        private double tdsDeducted = 0;
        private String deductorTAN; // MANDATORY if TDS deducted
        private String certificateNo; // If TDS deducted
        private LocalDate interestCreditDate; // MANDATORY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendDetail {
        private String companyName;
        private String isin;
        @Builder.Default
        private double dividendReceived = 0;
        @Builder.Default
        private double tdsDeducted = 0;
        private String deductorTAN;
    }

    /**
     * Chapter VI-A Deductions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deductions {
        // 80C Group (combined limit ₹1.5L)
        @Builder.Default
        private double lic = 0;
        @Builder.Default
        private double ppf = 0;
        @Builder.Default
        private double elss = 0;
        @Builder.Default
        private double epfEmployee = 0;
        @Builder.Default
        private double vpf = 0;
        @Builder.Default
        private double nsc = 0;
        @Builder.Default
        private double sukanyaSamriddhi = 0;
        @Builder.Default
        private double homeLoanPrincipal = 0;
        @Builder.Default
        private double tuitionFees = 0;
        @Builder.Default
        private double ulip = 0;
        @Builder.Default
        private double seniorCitizenSavings = 0;
        @Builder.Default
        private double fiveYearBankFD = 0;
        @Builder.Default
        private double other80C = 0;
        @Builder.Default
        private double total80C = 0;
        @Builder.Default
        private double deduction80C = 0; // Min(total, 150000)
        
        // NPS
        @Builder.Default
        private double npsEmployee80CCD1 = 0; // Within 80C limit
        @Builder.Default
        private double npsEmployee80CCD1B = 0; // Extra 50K
        @Builder.Default
        private double npsEmployer80CCD2 = 0; // 10%/14% of salary
        
        // Health Insurance 80D
        @Builder.Default
        private double healthInsuranceSelf = 0;
        @Builder.Default
        private double healthInsuranceParents = 0;
        @Builder.Default
        private double preventiveHealthCheckup = 0;
        @Builder.Default
        private double deduction80D = 0;
        @Builder.Default
        private boolean selfSeniorCitizen = false;
        @Builder.Default
        private boolean parentsSeniorCitizen = false;
        
        // Other Deductions
        @Builder.Default
        private double deduction80DD = 0; // Disabled dependent
        @Builder.Default
        private double deduction80DDB = 0; // Medical treatment
        @Builder.Default
        private double deduction80E = 0; // Education loan interest
        @Builder.Default
        private double deduction80EE = 0; // Home loan interest (first buyer)
        @Builder.Default
        private double deduction80EEA = 0; // Affordable housing
        @Builder.Default
        private double deduction80EEB = 0; // EV loan interest
        @Builder.Default
        private double deduction80G = 0; // Donations
        @Builder.Default
        private double deduction80GG = 0; // Rent paid (no HRA)
        @Builder.Default
        private double deduction80GGC = 0; // Political party donation
        @Builder.Default
        private double deduction80TTA = 0; // Savings interest (below 60)
        @Builder.Default
        private double deduction80TTB = 0; // All interest (60+)
        @Builder.Default
        private double deduction80U = 0; // Self disabled
        
        // Total Deductions
        @Builder.Default
        private double totalDeductions = 0;
        
        // Supporting Details
        @Builder.Default
        private List<Deduction80CItem> deduction80CBreakdown = new ArrayList<>();
        @Builder.Default
        private List<Donation80GItem> deduction80GBreakdown = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deduction80CItem {
        private String itemType; // LIC, PPF, ELSS, etc. - MANDATORY
        private String investmentType; // MANDATORY - LIC/PPF/ELSS/NSC/etc.
        private String description;
        @Builder.Default
        private double amount = 0; // MANDATORY
        private String policyNo;
        private String policyHolderName; // MANDATORY for LIC
        private String policyHolderPAN; // MANDATORY if different from assessee
        private String insurerName; // MANDATORY
        private String insurerPAN; // MANDATORY
        private String receiptNo;
        private LocalDate paymentDate;
        private LocalDate investmentDate; // MANDATORY
        private String paymentMode; // MANDATORY - CASH/CHEQUE/NEFT/UPI
        private String instrumentNo; // MANDATORY if cheque/DD
        private LocalDate instrumentDate; // MANDATORY if cheque/DD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Donation80GItem {
        private String doneeName;
        private String doneePAN;
        private String doneeAddress;
        @Builder.Default
        private double donationAmount = 0;
        private String paymentMode; // CASH, CHEQUE, NEFT, UPI
        private String category; // 100_NO_LIMIT, 100_WITH_LIMIT, 50_NO_LIMIT, 50_WITH_LIMIT
        private String receiptNo;
        private LocalDate donationDate;
        @Builder.Default
        private double eligibleDeduction = 0;
        private String disallowedReason;
    }

    /**
     * Tax Payments - TDS, TCS, Advance Tax
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPayments {
        // TDS on Salary
        @Builder.Default
        private List<TDSOnSalary> tdsOnSalary = new ArrayList<>();
        @Builder.Default
        private double totalTDSOnSalary = 0;
        
        // TDS on Other Income
        @Builder.Default
        private List<TDSOnOther> tdsOnOther = new ArrayList<>();
        @Builder.Default
        private double totalTDSOnOther = 0;
        
        // TCS
        @Builder.Default
        private List<TCSEntry> tcsEntries = new ArrayList<>();
        @Builder.Default
        private double totalTCS = 0;
        
        // Advance Tax
        @Builder.Default
        private List<AdvanceTaxEntry> advanceTaxEntries = new ArrayList<>();
        @Builder.Default
        private double totalAdvanceTax = 0;
        
        // Self Assessment Tax
        @Builder.Default
        private List<SelfAssessmentTaxEntry> selfAssessmentTaxEntries = new ArrayList<>();
        @Builder.Default
        private double totalSelfAssessmentTax = 0;
        
        // Total
        @Builder.Default
        private double totalTaxesPaid = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOnSalary {
        private String employerName;
        private String employerTAN;
        private String tan; // Alias for employerTAN
        @Builder.Default
        private double salaryAmount = 0;
        @Builder.Default
        private double totalSalary = 0; // Alias for salaryAmount
        @Builder.Default
        private double tdsAmount = 0;
        @Builder.Default
        private double taxDeducted = 0; // Alias for tdsAmount
        private LocalDate periodFrom;
        private LocalDate periodTo;
        @Builder.Default
        private boolean verified26AS = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOnOther {
        private String deductorName; // MANDATORY
        private String deductorTAN; // MANDATORY
        private String deductorPAN; // MANDATORY for certain deductors
        private String section; // 194A, 194C, 194J, etc. - MANDATORY
        @Builder.Default
        private double incomeAmount = 0; // MANDATORY
        @Builder.Default
        private double grossAmount = 0; // Alias for incomeAmount
        @Builder.Default
        private double tdsAmount = 0; // MANDATORY
        @Builder.Default
        private double taxDeducted = 0; // Alias for tdsAmount
        @Builder.Default
        private double amountClaimed = 0; // Amount claimed (usually same as tdsAmount)
        private String certificateNo; // MANDATORY
        private LocalDate deductionDate; // MANDATORY
        private String uniqueTransactionNo; // MANDATORY from AY 2024-25
        private String financialYear; // MANDATORY
        private String assessmentYear; // MANDATORY
        @Builder.Default
        private double incomeAmountCredited = 0; // MANDATORY (vs paid)
        private LocalDate dateOfCredit; // MANDATORY
        private LocalDate dateOfPayment; // MANDATORY
        @Builder.Default
        private boolean tdsClaimed = true; // MANDATORY - YES/NO
        private String reasonForNonClaim; // If not claimed
        @Builder.Default
        private boolean verified26AS = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TCSEntry {
        private String collectorName;
        private String collectorTAN;
        private String section; // 206C(1G), etc.
        @Builder.Default
        private double collectionAmount = 0;
        @Builder.Default
        private double tcsAmount = 0;
        private String certificateNo;
        private LocalDate collectionDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvanceTaxEntry {
        private String bsrCode;
        private String challanNo;
        private LocalDate depositDate;
        @Builder.Default
        private double amount = 0;
        private String cin; // Challan Identification Number
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelfAssessmentTaxEntry {
        private String bsrCode;
        private String challanNo;
        private LocalDate depositDate;
        @Builder.Default
        private double amount = 0;
        private String cin;
    }

    /**
     * Schedule EI - Exempt Income - 101% CBDT Compliant
     * All incomes exempt under Section 10
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExemptIncome {
        // Agricultural Income (Section 10(1))
        // For ITR-1: Max ₹5,000 allowed; >₹5,000 requires ITR-2
        @Builder.Default
        private double agricultureIncome = 0;
        @Builder.Default
        private boolean agricultureIncomeExceeds5000 = false; // Triggers ITR-2
        
        // Section 10(10) - Gratuity
        @Builder.Default
        private double gratuityExempt = 0;
        
        // Section 10(10AA) - Leave Encashment
        @Builder.Default
        private double leaveEncashmentExempt = 0;
        
        // Section 10(10A) - Pension Commutation
        @Builder.Default
        private double commutationOfPension = 0;
        
        // Section 10(10B) - Compensation from Statutory Fund
        @Builder.Default
        private double statutoryFundCompensation = 0;
        
        // Section 10(10C) - VRS Compensation
        @Builder.Default
        private double vrsCompensationExempt = 0;
        
        // Section 10(11) - Recognized Provident Fund
        @Builder.Default
        private double rpfInterest = 0;
        
        // Section 10(12) - Approved Superannuation Fund
        @Builder.Default
        private double superannuationMaturity = 0;
        
        // Section 10(13) - Approved Gratuity Fund
        @Builder.Default
        private double gratuityFundMaturity = 0;
        
        // PPF Interest (Section 10(11))
        @Builder.Default
        private double ppfInterest = 0;
        
        // Sukanya Samriddhi Interest (Section 10(11))
        @Builder.Default
        private double sukanyaSamriddhiInterest = 0;
        
        // Tax Free Bonds Interest (Section 10(15))
        @Builder.Default
        private double taxFreeBondsInterest = 0;
        
        // LIC Maturity (Section 10(10D)) - Exempt if premium <= 10% of sum assured
        @Builder.Default
        private double licMaturityProceeds = 0;
        
        // Section 10(15) - Other tax-free interest
        @Builder.Default
        private double otherTaxFreeInterest = 0;
        
        // Dividend from Indian companies (Section 10(34)) - but taxable under 115BBDA
        @Builder.Default
        private double dividendExempt = 0; // Under ₹10,000 per company is exempt from TDS
        
        // Share of Profit from HUF (Section 10(2))
        @Builder.Default
        private double hufIncome = 0;
        
        // Any Other Exempt Income
        @Builder.Default
        private double otherExemptIncome = 0;
        
        // Total Exempt Income
        @Builder.Default
        private double totalExemptIncome = 0;
        
        // Supporting Details
        @Builder.Default
        private List<ExemptIncomeDetail> exemptIncomeDetails = new ArrayList<>();
        
        // ===== BACKWARD COMPATIBILITY ALIASES =====
        public double getTaxFreeBodsInterest() {
            return this.taxFreeBondsInterest;
        }
        
        public double getShareOfProfitFromFirm() {
            return this.hufIncome;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExemptIncomeDetail {
        private String incomeType; // AGRICULTURE, GRATUITY, LEAVE_ENCASHMENT, VRS, etc.
        private String section; // 10(1), 10(10), 10(10AA), etc.
        @Builder.Default
        private double amount = 0;
        private String description;
        private String payerName;
        private String payerPAN;
    }

    /**
     * Tax Computation - 101% CBDT Compliant
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxComputation {
        // Income Summary - All 5 Income Heads
        @Builder.Default
        private double incomeFromSalary = 0;
        @Builder.Default
        private double incomeFromHP = 0;
        @Builder.Default
        private double incomeFromCapitalGains = 0;
        @Builder.Default
        private double incomeFromBusiness = 0;
        @Builder.Default
        private double incomeFromOtherSources = 0;
        
        // Agricultural Income Clubbing (Section 17(1) + Section 10(1))
        @Builder.Default
        private double agricultureIncome = 0; // From Exempt Income
        @Builder.Default
        private double agricultureIncomeAbove5000 = 0; // Portion > ₹5,000 to be clubbed
        @Builder.Default
        private boolean clubAgriculturalIncome = false;
        @Builder.Default
        private double netAgricultureIncome = 0; // After set-off of losses
        
        // Gross Total Income (GTI) - sum of all income heads + clubbed agri income
        @Builder.Default
        private double grossTotalIncome = 0;
        
        // HP Loss Set-off (Section 71)
        @Builder.Default
        private double hpLoss = 0;
        @Builder.Default
        private double hpLossSetOff = 0; // Max 2L
        @Builder.Default
        private double hpLossCarryForward = 0; // Must file ITR-2 if > 0
        
        // Business Loss Set-off (Section 71)
        @Builder.Default
        private double businessLossSetOff = 0;
        
        // Capital Losses Set-off (Section 74)
        @Builder.Default
        private double shortTermCapitalLossSetOff = 0;
        @Builder.Default
        private double longTermCapitalLossSetOff = 0;
        
        // Deductions under Chapter VI-A
        @Builder.Default
        private double deduction80C = 0;
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
        private double deduction80G = 0;
        @Builder.Default
        private double deduction80GG = 0;
        @Builder.Default
        private double deduction80TTA = 0;
        @Builder.Default
        private double deduction80TTB = 0;
        @Builder.Default
        private double deduction80U = 0;
        @Builder.Default
        private double totalDeductions = 0;
        
        // Total Income (Taxable Income)
        @Builder.Default
        private double totalIncome = 0;
        
        // === Special Rate Incomes ===
        
        // Short Term Capital Gains (Section 111A) - 15%
        @Builder.Default
        private double stcg111A = 0;
        @Builder.Default
        private double stcg111ATax = 0;
        
        // Long Term Capital Gains (Section 112A) - 10% above ₹1L
        @Builder.Default
        private double ltcg112A = 0;
        @Builder.Default
        private double ltcg112ATax = 0;
        
        // Long Term Capital Gains (Section 112) - 20% with indexation
        @Builder.Default
        private double ltcg112 = 0;
        @Builder.Default
        private double ltcg112Tax = 0;
        
        // Dividend Income (Section 115BBDA) - 10% above ₹10K per company
        @Builder.Default
        private double dividendIncome = 0;
        @Builder.Default
        private double dividendTaxableAmount = 0;
        @Builder.Default
        private double dividendTax = 0;
        
        // Lottery/Winnings (Section 115BBE) - 30%
        @Builder.Default
        private double lotteryIncome = 0;
        @Builder.Default
        private double lotteryTax = 0;
        
        // Total Special Rate Tax
        @Builder.Default
        private double specialRateIncomeTotal = 0;
        @Builder.Default
        private double specialRateTaxTotal = 0;
        
        // Tax Calculation
        @Builder.Default
        private double taxOnNormalIncome = 0;
        @Builder.Default
        private double rebate87A = 0;
        @Builder.Default
        private double taxAfterRebate = 0;
        
        // Surcharge
        @Builder.Default
        private double surcharge = 0;
        @Builder.Default
        private double marginalRelief = 0;
        @Builder.Default
        private double surchargeRate = 0;
        
        // Health & Education Cess
        @Builder.Default
        private double cess = 0;
        
        // Total Tax Liability
        @Builder.Default
        private double totalTaxLiability = 0;
        
        // Relief u/s 89 (for irregular income)
        @Builder.Default
        private double relief89 = 0;
        @Builder.Default
        private boolean form10EFiled = false;
        
        // Tax Payable after Relief
        @Builder.Default
        private double taxPayableAfterRelief = 0;
        
        // Taxes Paid (From Form 26AS)
        @Builder.Default
        private double totalTDS = 0;
        @Builder.Default
        private double totalTCS = 0;
        @Builder.Default
        private double advanceTaxPaid = 0;
        @Builder.Default
        private double selfAssessmentTaxPaid = 0;
        @Builder.Default
        private double totalTaxesPaid = 0;
        
        // Balance Tax (before filing)
        @Builder.Default
        private double balanceTax = 0;
        
        // Interest and Late Filing Fees
        @Builder.Default
        private double interest234A = 0; // Late filing
        @Builder.Default
        private double interest234B = 0; // Advance tax shortfall
        @Builder.Default
        private double interest234C = 0; // Installment default
        @Builder.Default
        private double fee234F = 0; // Late filing fee
        @Builder.Default
        private double totalInterestAndFees = 0;
        
        // Final Demand / Refund
        @Builder.Default
        private double taxPayable = 0;
        @Builder.Default
        private double refund = 0;
        
        // === Regime Comparison for Tax Planning ===
        private RegimeComparison oldRegime;
        private RegimeComparison newRegime;
        private String selectedRegime;
        @Builder.Default
        private double taxSavings = 0;
        
        // Computation Log / Steps for Audit Trail
        @Builder.Default
        private List<String> computationSteps = new ArrayList<>();
        
        // ===== BACKWARD COMPATIBILITY ALIASES FOR LEGACY CODE =====
        // These methods provide backward compatibility with code that expects old field names
        
        /**
         * Alias for total CG tax calculation - backward compatibility
         */
        public double getTotalCGTax() {
            return this.stcg111ATax + this.ltcg112ATax + this.ltcg112Tax + this.dividendTax + this.lotteryTax;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegimeComparison {
        private String regime;
        @Builder.Default
        private double totalIncome = 0;
        @Builder.Default
        private double taxOnIncome = 0;
        @Builder.Default
        private double rebate87A = 0;
        @Builder.Default
        private double surcharge = 0;
        @Builder.Default
        private double cess = 0;
        @Builder.Default
        private double totalTax = 0;
        @Builder.Default
        private double netTaxPayable = 0;
    }
}
