package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ITR-1 (Sahaj) Form Data - 101% CBDT Compliant
 * For Resident Individuals with:
 * - Salary/Pension income
 * - ONE house property (no carried forward loss)
 * - Other sources (interest, dividend)
 * - Total income ≤ ₹50 lakh
 * - No capital gains, business income, or foreign assets
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
    
    // Deductions
    private Deductions deductions;
    
    // Tax Schedules
    private TaxPayments taxPayments;
    private ExemptIncome exemptIncome;
    
    // Computation
    private TaxComputation taxComputation;
    
    // Validation
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    @Builder.Default
    private List<String> validationWarnings = new ArrayList<>();

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
        private double daAmount = 0; // Dearness Allowance
        @Builder.Default
        private double bonusAmount = 0;
        @Builder.Default
        private double commissionAmount = 0; // CBDT mandatory
        @Builder.Default
        private double hraReceived = 0; // HRA received (before exemption)
        @Builder.Default
        private double ltaReceived = 0; // LTA received (before exemption)
        @Builder.Default
        private double ceaReceived = 0; // Children Education Allowance received
        @Builder.Default
        private double otherAllowance = 0; // Any other allowance
        @Builder.Default
        private double salary17_1 = 0; // Basic + DA + Bonus + Commission
        @Builder.Default
        private double perquisites17_2 = 0; // Rent-free accommodation, car, etc.
        @Builder.Default
        private double profitsInLieu17_3 = 0; // Gratuity excess, leave encashment excess (Section 17(3))
        @Builder.Default
        private double grossSalary = 0;
        
        // Exempt Allowances u/s 10
        @Builder.Default
        private double hraExempt = 0; // u/s 10(13A)
        @Builder.Default
        private double ltaExempt = 0; // u/s 10(5)
        @Builder.Default
        private double ceaExempt = 0; // u/s 10(14) - max ₹100/month per child, 2 children
        @Builder.Default
        private double childrenEducationExempt = 0;
        @Builder.Default
        private double hostelExpenditureExempt = 0;
        @Builder.Default
        private double transportAllowanceExempt = 0; // For disabled employees
        @Builder.Default
        private double otherAllowancesExempt = 0;
        @Builder.Default
        private double otherExempt = 0; // Any other exempt allowance
        @Builder.Default
        private double totalExemptAllowances = 0;
        
        // Net Salary
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
        
        // Supporting data
        @Builder.Default
        private double daForRetirement = 0;
        @Builder.Default
        private boolean isGovernmentEmployee = false;
        @Builder.Default
        private List<EmployerDetails> employers = new ArrayList<>();
        @Builder.Default
        private List<ExemptAllowanceDetail> exemptAllowanceDetails = new ArrayList<>();
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
     * Schedule House Property - ONE property only for ITR-1
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HousePropertyIncome {
        private String propertyType; // SELF_OCCUPIED, LET_OUT
        private String address;
        private String city;
        private String state;
        private String pinCode;
        private String propertyIdentificationNo; // CBDT MANDATORY - Survey/Plot No
        @Builder.Default
        private boolean isPropertyCoOwned = false; // CBDT MANDATORY question
        @Builder.Default
        private boolean isPropertyInJointOwnership = false; // CBDT MANDATORY
        private String ownershipType; // SOLE/JOINT - CBDT MANDATORY
        @Builder.Default
        private double ownershipShare = 100.0; // Percentage
        
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
        private double vacancyPeriodMonths = 0; // CBDT MANDATORY for let-out
        @Builder.Default
        private double unrealizedRent = 0; // CBDT MANDATORY if applicable
        
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
        
        // Loan Details
        private String lenderName;
        private String lenderPAN;
        private String loanAccountNo;
        @Builder.Default
        private double principalRepayment = 0; // For 80C
        @Builder.Default
        private double preConstructionInterest = 0;
        private LocalDate loanDate;
        private LocalDate completionDate;
        @Builder.Default
        private boolean completedWithin5Years = true;
        
        // Tenant Details (if let-out)
        private String tenantName;
        private String tenantPAN;
        
        // Co-owner Details
        @Builder.Default
        private List<CoOwner> coOwners = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoOwner {
        private String name;
        private String pan;
        @Builder.Default
        private double ownershipShare = 0;
    }

    /**
     * Schedule Other Sources
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherSourcesIncome {
        // Interest Income
        @Builder.Default
        private double savingsAccountInterest = 0;
        @Builder.Default
        private double fixedDepositInterest = 0;
        @Builder.Default
        private double recurringDepositInterest = 0;
        @Builder.Default
        private double nscInterest = 0;
        @Builder.Default
        private double scssInterest = 0;
        @Builder.Default
        private double otherInterest = 0;
        @Builder.Default
        private double totalInterestIncome = 0;
        
        // Dividend Income
        @Builder.Default
        private double dividendFromShares = 0;
        @Builder.Default
        private double dividendFromMutualFunds = 0;
        @Builder.Default
        private double totalDividendIncome = 0;
        
        // Family Pension
        @Builder.Default
        private double familyPensionReceived = 0;
        @Builder.Default
        private double familyPensionDeduction = 0; // 1/3 or 15K/25K
        @Builder.Default
        private double familyPensionTaxable = 0;
        
        // Other Income
        @Builder.Default
        private double incomeFromITRefund = 0; // u/s 244A interest on IT refund
        @Builder.Default
        private double lotteryIncome = 0; // u/s 115BB - taxed @ 30% + surcharge + cess
        @Builder.Default
        private double horseRaceIncome = 0; // u/s 115BB - taxed @ 30% + surcharge + cess
        @Builder.Default
        private double giftsFromNonRelatives = 0; // > ₹50K taxable
        @Builder.Default
        private double casualIncome = 0; // Prize money (not lottery)
        @Builder.Default
        private double otherIncome = 0;
        
        // Total
        @Builder.Default
        private double totalOtherSourcesIncome = 0;
        
        // Supporting Details
        @Builder.Default
        private List<BankInterestDetail> bankInterestDetails = new ArrayList<>();
        @Builder.Default
        private List<DividendDetail> dividendDetails = new ArrayList<>();
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
     * Schedule EI - Exempt Income
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExemptIncome {
        @Builder.Default
        private double agricultureIncome = 0; // Max 5K for ITR-1
        @Builder.Default
        private double ppfInterest = 0;
        @Builder.Default
        private double sukanyaSamriddhiInterest = 0;
        @Builder.Default
        private double taxFreeBodsInterest = 0;
        @Builder.Default
        private double licMaturityProceeds = 0;
        @Builder.Default
        private double gratuityExempt = 0;
        @Builder.Default
        private double leaveEncashmentExempt = 0;
        @Builder.Default
        private double vrsCompensationExempt = 0;
        @Builder.Default
        private double commutationOfPension = 0;
        @Builder.Default
        private double shareOfProfitFromFirm = 0;
        @Builder.Default
        private double hufIncome = 0;
        @Builder.Default
        private double otherExemptIncome = 0;
        @Builder.Default
        private double totalExemptIncome = 0;
    }

    /**
     * Tax Computation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxComputation {
        // Income Summary
        @Builder.Default
        private double incomeFromSalary = 0;
        @Builder.Default
        private double incomeFromHP = 0;
        @Builder.Default
        private double incomeFromOtherSources = 0;
        @Builder.Default
        private double grossTotalIncome = 0;
        
        // HP Loss Set-off
        @Builder.Default
        private double hpLoss = 0;
        @Builder.Default
        private double hpLossSetOff = 0; // Max 2L
        @Builder.Default
        private double hpLossCarryForward = 0; // Must file ITR-2 if > 0
        
        // Deductions
        @Builder.Default
        private double totalDeductions = 0;
        
        // Total Income
        @Builder.Default
        private double totalIncome = 0;
        
        // Capital Gains (for ITR-1 eligible CG only)
        @Builder.Default
        private double ltcgAmount = 0; // LTCG u/s 112A (listed equity/MF with STT)
        @Builder.Default
        private double stcgAmount = 0; // STCG u/s 111A (listed equity/MF with STT)
        @Builder.Default
        private double totalCGTax = 0; // Combined CG tax
        
        // Tax Calculation
        @Builder.Default
        private double taxOnNormalIncome = 0;
        @Builder.Default
        private double rebate87A = 0;
        @Builder.Default
        private double taxAfterRebate = 0;
        @Builder.Default
        private double surcharge = 0;
        @Builder.Default
        private double marginalRelief = 0;
        @Builder.Default
        private double cess = 0;
        @Builder.Default
        private double totalTaxLiability = 0;
        
        // Relief u/s 89
        @Builder.Default
        private double relief89 = 0;
        @Builder.Default
        private boolean form10EFiled = false;
        
        // Tax Payable
        @Builder.Default
        private double taxPayableAfterRelief = 0;
        @Builder.Default
        private double totalTaxesPaid = 0;
        @Builder.Default
        private double balanceTax = 0;
        
        // Interest and Fees
        @Builder.Default
        private double interest234A = 0;
        @Builder.Default
        private double interest234B = 0;
        @Builder.Default
        private double interest234C = 0;
        @Builder.Default
        private double fee234F = 0;
        @Builder.Default
        private double totalInterestAndFees = 0;
        
        // Final Demand/Refund
        @Builder.Default
        private double taxPayable = 0;
        @Builder.Default
        private double refund = 0;
        
        // Regime Comparison
        private RegimeComparison oldRegime;
        private RegimeComparison newRegime;
        private String selectedRegime;
        @Builder.Default
        private double taxSavings = 0;
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
