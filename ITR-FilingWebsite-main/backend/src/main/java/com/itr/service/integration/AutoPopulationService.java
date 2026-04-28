package com.itr.service.integration;

import com.itr.dto.AISData;
import com.itr.dto.Form26ASData;
import com.itr.dto.TISData;
import com.itr.dto.FlatFormData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-population service with comprehensive mapping
 * Priority: TIS (accepted) > AIS (processed) > 26AS (for TDS credit)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoPopulationService {

    private final ObjectMapper objectMapper;

    public FlatFormData autoPopulateAll(AISData ais, Form26ASData f26as, TISData tis, String itrType) {
        FlatFormData flat = FlatFormData.builder()
                .tdsEntries(new ArrayList<>())
                .advanceTaxEntries(new ArrayList<>())
                .selfAssessmentTaxEntries(new ArrayList<>())
                .employerEntries(new ArrayList<>())
                .build();

        // === PERSONAL INFO ===
        if (ais != null && ais.getGeneralInfo() != null) {
            flat.setMobile(ais.getGeneralInfo().getMobile());
            flat.setEmail(ais.getGeneralInfo().getEmail());
        }

        // === MULTI-EMPLOYER EXTRACTION from AIS TDS Salary (Section 192) ===
        if (ais != null && ais.getTdsSalary() != null && !ais.getTdsSalary().isEmpty()) {
            for (AISData.TDSSalary salEntry : ais.getTdsSalary()) {
                FlatFormData.EmployerEntry empEntry = FlatFormData.EmployerEntry.builder()
                        .employerName(salEntry.getDeductorName())
                        .employerTAN(salEntry.getDeductorTAN())
                        .basic(salEntry.getTotalAmountPaid() != null ? salEntry.getTotalAmountPaid() : 0.0)
                        .tdsDeducted(salEntry.getTotalTaxDeducted() != null ? salEntry.getTotalTaxDeducted() : 0.0)
                        .grossSalary(salEntry.getTotalAmountPaid() != null ? salEntry.getTotalAmountPaid() : 0.0)
                        .netSalary(salEntry.getTotalAmountPaid() != null ? salEntry.getTotalAmountPaid() : 0.0)
                        .build();
                flat.getEmployerEntries().add(empEntry);
                log.info("Employer entry added from AIS: {} (TAN: {}), Salary: {}, TDS: {}", 
                    empEntry.getEmployerName(), empEntry.getEmployerTAN(), empEntry.getGrossSalary(), empEntry.getTdsDeducted());
            }
        }
        
        // Fallback: Calculate total salary for legacy field
        if (ais != null && ais.getTdsSalary() != null && !ais.getTdsSalary().isEmpty()) {
            double totalSalary = ais.getTdsSalary().stream()
                    .mapToDouble(s -> s.getTotalAmountPaid() != null ? s.getTotalAmountPaid() : 0.0)
                    .sum();
            flat.setBasic(totalSalary);
            log.info("Total salary income: {}", totalSalary);
        }

        // === OTHER SOURCES — INTEREST INCOME ===
        long interestFromDeposit = 0;
        long interestFromSavings = 0;
        
        if (tis != null && tis.getInterestFromDeposit() > 0) {
            interestFromDeposit = tis.getInterestFromDeposit();
        } else if (ais != null && ais.getPartB1() != null) {
            for (AISData.AISTDSEntry entry : ais.getPartB1().getTdsEntries()) {
                if ("194A".equals(entry.getSection())) {
                    interestFromDeposit += entry.getTotalAmountPaid();
                } else if ("194I".equals(entry.getSection())) {
                    // Interest from savings account (no TDS if < 40k)
                    interestFromSavings += entry.getTotalAmountPaid();
                }
            }
        }
        
        // Also check TDSOther for interest income
        if (ais != null && ais.getTdsOther() != null) {
            for (AISData.TDSOther entry : ais.getTdsOther()) {
                if ("194A".equals(entry.getSection())) {
                    interestFromDeposit += (entry.getAmountPaid() != null ? entry.getAmountPaid().longValue() : 0);
                }
            }
        }
        
        flat.setInterestFD(interestFromDeposit);
        flat.setInterestSB(interestFromSavings);

        // === OTHER SOURCES — DIVIDEND ===
        long dividend = 0;
        if (tis != null) dividend = tis.getDividendIncome();
        else if (ais != null && ais.getPartB2() != null) dividend = ais.getPartB2().getDividendIncome();
        flat.setDividends(dividend);

        // === CAPITAL GAINS — from AIS SFT-17 sale details ===
        if (ais != null && ais.getPartB2() != null && !ais.getPartB2().getSecuritiesSale().isEmpty()) {
            AISData.SFTSaleEntry sale = ais.getPartB2().getSecuritiesSale().get(0);
            long gain = sale.getSalesConsideration().longValue() - sale.getCostOfAcquisition().longValue();

            if ("LTCG".equals(sale.getAssetType())) {
                if (sale.getTransferDate().isBefore(LocalDate.of(2024, 7, 23))) {
                    flat.setLtcg112APre(Math.max(0, gain));
                } else {
                    flat.setLtcg112APost(Math.max(0, gain));
                }
            } else {
                if (sale.getTransferDate().isBefore(LocalDate.of(2024, 7, 23))) {
                    flat.setStcgEquityPre(Math.max(0, gain));
                } else {
                    flat.setStcgEquityPost(Math.max(0, gain));
                }
            }
        }

        // === TDS FROM 26AS (for Schedule TDS2 — authoritative) ===
        // CBDT Compliant: Create separate TDS entries for each deductor
        List<FlatFormData.TDSEntry> tdsEntries = new ArrayList<>();
        double totalTds192 = 0;
        double totalTds194A = 0;
        double totalTdsOther = 0;
        
        if (f26as != null && f26as.getPartIEntries() != null) {
            for (Form26ASData.TDSEntry26AS entry : f26as.getPartIEntries()) {
                FlatFormData.TDSEntry tdsEntry = FlatFormData.TDSEntry.builder()
                        .section(entry.getSection())
                        .deductorName(entry.getDeductorName())
                        .deductorTAN(entry.getTan())
                        .incomeAmount(entry.getAmountPaid() != null ? entry.getAmountPaid().doubleValue() : 0)
                        .tdsDeducted(entry.getTaxDeducted() != null ? entry.getTaxDeducted().doubleValue() : 0)
                        .financialYear("2024-25")
                        .verified26AS(true)
                        .claimedInReturn(true)
                        .build();
                tdsEntries.add(tdsEntry);
                
                if ("192".equals(entry.getSection())) {
                    totalTds192 += entry.getTaxDeposited().doubleValue();
                } else if ("194A".equals(entry.getSection())) {
                    totalTds194A += entry.getTaxDeposited().doubleValue();
                } else {
                    totalTdsOther += entry.getTaxDeposited().doubleValue();
                }
            }
        } else if (ais != null && ais.getPartB1() != null && !ais.getPartB1().getTdsEntries().isEmpty()) {
            // Fallback to AIS Part B1 TDS if 26AS not available
            for (AISData.AISTDSEntry entry : ais.getPartB1().getTdsEntries()) {
                FlatFormData.TDSEntry tdsEntry = FlatFormData.TDSEntry.builder()
                        .section(entry.getSection())
                        .deductorName(entry.getDeductorName())
                        .deductorTAN(entry.getDeductorTAN())
                        .incomeAmount(entry.getTotalAmountPaid())
                        .tdsDeducted(entry.getTotalTDSDeducted())
                        .financialYear("2024-25")
                        .verified26AS(false)
                        .claimedInReturn(true)
                        .build();
                tdsEntries.add(tdsEntry);
                
                if ("192".equals(entry.getSection())) {
                    totalTds192 += entry.getTotalTDSDeducted();
                } else if ("194A".equals(entry.getSection())) {
                    totalTds194A += entry.getTotalTDSDeducted();
                } else {
                    totalTdsOther += entry.getTotalTDSDeducted();
                }
            }
        } else if (ais != null && ais.getTdsSalary() != null) {
            // Legacy fallback
            for (AISData.TDSSalary entry : ais.getTdsSalary()) {
                FlatFormData.TDSEntry tdsEntry = FlatFormData.TDSEntry.builder()
                        .section("192")
                        .deductorName(entry.getDeductorName())
                        .deductorTAN(entry.getDeductorTAN())
                        .incomeAmount(entry.getTotalAmountPaid() != null ? entry.getTotalAmountPaid() : 0)
                        .tdsDeducted(entry.getTotalTaxDeducted() != null ? entry.getTotalTaxDeducted() : 0)
                        .financialYear("2024-25")
                        .verified26AS(false)
                        .claimedInReturn(true)
                        .build();
                tdsEntries.add(tdsEntry);
                totalTds192 += (entry.getTotalTaxDeducted() != null ? entry.getTotalTaxDeducted() : 0);
            }
        }
        
        flat.setTdsEntries(tdsEntries);
        flat.setTdsS192(totalTds192);
        flat.setTds194A(totalTds194A);
        flat.setTdsOther(totalTdsOther);

        // === TAX PAYMENTS from AIS Part B3 - CBDT Compliant Multi-Entry ===
        List<FlatFormData.AdvanceTaxEntry> advanceTaxEntries = new ArrayList<>();
        List<FlatFormData.SelfAssessmentTaxEntry> selfAssessmentTaxEntries = new ArrayList<>();
        double totalSelfTax = 0;
        double totalAdv15Jun = 0, totalAdv15Sep = 0, totalAdv15Dec = 0, totalAdv15Mar = 0;
        
        if (ais != null && ais.getPartB3() != null) {
            for (AISData.TaxPaymentAIS payment : ais.getPartB3()) {
                if (payment.getMinorHead().contains("Self Assessment")) {
                    FlatFormData.SelfAssessmentTaxEntry satEntry = FlatFormData.SelfAssessmentTaxEntry.builder()
                            .bsrCode(payment.getBsrCode())
                            .challanNo(payment.getChallanSerialNo())
                            .depositDate(payment.getDepositDate())
                            .amount(payment.getTotalAmount())
                            .cin(payment.getChallanSerialNo()) // CIN same as challan for now
                            .build();
                    selfAssessmentTaxEntries.add(satEntry);
                    totalSelfTax += payment.getTotalAmount();
                } else if (payment.getMinorHead().contains("Advance Tax")) {
                    FlatFormData.AdvanceTaxEntry advEntry = FlatFormData.AdvanceTaxEntry.builder()
                            .bsrCode(payment.getBsrCode())
                            .challanNo(payment.getChallanSerialNo())
                            .depositDate(payment.getDepositDate())
                            .amount(payment.getTotalAmount())
                            .cin(payment.getChallanSerialNo())
                            .build();
                    advanceTaxEntries.add(advEntry);
                    
                    // Map to quarters for backward compatibility
                    LocalDate date = payment.getDepositDate();
                    int year = date.getYear();
                    if (!date.isAfter(LocalDate.of(year, 6, 15))) {
                        totalAdv15Jun += payment.getTotalAmount();
                    } else if (!date.isAfter(LocalDate.of(year, 9, 15))) {
                        totalAdv15Sep += payment.getTotalAmount();
                    } else if (!date.isAfter(LocalDate.of(year, 12, 15))) {
                        totalAdv15Dec += payment.getTotalAmount();
                    } else {
                        totalAdv15Mar += payment.getTotalAmount();
                    }
                }
            }
        }
        
        flat.setAdvanceTaxEntries(advanceTaxEntries);
        flat.setSelfAssessmentTaxEntries(selfAssessmentTaxEntries);
        flat.setSelfTax(totalSelfTax);
        flat.setAdv15Jun(totalAdv15Jun);
        flat.setAdv15Sep(totalAdv15Sep);
        flat.setAdv15Dec(totalAdv15Dec);
        flat.setAdv15Mar(totalAdv15Mar);

        log.info("Auto-population complete: Salary={}, Interest FD={}, Interest SB={}, Dividend={}, TDS Entries={}, SAT Entries={}, Adv Tax Entries={}", 
                flat.getBasic(), interestFromDeposit, interestFromSavings, dividend, 
                tdsEntries.size(), selfAssessmentTaxEntries.size(), advanceTaxEntries.size());

        return flat;
    }

    // Backward compatibility methods for old IntegrationController endpoints
    public com.itr.dto.Itr1FormData autoPopulateFromForm16(com.itr.dto.Itr1FormData formData, com.itr.dto.Form16Data form16) {
        // Note: Itr1FormData doesn't have employerEntries field - this is a stub for backward compatibility
        // Multi-employer support is only available in FlatFormData
        log.info("Form16 auto-populate called for Itr1FormData - multi-employer not supported in this DTO");
        return formData;
    }

    public com.itr.dto.Itr1FormData autoPopulateFromAIS(com.itr.dto.Itr1FormData formData, AISData ais) {
        // Stub - implement if needed
        return formData;
    }

    public com.itr.dto.Itr1FormData autoPopulateFrom26AS(com.itr.dto.Itr1FormData formData, Form26ASData f26as) {
        // Stub - implement if needed
        return formData;
    }

    public com.itr.dto.Itr2FormData autoPopulateITR2FromAIS(com.itr.dto.Itr2FormData formData, AISData ais) {
        // Stub - implement if needed
        return formData;
    }

    public com.itr.dto.Itr2FormData autoPopulateITR2From26AS(com.itr.dto.Itr2FormData formData, Form26ASData f26as) {
        // Stub - implement if needed
        return formData;
    }
}
