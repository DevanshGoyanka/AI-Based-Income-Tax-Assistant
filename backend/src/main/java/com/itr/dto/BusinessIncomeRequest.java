package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Business Income Calculation
 * All calculations done in backend - frontend just provides input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessIncomeRequest {
    
    // Scheme type
    private String scheme; // 44AD, 44ADA, 44AE, Regular
    
    // Basic inputs
    private Double grossTurnover;
    private Double declaredIncome;
    private Double netProfitPL;
    
    // Presumptive scheme additional info
    private Boolean isPresumptive44ADExceed50L;
    private Boolean isPassengerVehicle;
    
    // Regular scheme inputs - Addbacks (disallowances)
    // Section 40A disallowances
    private Double disallowance40AIa;     // Related party payments
    private Double disallowance40A2;      // Excessive payments to specified persons
    private Double disallowance40A3;      // Cash payments > ₹10,000
    private Double disallowance43B;       // Unpaid statutory dues
    private Double disallowance43Bh;      // Unpaid MSE dues
    private Double disallowance14A;       // Expenses for exempt income
    
    // Personal/capital expenses
    private Double personalExpenses;
    private Double capitalExpenses;
    
    // Deductions
    private Double depreciation;
    private Double additionalDepreciation;
    private Double deduction35AD;
    private Double otherDeductions;
    
    // Loss set-off
    private Double broughtForwardLoss;
    
    // Audit info
    private Boolean isAudited;
    
    // Nature of business
    private String businessNature;
    private String nicCode;
}
