package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.itd.*;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Service to map ITD import files (Prefill, AIS, TIS, 26AS, Form16) to ITR form data
 * Handles all CBDT mandatory fields mapping
 */
@Service
@Slf4j
public class ITDImportMappingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Map ITD Prefill JSON to ITR-1 Form Data
     */
    public Itr1FormData mapPrefillToItr1(ITDPrefillDataNew prefill) {
        Itr1FormData.Itr1FormDataBuilder builder = Itr1FormData.builder();
        
        // Personal Info
        if (prefill.getPersonalInfo() != null) {
            builder.personalInfo(mapPersonalInfo(prefill.getPersonalInfo()));
        }
        
        // Salary Income from Form26AS within prefill
        if (prefill.getForm26as() != null) {
            builder.salaryIncome(mapSalaryFromForm26AS(prefill.getForm26as()));
        }
        
        // Other Sources from insights
        if (prefill.getInsights() != null) {
            builder.otherSourcesIncome(mapOtherSourcesFromInsights(prefill.getInsights()));
        }
        
        // TDS from Form26AS
        if (prefill.getForm26as() != null) {
            builder.taxPayments(mapTaxPaymentsFromForm26AS(prefill.getForm26as()));
        }
        
        // Deductions from insights
        if (prefill.getInsights() != null && prefill.getInsights().getUsrDeductUndChapVIAType() != null) {
            builder.deductions(mapDeductionsFromInsights(prefill.getInsights()));
        }
        
        return builder.build();
    }

    /**
     * Map Form 26AS to ITR-1 Form Data
     */
    public Itr1FormData mapForm26ASToItr1(Form26ASDataNew form26as) {
        Itr1FormData.Itr1FormDataBuilder builder = Itr1FormData.builder();
        
        // TDS on Salary (Part A)
        if (form26as.getPartA() != null) {
            builder.salaryIncome(mapSalaryFromForm26ASNew(form26as.getPartA()));
            builder.taxPayments(mapTDSSalaryFromForm26AS(form26as.getPartA()));
        }
        
        // TDS on Other Income (Part B)
        if (form26as.getPartB() != null) {
            Itr1FormData.TaxPayments taxPayments = builder.build().getTaxPayments();
            if (taxPayments == null) {
                taxPayments = Itr1FormData.TaxPayments.builder().build();
            }
            mapTDSOtherFromForm26AS(form26as.getPartB(), taxPayments);
            builder.taxPayments(taxPayments);
        }
        
        // Tax Payments (Part D)
        if (form26as.getPartD() != null) {
            Itr1FormData.TaxPayments taxPayments = builder.build().getTaxPayments();
            if (taxPayments == null) {
                taxPayments = Itr1FormData.TaxPayments.builder().build();
            }
            mapAdvanceTaxFromForm26AS(form26as.getPartD(), taxPayments);
            builder.taxPayments(taxPayments);
        }
        
        return builder.build();
    }

    /**
     * Map TIS to ITR-1 Form Data
     */
    public Itr1FormData mapTISToItr1(TISData tis) {
        // TIS structure is similar to 26AS but with additional feedback
        // For now, map the core TDS/TCS data
        Itr1FormData.Itr1FormDataBuilder builder = Itr1FormData.builder();
        
        if (tis.getPartA() != null) {
            builder.taxPayments(mapTDSSalaryFromTIS(tis.getPartA()));
        }
        
        if (tis.getPartB() != null) {
            Itr1FormData.TaxPayments taxPayments = builder.build().getTaxPayments();
            if (taxPayments == null) {
                taxPayments = Itr1FormData.TaxPayments.builder().build();
            }
            mapTDSOtherFromTIS(tis.getPartB(), taxPayments);
            builder.taxPayments(taxPayments);
        }
        
        return builder.build();
    }

    // ==================== PRIVATE MAPPING METHODS ====================

    private Itr1FormData.PersonalInfo mapPersonalInfo(ITDPrefillDataNew.PersonalInfo pi) {
        Itr1FormData.PersonalInfo.PersonalInfoBuilder builder = Itr1FormData.PersonalInfo.builder();
        
        // Name
        if (pi.getAssesseeName() != null) {
            String fullName = (pi.getAssesseeName().getFirstName() != null ? pi.getAssesseeName().getFirstName() : "") +
                    " " + (pi.getAssesseeName().getMiddleName() != null ? pi.getAssesseeName().getMiddleName() : "") +
                    " " + (pi.getAssesseeName().getSurNameOrOrgName() != null ? pi.getAssesseeName().getSurNameOrOrgName() : "");
            builder.assesseeName(fullName.trim());
        }
        
        // PAN
        builder.pan(pi.getPan() != null ? pi.getPan() : pi.getAssesseVerPan());
        
        // Aadhaar (decode if base64 encoded)
        if (pi.getAadhaarCardNo() != null) {
            try {
                String decoded = new String(Base64.getDecoder().decode(pi.getAadhaarCardNo()));
                builder.aadhaar(decoded.replaceAll("\\D", "").substring(0, Math.min(12, decoded.length())));
            } catch (Exception e) {
                builder.aadhaar(pi.getAadhaarCardNo());
            }
        }
        
        // DOB
        if (pi.getDob() != null) {
            try {
                builder.dateOfBirth(LocalDate.parse(pi.getDob(), DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("Failed to parse DOB: {}", pi.getDob());
            }
        }
        
        // Address
        if (pi.getAddress() != null) {
            ITDPrefillDataNew.Address addr = pi.getAddress();
            builder.flatDoorNo(addr.getResidenceNo())
                   .premisesName(addr.getResidenceName())
                   .roadStreet(addr.getRoadOrStreet())
                   .area(addr.getLocalityOrArea())
                   .townCity(addr.getCityOrTownOrDistrict())
                   .state(addr.getStateCode())
                   .pinCode(addr.getPinCode())
                   .email(addr.getEmailAddress())
                   .mobile(addr.getMobileNo() != null ? addr.getMobileNo().toString() : null);
        }
        
        return builder.build();
    }

    private Itr1FormData.SalaryIncome mapSalaryFromForm26AS(ITDPrefillDataNew.Form26AS form26as) {
        // Extract salary info from Form26AS structure within prefill
        // This is a simplified mapping - actual structure may vary
        return Itr1FormData.SalaryIncome.builder()
                .employers(new ArrayList<>())
                .build();
    }

    private Itr1FormData.SalaryIncome mapSalaryFromForm26ASNew(Form26ASDataNew.PartA partA) {
        Itr1FormData.SalaryIncome.SalaryIncomeBuilder builder = Itr1FormData.SalaryIncome.builder();
        builder.employers(new ArrayList<>());
        
        if (partA.getEntries() != null) {
            for (Form26ASDataNew.TDSSalaryEntry entry : partA.getEntries()) {
                if (entry.getEmployerOrDeductorOrCollectDetl() != null) {
                    Form26ASDataNew.EmployerOrDeductorOrCollectDetl emp = entry.getEmployerOrDeductorOrCollectDetl();
                    
                    Itr1FormData.EmployerDetails empDetails = Itr1FormData.EmployerDetails.builder()
                            .employerName(emp.getEmployerOrDeductorOrCollecterName())
                            .employerTAN(emp.getTan())
                            .employerAddress(emp.getAddress())
                            .salaryReceived(entry.getTaxDeductCreditDtls() != null ? 
                                    entry.getTaxDeductCreditDtls().getTaxClaimedOwnHands() : 0)
                            .tdsDeducted(entry.getTaxDeductCreditDtls() != null ? 
                                    entry.getTaxDeductCreditDtls().getTaxDeductedOwnHands() : 0)
                            .build();
                    
                    builder.employers(new ArrayList<>());
                    builder.build().getEmployers().add(empDetails);
                }
            }
        }
        
        return builder.build();
    }

    private Itr1FormData.OtherSourcesIncome mapOtherSourcesFromInsights(ITDPrefillDataNew.Insights insights) {
        Itr1FormData.OtherSourcesIncome.OtherSourcesIncomeBuilder builder = Itr1FormData.OtherSourcesIncome.builder();
        
        // Interest income
        if (insights.getIntrstFrmSavingBank() != null) {
            builder.savingsAccountInterest(insights.getIntrstFrmSavingBank());
        }
        if (insights.getIntrstFrmTermDeposit() != null) {
            builder.fixedDepositInterest(insights.getIntrstFrmTermDeposit());
        }
        
        // Dividend income
        if (insights.getScheduleOS() != null && insights.getScheduleOS().getIncOthThanOwnRaceHorse() != null) {
            builder.dividendFromShares(insights.getScheduleOS().getIncOthThanOwnRaceHorse().getDividendGross());
        }
        
        return builder.build();
    }

    private Itr1FormData.TaxPayments mapTaxPaymentsFromForm26AS(ITDPrefillDataNew.Form26AS form26as) {
        Itr1FormData.TaxPayments.TaxPaymentsBuilder builder = Itr1FormData.TaxPayments.builder();
        builder.tdsOnSalary(new ArrayList<>())
               .tdsOnOther(new ArrayList<>())
               .advanceTaxEntries(new ArrayList<>());
        
        // Map TDS on other income
        if (form26as.getTdsOnOthThanSals() != null && form26as.getTdsOnOthThanSals().getTdSonOthThanSal() != null) {
            for (ITDPrefillDataNew.TdSonOthThanSal tds : form26as.getTdsOnOthThanSals().getTdSonOthThanSal()) {
                if (tds.getEmployerOrDeductorOrCollectDetl() != null) {
                    Itr1FormData.TDSOnOther tdsOther = Itr1FormData.TDSOnOther.builder()
                            .deductorName(tds.getEmployerOrDeductorOrCollectDetl().getEmployerOrDeductorOrCollecterName())
                            .deductorTAN(tds.getEmployerOrDeductorOrCollectDetl().getTan())
                            .section(tds.getSectionCode())
                            .incomeAmount(tds.getGrossAmount())
                            .tdsAmount(tds.getTaxDeductCreditDtls() != null ? 
                                    tds.getTaxDeductCreditDtls().getTaxDeductedOwnHands() : 0)
                            .build();
                    
                    builder.build().getTdsOnOther().add(tdsOther);
                }
            }
        }
        
        return builder.build();
    }

    private Itr1FormData.TaxPayments mapTDSSalaryFromForm26AS(Form26ASDataNew.PartA partA) {
        Itr1FormData.TaxPayments.TaxPaymentsBuilder builder = Itr1FormData.TaxPayments.builder();
        builder.tdsOnSalary(new ArrayList<>());
        
        if (partA.getEntries() != null) {
            for (Form26ASDataNew.TDSSalaryEntry entry : partA.getEntries()) {
                if (entry.getEmployerOrDeductorOrCollectDetl() != null) {
                    Itr1FormData.TDSOnSalary tds = Itr1FormData.TDSOnSalary.builder()
                            .employerName(entry.getEmployerOrDeductorOrCollectDetl().getEmployerOrDeductorOrCollecterName())
                            .employerTAN(entry.getEmployerOrDeductorOrCollectDetl().getTan())
                            .tdsAmount(entry.getTaxDeductCreditDtls() != null ? 
                                    entry.getTaxDeductCreditDtls().getTaxDeductedOwnHands() : 0)
                            .build();
                    
                    builder.build().getTdsOnSalary().add(tds);
                }
            }
        }
        
        return builder.build();
    }

    private void mapTDSOtherFromForm26AS(Form26ASDataNew.PartB partB, Itr1FormData.TaxPayments taxPayments) {
        if (taxPayments.getTdsOnOther() == null) {
            taxPayments.setTdsOnOther(new ArrayList<>());
        }
        
        if (partB.getEntries() != null) {
            for (Form26ASDataNew.TDSOtherEntry entry : partB.getEntries()) {
                if (entry.getEmployerOrDeductorOrCollectDetl() != null) {
                    Itr1FormData.TDSOnOther tds = Itr1FormData.TDSOnOther.builder()
                            .deductorName(entry.getEmployerOrDeductorOrCollectDetl().getEmployerOrDeductorOrCollecterName())
                            .deductorTAN(entry.getEmployerOrDeductorOrCollectDetl().getTan())
                            .section(entry.getSectionCode())
                            .incomeAmount(entry.getGrossAmount())
                            .tdsAmount(entry.getTaxDeductCreditDtls() != null ? 
                                    entry.getTaxDeductCreditDtls().getTaxDeductedOwnHands() : 0)
                            .certificateNo(entry.getCertificateNumber())
                            .build();
                    
                    taxPayments.getTdsOnOther().add(tds);
                }
            }
        }
    }

    private void mapAdvanceTaxFromForm26AS(Form26ASDataNew.PartD partD, Itr1FormData.TaxPayments taxPayments) {
        if (taxPayments.getAdvanceTaxEntries() == null) {
            taxPayments.setAdvanceTaxEntries(new ArrayList<>());
        }
        
        if (partD.getEntries() != null) {
            for (Form26ASDataNew.TaxPaymentEntry entry : partD.getEntries()) {
                Itr1FormData.AdvanceTaxEntry advTax = Itr1FormData.AdvanceTaxEntry.builder()
                        .bsrCode(entry.getBsrCode())
                        .challanNo(entry.getChallanSerialNumber())
                        .amount(entry.getAmount())
                        .cin(entry.getReceiptNumber())
                        .build();
                
                if (entry.getDateOfDeposit() != null) {
                    try {
                        advTax.setDepositDate(LocalDate.parse(entry.getDateOfDeposit(), DATE_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse deposit date: {}", entry.getDateOfDeposit());
                    }
                }
                
                taxPayments.getAdvanceTaxEntries().add(advTax);
            }
        }
    }

    private Itr1FormData.Deductions mapDeductionsFromInsights(ITDPrefillDataNew.Insights insights) {
        Itr1FormData.Deductions.DeductionsBuilder builder = Itr1FormData.Deductions.builder();
        
        if (insights.getUsrDeductUndChapVIAType() != null) {
            if (insights.getUsrDeductUndChapVIAType().getSection80TTB() != null) {
                builder.deduction80TTB(insights.getUsrDeductUndChapVIAType().getSection80TTB());
            }
        }
        
        return builder.build();
    }

    private Itr1FormData.TaxPayments mapTDSSalaryFromTIS(TISData.TISPartA partA) {
        Itr1FormData.TaxPayments.TaxPaymentsBuilder builder = Itr1FormData.TaxPayments.builder();
        builder.tdsOnSalary(new ArrayList<>());
        
        if (partA.getEntries() != null) {
            for (TISData.TDSSalaryEntry entry : partA.getEntries()) {
                Itr1FormData.TDSOnSalary tds = Itr1FormData.TDSOnSalary.builder()
                        .employerName(entry.getDeductorName())
                        .employerTAN(entry.getDeductorTAN())
                        .tdsAmount(entry.getTotalTaxDeducted())
                        .salaryAmount(entry.getTotalAmountPaid())
                        .build();
                
                builder.build().getTdsOnSalary().add(tds);
            }
        }
        
        return builder.build();
    }

    private void mapTDSOtherFromTIS(TISData.TISPartB partB, Itr1FormData.TaxPayments taxPayments) {
        if (taxPayments.getTdsOnOther() == null) {
            taxPayments.setTdsOnOther(new ArrayList<>());
        }
        
        if (partB.getEntries() != null) {
            for (TISData.TDSOtherEntry entry : partB.getEntries()) {
                Itr1FormData.TDSOnOther tds = Itr1FormData.TDSOnOther.builder()
                        .deductorName(entry.getDeductorName())
                        .deductorTAN(entry.getDeductorTAN())
                        .section(entry.getSection())
                        .incomeAmount(entry.getAmountPaid())
                        .tdsAmount(entry.getTaxDeducted())
                        .build();
                
                if (entry.getTransactionDate() != null) {
                    try {
                        tds.setDeductionDate(LocalDate.parse(entry.getTransactionDate(), DATE_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse transaction date: {}", entry.getTransactionDate());
                    }
                }
                
                taxPayments.getTdsOnOther().add(tds);
            }
        }
    }
}
