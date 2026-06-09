package com.itr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * House Property Data Transfer Object
 * CBDT Compliant for AY 2026-27
 * 
 * Supports:
 * - Up to 2 house properties for ITR-1 (NEW for AY 2026-27)
 * - Multiple properties for ITR-2/3/4
 * - All property types: Self-Occupied, Let-Out, Deemed Let-Out
 * - Section 24B detailed loan disclosure
 * - Co-owner details
 * - Tenant details
 * 
 * Used across all ITR forms (ITR-1, ITR-2, ITR-3, ITR-4)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HouseProperty {

    // ===== Property Identification =====
    
    /**
     * Property Sequence Number in the return
     * For ITR-1: 1 or 2 (NEW for AY 2026-27)
     * For other ITRs: Multiple properties allowed
     */
    @Builder.Default
    private int propertySequenceNo = 1;
    
    /**
     * Property Type
     * Values: SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT
     */
    private String propertyType;
    
    // ===== Full Address Details (Required for AY 2026-27) =====
    
    /**
     * Flat/Door/Block No. - AddrDetail
     */
    private String address;
    
    /**
     * Name of Premises/Building - ResidenceName
     */
    private String premisesName;
    
    /**
     * Road/Street - RoadOrStreet
     */
    private String roadOrStreet;
    
    /**
     * Area/Locality - LocalityOrArea
     */
    private String area;
    
    /**
     * City/Town/District - CityOrTownOrDistrict (MANDATORY)
     */
    private String city;
    
    /**
     * State - StateCode (MANDATORY, format: 01-37, 99)
     */
    private String state;
    
    /**
     * PIN Code - PinCode (MANDATORY, 6 digits)
     */
    private String pinCode;
    
    /**
     * Country Code - CountryCode (Default: 91 for India)
     */
    @Builder.Default
    private String countryCode = "91";
    
    /**
     * Property Identification (Survey/Plot/Khasra Number)
     */
    private String propertyIdentificationNo;
    
    // ===== Ownership Details (Required for AY 2026-27) =====
    
    /**
     * Property Owner Type
     * SE = Single Owner
     * MI = Minor
     * SP = Self + Spouse
     * OT = Others
     */
    @Builder.Default
    private String propertyOwnerType = "SE";
    
    /**
     * Whether property is Co-Owned
     * YES/NO (PropCoOwnedFlg)
     */
    @Builder.Default
    private boolean isCoOwned = false;
    
    /**
     * Co-Owner Details List (MANDATORY if isCoOwned = true)
     */
    @Builder.Default
    private List<CoOwner> coOwners = new ArrayList<>();
    
    /**
     * Assessee's Share of Property (0-100%)
     * Default: 100.0 for single owner
     */
    @Builder.Default
    private double assesseeShareProperty = 100.0;
    
    // ===== Let-Out Property Income Fields =====
    
    /**
     * Gross Annual Rent / Annual Letable Value
     * Received or receivable from let-out property
     */
    @Builder.Default
    private double annualRent = 0;
    
    /**
     * Municipal/Local Authority Rateable Value
     */
    @Builder.Default
    private double municipalRateableValue = 0;
    
    /**
     * Fair Rent as per Rent Control Act
     * Used to determine Maximum Rent
     */
    @Builder.Default
    private double fairRentValue = 0;
    
    /**
     * Standard Rent (for controlled tenancies)
     */
    @Builder.Default
    private double standardRent = 0;
    
    /**
     * Maximum of (Fair Rent, Standard Rent, Municipal RV)
     * Used for GAV calculation
     */
    @Builder.Default
    private double maxRent = 0;
    
    // ===== Vacancy & Unrealized Rent =====
    
    /**
     * Period of vacancy (in months)
     * Applied for let-out property
     */
    @Builder.Default
    private double vacancyPeriodMonths = 0;
    
    /**
     * Rent not realized due to vacancy
     */
    @Builder.Default
    private double unrealizedRent = 0;
    
    /**
     * Arrears of rent received (taxable in year received)
     */
    @Builder.Default
    private double arrearsOfRent = 0;
    
    // ===== Deductions =====
    
    /**
     * Municipal Taxes / Property Tax paid
     * Allowed as deduction from GAV
     */
    @Builder.Default
    private double municipalTaxesPaid = 0;
    
    /**
     * Interest on Housing Loan
     * Section 24: Self-Occupied max ₹2,00,000
     * Let-out: Full interest allowed
     */
    @Builder.Default
    private double interestOnLoan = 0;
    
    // ===== Computed Fields (Calculated by Backend) =====
    
    /**
     * Gross Annual Value (GAV)
     * = Annual Rent - Unrealized Rent - Municipal Taxes
     * OR = Max Rent - Municipal Taxes (if let-out at higher rent)
     */
    @Builder.Default
    private double grossAnnualValue = 0;
    
    /**
     * Net Annual Value (NAV)
     * = GAV - Municipal Taxes (vacancy adjustment if applicable)
     */
    @Builder.Default
    private double netAnnualValue = 0;
    
    /**
     * Standard Deduction @30% of NAV
     * Allowed under Section 24(a)
     */
    @Builder.Default
    private double standardDeduction = 0;
    
    /**
     * Final Income from House Property
     * Positive = Taxable Income
     * Negative = Loss (can be set-off against other income)
     */
    @Builder.Default
    private double incomeFromHP = 0;
    
    // ===== Section 24B - Home Loan Details (NEW for AY 2026-27) =====
    
    /**
     * List of home loans for Section 24B deduction
     * MANDATORY if interestOnLoan > 0
     */
    @Builder.Default
    private List<HomeLoan> homeLoans = new ArrayList();
    
    /**
     * Total interest from all loans
     * (May differ from interestOnLoan if some loans not eligible)
     */
    @Builder.Default
    private double totalInterest24B = 0;
    
    // ===== Tenant Details (AY 2026-27 EXPANSION) =====
    
    /**
     * List of tenants
     * Required if rent >= ₹50,000 per tenant (Section 194-IB)
     * OR if rent >= ₹2.4L total
     */
    @Builder.Default
    private List<Tenant> tenants = new ArrayList<>();
    
    // ===== Pre-Construction Interest (Section 24) =====
    
    /**
     * Pre-construction interest
     * Eligible for spread over 5 years
     */
    @Builder.Default
    private double preConstructionInterest = 0;
    
    /**
     * Amount of pre-construction interest claimed this year
     */
    @Builder.Default
    private double preConstructionInterestClaimed = 0;
    
    // ===== Loss Carry Forward =====
    
    /**
     * Brought forward loss from previous year
     */
    @Builder.Default
    private double broughtForwardLoss = 0;
    
    /**
     * Loss set-off applied this year
     */
    @Builder.Default
    private double lossSetOff = 0;
    
    /**
     * Remaining loss to carry forward
     */
    @Builder.Default
    private double lossCarryForward = 0;
    
    // ========================================
    // NESTED CLASSES
    // ========================================
    
    /**
     * Co-Owner Detail
     * MANDATORY if property is co-owned
     * AY 2026-27: Mandatory details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoOwner {
        
        /**
         * Co-Owner Sequence Number
         */
        @Builder.Default
        private int coOwnerSNo = 1;
        
        /**
         * Name of Co-Owner (NameCoOwner)
         */
        private String name;
        
        /**
         * PAN of Co-Owner (PANCoOwner)
         */
        private String pan;
        
        /**
         * Aadhaar of Co-Owner (AadhaarCoOwner)
         * NEW for AY 2026-27
         */
        private String aadhaar;
        
        /**
         * Percentage Share in Property (PercentShareProperty)
         */
        @Builder.Default
        private double sharePercentage = 0;
    }
    
    /**
     * Home Loan Detail
     * Section 24B - Required for interest deduction
     * NEW for AY 2026-27 - Detailed disclosure required
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeLoan {
        
        /**
         * Loan from: B (Bank), I (Institution), L (Lender/Other)
         */
        @Builder.Default
        private String lenderType = "B";
        
        /**
         * Name of Bank/Institution (BankOrInstnName)
         */
        private String lenderName;
        
        /**
         * PAN of Bank/Institution (if institutional lender)
         */
        private String lenderPAN;
        
        /**
         * Loan Account Number (LoanAccNoOfBankOrInstnRefNo)
         * NEW for AY 2026-27
         */
        private String loanAccountNo;
        
        /**
         * Date of Loan Sanction (DateofLoan)
         * NEW for AY 2026-27
         */
        private LocalDate dateOfLoan;
        
        /**
         * Total Loan Amount (TotalLoanAmt)
         * NEW for AY 2026-27
         */
        @Builder.Default
        private double totalLoanAmount = 0;
        
        /**
         * Outstanding Loan at year end (LoanOutstndngAmt)
         * NEW for AY 2026-27
         */
        @Builder.Default
        private double loanOutstandingAmount = 0;
        
        /**
         * Interest claimed under Section 24B (InterestUs24B)
         */
        @Builder.Default
        private double interestUs24B = 0;
        
        /**
         * Property Construction Completion Date
         * For pre-construction interest calculation
         */
        private LocalDate constructionCompletionDate;
        
        /**
         * Whether property completed within 5 years of loan
         * Affects pre-construction interest eligibility
         */
        @Builder.Default
        private boolean completedWithin5Years = true;
        
        /**
         * Pre-construction interest eligible for spread
         */
        @Builder.Default
        private double preConstructionInterest = 0;
        
        /**
         * Pre-construction interest claimed this year
         */
        @Builder.Default
        private double preConstructionInterestThisYear = 0;
    }
    
    /**
     * Tenant Detail
     * Required for deduction of TDS by tenant
     * NEW for AY 2026-27
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tenant {
        
        /**
         * Tenant Sequence Number
         */
        @Builder.Default
        private int tenantSNo = 1;
        
        /**
         * Name of Tenant (NameOfTenant)
         */
        private String name;
        
        /**
         * PAN of Tenant (PANOfTenant)
         * MANDATORY if annual rent >= ₹50,00,000 (₹50L)
         */
        private String pan;
        
        /**
         * Aadhaar of Tenant (AadhaarOfTenant)
         * NEW for AY 2026-27
         */
        private String aadhaar;
        
        /**
         * Amount of TDS deducted by this tenant
         * Section 194-IB
         */
        @Builder.Default
        private double tdsDeducted = 0;
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    /**
     * Check if this is a let-out property
     */
    public boolean isLetOut() {
        return "LET_OUT".equals(propertyType) || "L".equals(propertyType);
    }
    
    /**
     * Check if this is a self-occupied property
     */
    public boolean isSelfOccupied() {
        return "SELF_OCCUPIED".equals(propertyType) || "S".equals(propertyType);
    }
    
    /**
     * Check if this is a deemed let-out property
     */
    public boolean isDeemedLetOut() {
        return "DEEMED_LET_OUT".equals(propertyType) || "D".equals(propertyType);
    }
    
    /**
     * Get maximum allowed interest under Section 24
     * Self-occupied: ₹2,00,000
     * Let-out: Full interest
     */
    public double getMaxAllowedInterest() {
        if (isSelfOccupied() || isDeemedLetOut()) {
            return Math.min(interestOnLoan, 200000);
        }
        return interestOnLoan; // Let-out - full interest allowed
    }
    
    /**
     * Calculate income from house property
     * Per CBDT rules:
     * 1. Determine Gross Annual Value
     * 2. Subtract Municipal Taxes = Net Annual Value
     * 3. Deduct 30% of NAV (Standard Deduction)
     * 4. Deduct Interest on Loan (Section 24)
     */
    public void calculateIncome() {
        if (isSelfOccupied()) {
            // Self-Occupied: NAV - Interest
            // Standard deduction is 30% of NIL = 0
            netAnnualValue = 0;
            standardDeduction = 0;
            incomeFromHP = -getMaxAllowedInterest();
        } else {
            // Let-out or Deemed Let-out
            // GAV = Annual Rent - Unrealized Rent - Municipal Taxes
            grossAnnualValue = annualRent - unrealizedRent - municipalTaxesPaid;
            
            // NAV = GAV (already reduced by municipal taxes)
            netAnnualValue = grossAnnualValue;
            
            // Standard Deduction @30% of NAV
            standardDeduction = netAnnualValue * 0.30;
            
            // Income = NAV - Standard Deduction - Interest
            incomeFromHP = netAnnualValue - standardDeduction - interestOnLoan;
        }
    }
}
