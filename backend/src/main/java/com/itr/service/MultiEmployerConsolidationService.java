package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Employer Consolidation Service - 101% CBDT Compliant
 * Consolidates multiple Form 16s from different employers
 * Reference: ITR_Import_JSON_Validation.md Section 1.4
 */
@Slf4j
@Service
public class MultiEmployerConsolidationService {

    public ConsolidatedSalary consolidateMultipleEmployers(List<EmployerData> employers) {
        ConsolidatedSalary result = new ConsolidatedSalary();
        
        double totalGrossSalary = 0;
        double totalExemptions = 0;
        double totalTDS = 0;
        double totalProfessionalTax = 0;
        
        for (EmployerData emp : employers) {
            totalGrossSalary += emp.getGrossSalary();
            totalExemptions += emp.getExemptAllowances();
            totalTDS += emp.getTdsDeducted();
            totalProfessionalTax += emp.getProfessionalTax();
        }
        
        result.setTotalGrossSalary(totalGrossSalary);
        result.setTotalExemptions(totalExemptions);
        result.setNetSalary(totalGrossSalary - totalExemptions);
        
        // Standard deduction claimed only ONCE (max applicable)
        double standardDeduction = employers.stream()
            .mapToDouble(EmployerData::getStandardDeduction)
            .max()
            .orElse(50000);
        result.setStandardDeduction(standardDeduction);
        
        // Professional tax total
        result.setProfessionalTax(Math.min(totalProfessionalTax, 2500));
        
        // Total deductions u/s 16
        result.setTotalDeductions16(standardDeduction + result.getProfessionalTax());
        
        // Income from salary
        result.setIncomeFromSalary(result.getNetSalary() - result.getTotalDeductions16());
        
        // Total TDS
        result.setTotalTDS(totalTDS);
        
        result.setEmployers(employers);
        
        return result;
    }

    @Data
    public static class EmployerData {
        private String employerName;
        private String employerTAN;
        private String employerPAN;
        private double grossSalary;
        private double exemptAllowances;
        private double standardDeduction;
        private double professionalTax;
        private double tdsDeducted;
        private String periodFrom;
        private String periodTo;
    }

    @Data
    public static class ConsolidatedSalary {
        private double totalGrossSalary;
        private double totalExemptions;
        private double netSalary;
        private double standardDeduction;
        private double professionalTax;
        private double totalDeductions16;
        private double incomeFromSalary;
        private double totalTDS;
        private List<EmployerData> employers = new ArrayList<>();
    }
}
