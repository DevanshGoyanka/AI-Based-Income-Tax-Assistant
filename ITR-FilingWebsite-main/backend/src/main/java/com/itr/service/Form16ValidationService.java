package com.itr.service;

import com.itr.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Form 16 Validation Service
 * Cross-validates Part A and Part B, auto-populates ITR-1/2/3/4
 */
@Slf4j
@Service
public class Form16ValidationService {

    public ValidationResult validateAndAutoPopulate(
            Form16PartAData partA, 
            Form16PartBData partB,
            String taxpayerPan, 
            String assessmentYear,
            Object itrFormData) {

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // VAL-F16A-001: TAN format
        if (partA.getTanOfEmployer() == null || 
            !partA.getTanOfEmployer().matches("[A-Z]{4}[0-9]{5}[A-Z]")) {
            errors.add("VAL-F16A-001: Invalid TAN format");
        }

        // VAL-F16A-002: PAN match
        if (!taxpayerPan.equals(partA.getPanOfEmployee())) {
            errors.add("VAL-F16A-002: PAN mismatch - Form 16: " + partA.getPanOfEmployee() + 
                      ", ITR: " + taxpayerPan);
        }

        // VAL-F16A-003: Assessment year match
        if (!assessmentYear.equals(partA.getAssessmentYear())) {
            errors.add("VAL-F16A-003: Assessment year mismatch");
        }

        // VAL-F16A-004: Part A vs Part B salary match (tolerance Rs 100)
        long salaryDiff = Math.abs(partA.getAggregateSalaryPaid() - partB.getGrossSalary());
        if (salaryDiff > 100) {
            warnings.add("VAL-F16A-004: Salary mismatch between Part A and Part B: Rs " + salaryDiff);
        }

        // VAL-F16A-005: Part A vs Part B TDS match (tolerance Rs 100)
        long tdsDiff = Math.abs(partA.getAggregateTdsDeposited() - partB.getTdsDeductedTotal());
        if (tdsDiff > 100) {
            warnings.add("VAL-F16A-005: TDS mismatch between Part A and Part B: Rs " + tdsDiff);
        }

        // VAL-F16A-006: Quarterly TDS sum equals aggregate
        long quarterlySum = partA.getQuarterlyBreakup().stream()
            .mapToLong(Form16PartAData.QuarterlyTDS::getAmountOfTaxDeposited)
            .sum();
        if (Math.abs(quarterlySum - partA.getAggregateTdsDeposited()) > 100) {
            warnings.add("VAL-F16A-006: Quarterly TDS sum mismatch: " + quarterlySum + 
                        " vs aggregate: " + partA.getAggregateTdsDeposited());
        }

        // VAL-F16A-007: Standard deduction validation
        long stdDeduction = partB.getStandardDeduction();
        if ("2026-27".equals(assessmentYear) && stdDeduction != 75000) {
            warnings.add("VAL-F16A-007: Standard deduction should be Rs 75,000 for AY 2026-27");
        } else if ("2025-26".equals(assessmentYear) && stdDeduction != 50000) {
            warnings.add("VAL-F16A-007: Standard deduction should be Rs 50,000 for AY 2025-26");
        }

        // VAL-F16A-008: Professional tax limit
        if (partB.getProfessionalTax() > 2500) {
            warnings.add("VAL-F16A-008: Professional tax exceeds Rs 2,500 limit");
        }

        // Auto-populate applicable ITR form if no errors
        if (errors.isEmpty() && itrFormData != null) {
            if (itrFormData instanceof Itr1FormData) {
                autoPopulateITR1(partA, partB, (Itr1FormData) itrFormData);
            } else if (itrFormData instanceof Itr2FormData) {
                autoPopulateITR2(partA, partB, (Itr2FormData) itrFormData);
            } else if (itrFormData instanceof Itr3FormData) {
                autoPopulateITR3(partA, partB, (Itr3FormData) itrFormData);
            } else if (itrFormData instanceof Itr4FormData) {
                autoPopulateITR4(partA, partB, (Itr4FormData) itrFormData);
            }
        }

        ValidationResult result = new ValidationResult();
        result.setErrors(errors);
        result.setWarnings(warnings);
        result.setValid(errors.isEmpty());

        log.info("Form 16 validation: {} errors, {} warnings", errors.size(), warnings.size());
        return result;
    }

