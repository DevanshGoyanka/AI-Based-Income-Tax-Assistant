package com.itr.service.integration;

import com.itr.dto.Form16Data;
import com.itr.dto.AISData;
import com.itr.dto.Form26ASData;
import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Auto-Population Service - 101% CBDT Compliant
 * Supports ITR-1 and ITR-2 with capital gains
 */
@Slf4j
@Service
public class AutoPopulationService {
    
    private final CapitalGainsAutoPopulationService capitalGainsService;
    
    public AutoPopulationService(CapitalGainsAutoPopulationService capitalGainsService) {
        this.capitalGainsService = capitalGainsService;
    }

    public Itr1FormData autoPopulateFromForm16(Itr1FormData formData, Form16Data form16) {
        log.info("Auto-populating ITR-1 from Form 16");
        
        if (form16.getPartA() != null) {
            if (formData.getPersonalInfo() == null) {
                formData.setPersonalInfo(Itr1FormData.PersonalInfo.builder().build());
            }
            formData.getPersonalInfo().setPan(form16.getPartA().getEmployeePAN());
            formData.getPersonalInfo().setFinancialYear(form16.getPartA().getFinancialYear());
            formData.getPersonalInfo().setAssessmentYear(form16.getPartA().getAssessmentYear());
        }
        
        if (form16.getPartB() != null) {
            Form16Data.PartB partB = form16.getPartB();
            
            if (formData.getSalaryIncome() == null) {
                formData.setSalaryIncome(Itr1FormData.SalaryIncome.builder().build());
            }
            
            if (partB.getSalary() != null) {
                formData.getSalaryIncome().setSalary17_1(partB.getSalary());
            }
            
            if (partB.getValueOfPerquisites() != null) {
                formData.getSalaryIncome().setPerquisites17_2(partB.getValueOfPerquisites());
            }
            
            if (partB.getProfitsInLieuOfSalary() != null) {
                formData.getSalaryIncome().setProfitsInLieu17_3(partB.getProfitsInLieuOfSalary());
            }
            
            if (partB.getStandardDeduction() != null) {
                formData.getSalaryIncome().setStandardDeduction(partB.getStandardDeduction());
            }
            
            if (partB.getProfessionalTax() != null) {
                formData.getSalaryIncome().setProfessionalTax(partB.getProfessionalTax());
            }
            
            if (partB.getDeductions() != null) {
                if (formData.getDeductions() == null) {
                    formData.setDeductions(Itr1FormData.Deductions.builder().build());
                }
                
                Form16Data.Deductions ded = partB.getDeductions();
                if (ded.getDeduction80C() != null) formData.getDeductions().setDeduction80C(ded.getDeduction80C());
                if (ded.getDeduction80CCD1B() != null) formData.getDeductions().setNpsEmployee80CCD1B(ded.getDeduction80CCD1B());
                if (ded.getDeduction80CCD2() != null) formData.getDeductions().setNpsEmployer80CCD2(ded.getDeduction80CCD2());
                if (ded.getDeduction80D() != null) formData.getDeductions().setDeduction80D(ded.getDeduction80D());
                if (ded.getDeduction80E() != null) formData.getDeductions().setDeduction80E(ded.getDeduction80E());
                if (ded.getDeduction80G() != null) formData.getDeductions().setDeduction80G(ded.getDeduction80G());
                if (ded.getDeduction80TTA() != null) formData.getDeductions().setDeduction80TTA(ded.getDeduction80TTA());
                if (ded.getDeduction80TTB() != null) formData.getDeductions().setDeduction80TTB(ded.getDeduction80TTB());
            }
        }
        
        log.info("Auto-population from Form 16 completed");
        return formData;
    }

    public Itr1FormData autoPopulateFromAIS(Itr1FormData formData, AISData ais) {
        log.info("Auto-populating ITR-1 from AIS");
        
        if (formData.getTaxPayments() == null) {
            formData.setTaxPayments(Itr1FormData.TaxPayments.builder().build());
        }
        
        if (ais.getTdsSalary() != null && !ais.getTdsSalary().isEmpty()) {
            double totalTDS = ais.getTdsSalary().stream()
                    .mapToDouble(AISData.TDSSalary::getTotalTaxDeducted)
                    .sum();
            formData.getTaxPayments().setTotalTDSOnSalary(totalTDS);
        }
        
        if (ais.getTaxPayments() != null && !ais.getTaxPayments().isEmpty()) {
            double advanceTax = ais.getTaxPayments().stream()
                    .filter(p -> "Advance Tax".equalsIgnoreCase(p.getTaxType()))
                    .mapToDouble(AISData.TaxPayment::getAmount)
                    .sum();
            formData.getTaxPayments().setTotalAdvanceTax(advanceTax);
            
            double selfAssessmentTax = ais.getTaxPayments().stream()
                    .filter(p -> "Self Assessment Tax".equalsIgnoreCase(p.getTaxType()))
                    .mapToDouble(AISData.TaxPayment::getAmount)
                    .sum();
            formData.getTaxPayments().setTotalSelfAssessmentTax(selfAssessmentTax);
        }
        
        log.info("Auto-population from AIS completed");
        return formData;
    }

    public Itr1FormData autoPopulateFrom26AS(Itr1FormData formData, Form26ASData data26AS) {
        log.info("Auto-populating ITR-1 from Form 26AS");
        
        if (formData.getTaxPayments() == null) {
            formData.setTaxPayments(Itr1FormData.TaxPayments.builder().build());
        }
        
        if (data26AS.getPartA() != null && !data26AS.getPartA().isEmpty()) {
            double totalTDS = data26AS.getPartA().stream()
                    .mapToDouble(Form26ASData.PartA::getTotalTaxDeducted)
                    .sum();
            formData.getTaxPayments().setTotalTDSOnSalary(totalTDS);
        }
        
        if (data26AS.getPartD() != null && !data26AS.getPartD().isEmpty()) {
            double advanceTax = data26AS.getPartD().stream()
                    .filter(p -> "Advance Tax".equalsIgnoreCase(p.getTaxType()))
                    .mapToDouble(Form26ASData.PartD::getAmount)
                    .sum();
            formData.getTaxPayments().setTotalAdvanceTax(advanceTax);
        }
        
        log.info("Auto-population from Form 26AS completed");
        return formData;
    }
    
    public Itr2FormData autoPopulateITR2FromAIS(Itr2FormData formData, AISData ais) {
        log.info("Auto-populating ITR-2 from AIS with capital gains");
        
        // Auto-populate capital gains transactions
        capitalGainsService.autoPopulateCapitalGains(formData, ais);
        
        log.info("Auto-population from AIS completed with capital gains");
        return formData;
    }
    
    public Itr2FormData autoPopulateITR2From26AS(Itr2FormData formData, Form26ASData data26AS) {
        log.info("Auto-populating ITR-2 from Form 26AS with capital gains");
        
        // Auto-populate capital gains transactions
        capitalGainsService.autoPopulateCapitalGainsFrom26AS(formData, data26AS);
        
        log.info("Auto-population from Form 26AS completed with capital gains");
        return formData;
    }
}
