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
                .bankInterestEntries(new ArrayList<>())
                .capitalGainTransactions(new ArrayList<>())
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

        // Priority 1: TIS (most authoritative — accepted data)
        if (tis != null && tis.getInterestFromDeposit() > 0) {
            interestFromDeposit = tis.getInterestFromDeposit();
        }
        // Priority 2: AIS Part B2 SFT data (structured savings/deposit interest)
        else if (ais != null && ais.getPartB2() != null) {
            if (ais.getPartB2().getSavingsInterest() != null) {
                interestFromSavings = ais.getPartB2().getSavingsInterest().stream()
                        .mapToLong(AISData.InterestEntry::getTotalAmount)
                        .sum();
            }
            if (ais.getPartB2().getDepositInterest() != null) {
                interestFromDeposit = ais.getPartB2().getDepositInterest().stream()
                        .mapToLong(AISData.InterestEntry::getTotalAmount)
                        .sum();
            }
        }
        // Priority 3: AIS Part B1 TDS entries (fallback for TDS-reported interest)
        if (interestFromDeposit == 0 && interestFromSavings == 0
                && ais != null && ais.getPartB1() != null) {
            for (AISData.AISTDSEntry entry : ais.getPartB1().getTdsEntries()) {
                if ("194A".equals(entry.getSection())) {
                    interestFromDeposit += entry.getTotalAmountPaid();
                } else if ("194I".equals(entry.getSection())) {
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
        log.info("Interest income from AIS: FD={}, SB={}", interestFromDeposit, interestFromSavings);

        // === OTHER SOURCES — BANK INTEREST ENTRIES (CBDT Multi-Entry) ===
        // Populate per-bank entries from AIS Part B2 SFT savings/deposit interest
        if (ais != null && ais.getPartB2() != null) {
            // Savings interest from SFT
            if (ais.getPartB2().getSavingsInterest() != null) {
                for (AISData.InterestEntry entry : ais.getPartB2().getSavingsInterest()) {
                    FlatFormData.BankInterestEntry bie = FlatFormData.BankInterestEntry.builder()
                            .bankName(entry.getSourceName())
                            .accountType("SAVINGS")
                            .section("194I")
                            .sourceId(entry.getSourceId())
                            .interestEarned(entry.getTotalAmount())
                            .build();
                    flat.getBankInterestEntries().add(bie);
                    log.info("BankInterestEntry (SB): {} = {} ({})",
                            entry.getSourceName(), entry.getTotalAmount(), entry.getTransactionCount());
                }
            }
            // Deposit interest from SFT
            if (ais.getPartB2().getDepositInterest() != null) {
                for (AISData.InterestEntry entry : ais.getPartB2().getDepositInterest()) {
                    FlatFormData.BankInterestEntry bie = FlatFormData.BankInterestEntry.builder()
                            .bankName(entry.getSourceName())
                            .accountType("FD")
                            .section("194A")
                            .sourceId(entry.getSourceId())
                            .interestEarned(entry.getTotalAmount())
                            .build();
                    flat.getBankInterestEntries().add(bie);
                    log.info("BankInterestEntry (FD): {} = {} ({})",
                            entry.getSourceName(), entry.getTotalAmount(), entry.getTransactionCount());
                }
            }
        }

        // === OTHER SOURCES — DIVIDEND ===
        long dividend = 0;
        if (tis != null) dividend = tis.getDividendIncome();
        else if (ais != null && ais.getPartB2() != null) dividend = ais.getPartB2().getDividendIncome();
        flat.setDividends(dividend);
        log.info("Dividend income from AIS: {}", dividend);

        // === CAPITAL GAINS — CBDT Multi-Entry from AIS Part B2 SFT ===
        // 1. Property sale (Land/Building)
        if (ais != null && ais.getPartB2() != null && ais.getPartB2().getPropertySales() != null) {
            for (AISData.SFTSaleEntry sale : ais.getPartB2().getPropertySales()) {
                long saleAmount = sale.getSalesConsideration() != null ? sale.getSalesConsideration().longValue() : 0;
                long costBasis = sale.getCostOfAcquisition() != null ? sale.getCostOfAcquisition().longValue() : 0;
                long gain = saleAmount - costBasis;
                FlatFormData.CapitalGainTransaction cgt = FlatFormData.CapitalGainTransaction.builder()
                        .securityName(sale.getSecurityName() != null ? sale.getSecurityName() : "Land/Building Sale")
                        .saleDate(sale.getTransferDate())
                        .salePrice(saleAmount)
                        .purchasePrice(costBasis)
                        .quantity(1.0)
                        .assetType("LTCG")
                        .build();
                flat.getCapitalGainTransactions().add(cgt);
                log.info("CapitalGain (Property): {} | Sale: {}, Cost: {}",
                        sale.getSecurityName(), saleAmount, costBasis);
            }
        }

        // 2. MF/Securities sale (SFT-18)
        if (ais != null && ais.getPartB2() != null && ais.getPartB2().getMfSales() != null) {
            for (AISData.SFTSaleEntry sale : ais.getPartB2().getMfSales()) {
                long saleAmount = sale.getSalesConsideration() != null ? sale.getSalesConsideration().longValue() : 0;
                long costBasis = sale.getCostOfAcquisition() != null ? sale.getCostOfAcquisition().longValue() : 0;
                String assetType = sale.getSecurityName() != null
                        && (sale.getSecurityName().contains("MUTUAL") || sale.getSecurityName().contains("FUND"))
                        ? "MUTUAL_FUND" : "EQUITY";

                FlatFormData.CapitalGainTransaction cgt = FlatFormData.CapitalGainTransaction.builder()
                        .securityName(sale.getSecurityName())
                        .saleDate(sale.getTransferDate())
                        .salePrice(saleAmount)
                        .purchasePrice(costBasis)
                        .quantity(sale.getQuantity() != null ? sale.getQuantity().doubleValue() : 1.0)
                        .sttPaid(sale.getCostOfAcquisition() != null ? 0 : 0) // STT not available in AIS
                        .assetType("LTCG")
                        .build();
                flat.getCapitalGainTransactions().add(cgt);
                log.info("CapitalGain (MF/Sec): {} | Sale: {}, Cost: {}",
                        sale.getSecurityName(), saleAmount, costBasis);
            }
        }

        // === TDS FROM 26AS (for Schedule TDS2 — authoritative) ===
        // CBDT Compliant: Create separate TDS entries for each deductor
        List<FlatFormData.TDSEntry> tdsEntries = new ArrayList<>();
        double totalTds192 = 0;
        double totalTds194A = 0;
        double totalTdsOther = 0;
        
        // Use new Form26ASData structure with tdsOnInterest, tdsOnContractor, etc.
        if (f26as != null) {
            // Process TDS on Interest (194A)
            if (f26as.getTdsOnInterest() != null) {
                for (Form26ASData.TDSOtherThanSalary entry : f26as.getTdsOnInterest()) {
                    totalTds194A += addTDSEntry(tdsEntries, entry.getSection(), entry.getDeductorName(), 
                        entry.getDeductorTAN(), entry.getAmountPaid(), entry.getTaxDeducted());
                }
            }
            
            // Process TDS on Contract (194C)
            if (f26as.getTdsOnContractor() != null) {
                for (Form26ASData.TDSOtherThanSalary entry : f26as.getTdsOnContractor()) {
                    totalTdsOther += addTDSEntry(tdsEntries, entry.getSection(), entry.getDeductorName(),
                        entry.getDeductorTAN(), entry.getAmountPaid(), entry.getTaxDeducted());
                }
            }
            
            // Process TDS on Professional (194J)
            if (f26as.getTdsOnProfessional() != null) {
                for (Form26ASData.TDSOtherThanSalary entry : f26as.getTdsOnProfessional()) {
                    totalTdsOther += addTDSEntry(tdsEntries, entry.getSection(), entry.getDeductorName(),
                        entry.getDeductorTAN(), entry.getAmountPaid(), entry.getTaxDeducted());
                }
            }
            
            // Process other TDS
            if (f26as.getTdsOnOther() != null) {
                for (Form26ASData.TDSOtherThanSalary entry : f26as.getTdsOnOther()) {
                    totalTdsOther += addTDSEntry(tdsEntries, entry.getSection(), entry.getDeductorName(),
                        entry.getDeductorTAN(), entry.getAmountPaid(), entry.getTaxDeducted());
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

        log.info("Auto-population complete: Salary={}, Interest FD={}, Interest SB={}, Dividend={}, TDS Entries={}, BankInterestEntries={}, CapitalGainEntries={}, SAT Entries={}, Adv Tax Entries={}",
                flat.getBasic(), interestFromDeposit, interestFromSavings, dividend,
                tdsEntries.size(), flat.getBankInterestEntries().size(),
                flat.getCapitalGainTransactions().size(),
                selfAssessmentTaxEntries.size(), advanceTaxEntries.size());

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
    
    // Helper method to add TDS entry from Form26ASData
    private double addTDSEntry(List<FlatFormData.TDSEntry> tdsEntries, String section, String name, String tan, 
                               java.math.BigDecimal amount, java.math.BigDecimal taxDeducted) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) return 0;
        
        FlatFormData.TDSEntry tdsEntry = FlatFormData.TDSEntry.builder()
                .section(section != null ? section : "OTHER")
                .deductorName(name)
                .deductorTAN(tan)
                .incomeAmount(amount.doubleValue())
                .tdsDeducted(taxDeducted != null ? taxDeducted.doubleValue() : 0)
                .financialYear("2025-26")
                .verified26AS(true)
                .claimedInReturn(true)
                .build();
        
        tdsEntries.add(tdsEntry);
        
        return taxDeducted != null ? taxDeducted.doubleValue() : 0;
    }
}