    private void autoPopulateITR1(Form16PartAData partA, Form16PartBData partB, Itr1FormData itr1) {
        // Populate salary income
        Itr1FormData.SalaryIncome salary = itr1.getSalaryIncome();
        if (salary == null) {
            salary = new Itr1FormData.SalaryIncome();
            itr1.setSalaryIncome(salary);
        }

        salary.setSalary17_1(partB.getTotalSalary17_1());
        salary.setPerquisites17_2(partB.getTotalPerquisites17_2());
        salary.setProfitsInLieu17_3(partB.getTotalProfitsInLieu17_3());
        salary.setGrossSalary(partB.getGrossSalary());
        salary.setHraExempt(partB.getHraExemption10_13A());
        salary.setLtaExempt(partB.getLtaExemption10_5());
        salary.setTotalExemptAllowances(partB.getTotalExemptionsUnder10());
        salary.setNetSalary(partB.getNetSalaryAfterExemptions());
        salary.setStandardDeduction(partB.getStandardDeduction());
        salary.setProfessionalTax(partB.getProfessionalTax());
        salary.setIncomeFromSalary(partB.getTaxableIncomeSalary());

        // Populate deductions
        Itr1FormData.Deductions deductions = itr1.getDeductions();
        if (deductions == null) {
            deductions = new Itr1FormData.Deductions();
            itr1.setDeductions(deductions);
        }

        deductions.setDeduction80C(partB.getDeduction80C());
        deductions.setNpsEmployee80CCD1(partB.getDeduction80CCD_1());
        deductions.setNpsEmployee80CCD1B(partB.getDeduction80CCD_1B());
        deductions.setNpsEmployer80CCD2(partB.getDeduction80CCD_2());
        deductions.setDeduction80D(partB.getDeduction80D());
        deductions.setDeduction80E(partB.getDeduction80E());
        deductions.setDeduction80G(partB.getDeduction80G());

        // Populate TDS
        Itr1FormData.TaxPayments taxPayments = itr1.getTaxPayments();
        if (taxPayments == null) {
            taxPayments = new Itr1FormData.TaxPayments();
            itr1.setTaxPayments(taxPayments);
        }

        taxPayments.setTotalTDSOnSalary(partB.getTdsDeductedTotal());

        log.info("Auto-populated ITR-1 from Form 16: Salary={}, TDS={}", 
                 partB.getGrossSalary(), partB.getTdsDeductedTotal());
    }

    private void autoPopulateITR2(Form16PartAData partA, Form16PartBData partB, Itr2FormData itr2) {
        // ITR-2: Salary + Capital Gains + Multiple properties
        CommonFormData.ScheduleSalary salary = itr2.getScheduleSalary();
        if (salary == null) {
            salary = new CommonFormData.ScheduleSalary();
            itr2.setScheduleSalary(salary);
        }

        salary.setGrossSalary(partB.getGrossSalary());
        salary.setAllowancesExempt(partB.getTotalExemptionsUnder10());
        salary.setNetSalary(partB.getNetSalaryAfterExemptions());
        salary.setStandardDeduction(partB.getStandardDeduction());
        salary.setProfessionalTax(partB.getProfessionalTax());
        salary.setIncomeFromSalary(partB.getTaxableIncomeSalary());

        // Populate deductions
        CommonFormData.DeductionsVIA deductions = itr2.getDeductions();
        if (deductions == null) {
            deductions = new CommonFormData.DeductionsVIA();
            itr2.setDeductions(deductions);
        }

        deductions.setDeduction80C(partB.getDeduction80C());
        deductions.setDeduction80CCD1(partB.getDeduction80CCD_1());
        deductions.setDeduction80CCD1B(partB.getDeduction80CCD_1B());
        deductions.setDeduction80CCD2(partB.getDeduction80CCD_2());
        deductions.setDeduction80D(partB.getDeduction80D());
        deductions.setDeduction80E(partB.getDeduction80E());
        deductions.setDeduction80G(partB.getDeduction80G());

        // Populate TDS
        CommonFormData.ScheduleTDS tds = itr2.getScheduleTDS();
        if (tds == null) {
            tds = new CommonFormData.ScheduleTDS();
            itr2.setScheduleTDS(tds);
        }

        tds.setTotalTDSSalary(partB.getTdsDeductedTotal());

        log.info("Auto-populated ITR-2 from Form 16: Salary={}, TDS={}", 
                 partB.getGrossSalary(), partB.getTdsDeductedTotal());
    }

    private void autoPopulateITR3(Form16PartAData partA, Form16PartBData partB, Itr3FormData itr3) {
        // ITR-3: Salary + Business/Profession
        CommonFormData.ScheduleSalary salary = itr3.getScheduleSalary();
        if (salary == null) {
            salary = new CommonFormData.ScheduleSalary();
            itr3.setScheduleSalary(salary);
        }

        salary.setGrossSalary(partB.getGrossSalary());
        salary.setAllowancesExempt(partB.getTotalExemptionsUnder10());
        salary.setNetSalary(partB.getNetSalaryAfterExemptions());
        salary.setStandardDeduction(partB.getStandardDeduction());
        salary.setProfessionalTax(partB.getProfessionalTax());
        salary.setIncomeFromSalary(partB.getTaxableIncomeSalary());

        // Populate deductions
        CommonFormData.DeductionsVIA deductions = itr3.getDeductions();
        if (deductions == null) {
            deductions = new CommonFormData.DeductionsVIA();
            itr3.setDeductions(deductions);
        }

        deductions.setDeduction80C(partB.getDeduction80C());
        deductions.setDeduction80CCD1(partB.getDeduction80CCD_1());
        deductions.setDeduction80CCD1B(partB.getDeduction80CCD_1B());
        deductions.setDeduction80CCD2(partB.getDeduction80CCD_2());
        deductions.setDeduction80D(partB.getDeduction80D());
        deductions.setDeduction80E(partB.getDeduction80E());
        deductions.setDeduction80G(partB.getDeduction80G());

        // Populate TDS
        CommonFormData.ScheduleTDS tds = itr3.getScheduleTDS();
        if (tds == null) {
            tds = new CommonFormData.ScheduleTDS();
            itr3.setScheduleTDS(tds);
        }

        tds.setTotalTDSSalary(partB.getTdsDeductedTotal());

        log.info("Auto-populated ITR-3 from Form 16: Salary={}, TDS={}", 
                 partB.getGrossSalary(), partB.getTdsDeductedTotal());
    }

    private void autoPopulateITR4(Form16PartAData partA, Form16PartBData partB, Itr4FormData itr4) {
        // ITR-4: Presumptive taxation + Salary
        CommonFormData.ScheduleSalary salary = itr4.getScheduleSalary();
        if (salary == null) {
            salary = new CommonFormData.ScheduleSalary();
            itr4.setScheduleSalary(salary);
        }

        salary.setGrossSalary(partB.getGrossSalary());
        salary.setAllowancesExempt(partB.getTotalExemptionsUnder10());
        salary.setNetSalary(partB.getNetSalaryAfterExemptions());
        salary.setStandardDeduction(partB.getStandardDeduction());
        salary.setProfessionalTax(partB.getProfessionalTax());
        salary.setIncomeFromSalary(partB.getTaxableIncomeSalary());

        // Populate deductions
        CommonFormData.DeductionsVIA deductions = itr4.getDeductions();
        if (deductions == null) {
            deductions = new CommonFormData.DeductionsVIA();
            itr4.setDeductions(deductions);
        }

        deductions.setDeduction80C(partB.getDeduction80C());
        deductions.setDeduction80CCD1(partB.getDeduction80CCD_1());
        deductions.setDeduction80CCD1B(partB.getDeduction80CCD_1B());
        deductions.setDeduction80CCD2(partB.getDeduction80CCD_2());
        deductions.setDeduction80D(partB.getDeduction80D());
        deductions.setDeduction80E(partB.getDeduction80E());
        deductions.setDeduction80G(partB.getDeduction80G());

        // Populate TDS
        CommonFormData.ScheduleTDS tds = itr4.getScheduleTDS();
        if (tds == null) {
            tds = new CommonFormData.ScheduleTDS();
            itr4.setScheduleTDS(tds);
        }

        tds.setTotalTDSSalary(partB.getTdsDeductedTotal());

        log.info("Auto-populated ITR-4 from Form 16: Salary={}, TDS={}", 
                 partB.getGrossSalary(), partB.getTdsDeductedTotal());
    }

    public static class ValidationResult {
        private List<String> errors;
        private List<String> warnings;
        private boolean valid;

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
    }
}
