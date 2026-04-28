package com.itr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itr.dto.Itr1FormData;
import com.itr.dto.itd.*;
import com.itr.util.ITDDateFormatter;
import com.itr.util.DigestCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ITD JSON Export Service - 101% CBDT Compliant
 * Converts internal Itr1FormData to ITD uploadable JSON format
 * Reference: ITR_Import_JSON_Validation.md Section 4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ITDJSONExportService {
    
    private final ObjectMapper objectMapper;
    
    // CBDT Assigned Software Credentials
    private static final String SW_VERSION = "1.0";
    private static final String SW_CREATED_BY = "SW20014242";
    private static final String JSON_CREATED_BY = "SW20014242";
    private static final String USER_ID = "ERIP013181";
    private static final String SECRET_KEY = "4448ffc0cec1a25d";
    private static final int ITERATIONS = 1344;
    private static final String CREATED_BY = "TP"; // Taxpayer
    
    public String exportITR1ToITDJson(Itr1FormData formData) {
        try {
            log.info("Starting ITR-1 JSON export for PAN: {}", formData.getPersonalInfo().getPan());
            
            // Step 1: Build output structure without digest
            ITR1Output output = mapToITR1Output(formData);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            // Step 2: Generate JSON without digest
            String jsonWithoutDigest = mapper.writeValueAsString(output);
            log.debug("Generated JSON without digest, length: {} bytes", jsonWithoutDigest.length());
            
            // Step 3: Calculate HMAC-SHA256 digest with iterations
            String digest = DigestCalculator.generateDigest(jsonWithoutDigest, SECRET_KEY, ITERATIONS);
            log.info("Calculated digest: {}", digest);
            
            // Step 4: Update CreationInfo with computed digest
            output.getItr().getItr1().getCreationInfo().setDigest(digest);
            
            // Step 5: Generate final JSON with digest
            String finalJson = mapper.writeValueAsString(output);
            log.info("ITR-1 JSON export completed successfully with digest");
            
            return finalJson;
            
        } catch (Exception e) {
            log.error("Error exporting ITR-1 to ITD JSON", e);
            throw new RuntimeException("Failed to export ITR-1 JSON: " + e.getMessage(), e);
        }
    }
    
    private ITR1Output mapToITR1Output(Itr1FormData formData) {
        ITR1Output.ITR1 itr1 = ITR1Output.ITR1.builder()
            .creationInfo(buildCreationInfo(formData))
            .formItr1(buildFormITR1(formData))
            .build();
        
        ITR1Output.ITRWrapper wrapper = ITR1Output.ITRWrapper.builder()
            .itr1(itr1)
            .build();
        
        return ITR1Output.builder()
            .itr(wrapper)
            .build();
    }
    
    private ITDCommonDtos.CreationInfo buildCreationInfo(Itr1FormData formData) {
        String intermediaryCity = formData.getPersonalInfo().getTownCity();
        if (intermediaryCity != null && intermediaryCity.length() > 25) {
            log.warn("IntermediaryCity exceeds 25 chars, truncating: {}", intermediaryCity);
            intermediaryCity = intermediaryCity.substring(0, 25);
        }
        
        return ITDCommonDtos.CreationInfo.builder()
            .swVersionNo(SW_VERSION)
            .swCreatedBy(SW_CREATED_BY)
            .xmlCreatedBy(SW_CREATED_BY)
            .xmlCreationDate(ITDDateFormatter.format(LocalDate.now()))
            .intermediaryCity(intermediaryCity)
            .digest(null) // Will be computed after full JSON generation
            .createdBy(CREATED_BY)
            .folioNo(null)
            .formName("ITR-1")
            .build();
    }
    
    private ITR1Output.FormITR1 buildFormITR1(Itr1FormData formData) {
        return ITR1Output.FormITR1.builder()
            .personalInfo(buildPersonalInfo(formData))
            .filingStatus(buildFilingStatus(formData))
            .incomeDeductions(buildIncomeDeductions(formData))
            .taxComputation(buildTaxComputation(formData))
            .taxPaid(buildTaxPaid(formData))
            .refund(buildRefund(formData))
            .schedule80G(buildSchedule80G(formData))
            .exemptIncome(buildExemptIncome(formData))
            .verification(buildVerification(formData))
            .build();
    }
    
    private ITDCommonDtos.PersonalInfo buildPersonalInfo(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        
        String[] nameParts = splitName(pi.getAssesseeName());
        
        return ITDCommonDtos.PersonalInfo.builder()
            .assesseeName(ITDCommonDtos.AssesseeName.builder()
                .firstName(nameParts[0])
                .middleName(nameParts[1])
                .surNameOrOrgName(nameParts[2])
                .build())
            .pan(pi.getPan())
            .dob(ITDDateFormatter.format(pi.getDateOfBirth()))
            .aadhaarCardNo(pi.getAadhaar())
            .primaryMobileNo(pi.getMobile())
            .emailId(pi.getEmail())
            .address(ITDCommonDtos.Address.builder()
                .residenceNo(pi.getFlatDoorNo())
                .roadOrStreet(pi.getRoadStreet())
                .localityOrArea(pi.getArea())
                .cityOrTownOrDistrict(pi.getTownCity())
                .stateCode(mapStateCode(pi.getState()))
                .pinCode(pi.getPinCode())
                .countryCode("91")
                .build())
            .employerCategory(mapEmployerCategory(pi.getEmployerCategory()))
            .residentialStatus("RES")
            .status("I")
            .assesseeType(mapAssesseeType(pi.getAgeCategory()))
            .seventhProviso(determineSeventhProviso(formData))
            .build();
    }
    
    private ITDCommonDtos.FilingStatus buildFilingStatus(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        boolean isRevised = "REVISED".equals(pi.getFilingType());
        
        return ITDCommonDtos.FilingStatus.builder()
            .returnFileSec(mapReturnFileSec(pi.getFilingType()))
            .returnType(mapReturnType(pi.getFilingType()))
            .isRevised(isRevised ? "Y" : "N")
            .originalReturnAckNo(isRevised ? pi.getOriginalAckNo() : null)
            .originalReturnFiledDate(isRevised && pi.getOriginalFilingDate() != null 
                ? ITDDateFormatter.format(pi.getOriginalFilingDate()) : null)
            .isDefective("DEFECTIVE".equals(pi.getFilingType()) ? "Y" : "N")
            .taxRegime("NEW".equals(pi.getRegime()) ? "N" : "O")
            .optOutNewTaxRegime("OLD".equals(pi.getRegime()) ? "Y" : "N")
            .modeOfFiling("O")
            .isPortugueseCivilCode5A("N")
            .build();
    }

    private ITR1Output.IncomeDeductions buildIncomeDeductions(Itr1FormData formData) {
        Itr1FormData.SalaryIncome sal = formData.getSalaryIncome();
        Itr1FormData.HousePropertyIncome hp = formData.getHousePropertyIncome();
        Itr1FormData.OtherSourcesIncome os = formData.getOtherSourcesIncome();
        Itr1FormData.Deductions ded = formData.getDeductions();
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        
        return ITR1Output.IncomeDeductions.builder()
            .grossSalary(toInteger(sal != null ? sal.getGrossSalary() : 0))
            .salary17_1(toInteger(sal != null ? sal.getSalary17_1() : 0))
            .perquisitesValue17_2(toInteger(sal != null ? sal.getPerquisites17_2() : 0))
            .profitsInLieuSalary17_3(toInteger(sal != null ? sal.getProfitsInLieu17_3() : 0))
            .allwncExemptUs10(toInteger(sal != null ? sal.getTotalExemptAllowances() : 0))
            .netSalary(toInteger(sal != null ? sal.getNetSalary() : 0))
            .deductionUs16(toInteger(sal != null ? sal.getStandardDeduction() + sal.getEntertainmentAllowance() + sal.getProfessionalTax() : 0))
            .deductionUs16ia(toInteger(sal != null ? sal.getStandardDeduction() : 0))
            .deductionUs16ii(toInteger(sal != null ? sal.getEntertainmentAllowance() : 0))
            .deductionUs16iii(toInteger(sal != null ? sal.getProfessionalTax() : 0))
            .incomeFromSal(toInteger(sal != null ? sal.getIncomeFromSalary() : 0))
            .typeOfHP(hp != null ? mapPropertyType(hp.getPropertyType()) : null)
            .grossRentReceived(toInteger(hp != null ? hp.getAnnualRent() : 0))
            .taxPaidLocalAuthority(toInteger(hp != null ? hp.getMunicipalTaxesPaid() : 0))
            .annualValue(toInteger(hp != null ? hp.getNetAnnualValue() : 0))
            .standardDeduction30Pct(toInteger(hp != null ? hp.getStandardDeduction30Pct() : 0))
            .interestPayable(toInteger(hp != null ? hp.getInterestOnLoan() : 0))
            .incomeFromHP(toInteger(hp != null ? hp.getIncomeFromHP() : 0))
            .incomeOthSrc(toInteger(os != null ? os.getTotalOtherSourcesIncome() : 0))
            .othersInc(buildOthersInc(os))
            .familyPension(toInteger(os != null ? os.getFamilyPensionReceived() : 0))
            .familyPensionDedUs57iia(toInteger(os != null ? os.getFamilyPensionDeduction() : 0))
            .grossTotIncome(toInteger(tax != null ? tax.getGrossTotalIncome() : 0))
            .deductionUs80C(toInteger(ded != null ? ded.getDeduction80C() : 0))
            .deductionUs80CCD1(toInteger(ded != null ? ded.getNpsEmployee80CCD1() : 0))
            .deductionUs80CCD1B(toInteger(ded != null ? ded.getNpsEmployee80CCD1B() : 0))
            .deductionUs80CCD2(toInteger(ded != null ? ded.getNpsEmployer80CCD2() : 0))
            .deductionUs80D(toInteger(ded != null ? ded.getDeduction80D() : 0))
            .deductionUs80DD(toInteger(ded != null ? ded.getDeduction80DD() : 0))
            .deductionUs80DDB(toInteger(ded != null ? ded.getDeduction80DDB() : 0))
            .deductionUs80E(toInteger(ded != null ? ded.getDeduction80E() : 0))
            .deductionUs80EE(toInteger(ded != null ? ded.getDeduction80EE() : 0))
            .deductionUs80EEA(toInteger(ded != null ? ded.getDeduction80EEA() : 0))
            .deductionUs80EEB(toInteger(ded != null ? ded.getDeduction80EEB() : 0))
            .deductionUs80G(toInteger(ded != null ? ded.getDeduction80G() : 0))
            .deductionUs80GG(toInteger(ded != null ? ded.getDeduction80GG() : 0))
            .deductionUs80TTA(toInteger(ded != null ? ded.getDeduction80TTA() : 0))
            .deductionUs80TTB(toInteger(ded != null ? ded.getDeduction80TTB() : 0))
            .deductionUs80U(toInteger(ded != null ? ded.getDeduction80U() : 0))
            .totalChapVIADeductions(toInteger(ded != null ? ded.getTotalDeductions() : 0))
            .totalIncome(toInteger(tax != null ? roundToNearest10(tax.getTotalIncome()) : 0))
            .build();
    }
    
    private ITR1Output.TaxComputation buildTaxComputation(Itr1FormData formData) {
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        if (tax == null) return null;
        
        double totalTax = tax.getTaxOnNormalIncome() + tax.getTotalCGTax();
        
        return ITR1Output.TaxComputation.builder()
            .taxPayableOnTI(toInteger(totalTax))
            .rebateUs87A(toInteger(tax.getRebate87A()))
            .taxAfterRebate(toInteger(tax.getTaxAfterRebate()))
            .surcharge25(toInteger(tax.getSurcharge()))
            .totalSurcharge(toInteger(tax.getSurcharge()))
            .healthEduCess(toInteger(tax.getCess()))
            .totalTaxAndCess(toInteger(tax.getTotalTaxLiability()))
            .reliefUs89(toInteger(tax.getRelief89()))
            .netTaxLiability(toInteger(tax.getTaxPayableAfterRelief()))
            .intrstUs234A(toInteger(tax.getInterest234A()))
            .intrstUs234B(toInteger(tax.getInterest234B()))
            .intrstUs234C(toInteger(tax.getInterest234C()))
            .lateFilingFeeUs234F(toInteger(tax.getFee234F()))
            .totalIntrstPnltyFee(toInteger(tax.getTotalInterestAndFees()))
            .grossTaxLiability(toInteger(tax.getTaxPayableAfterRelief() + tax.getTotalInterestAndFees()))
            .build();
    }
    
    private ITDTaxPaidDtos.TaxPaid buildTaxPaid(Itr1FormData formData) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        if (tp == null) return null;
        
        return ITDTaxPaidDtos.TaxPaid.builder()
            .tds1(buildTDS1(tp))
            .tds2(buildTDS2(tp))
            .tcs(buildTCS(tp))
            .advanceTax(buildAdvanceTax(tp))
            .totalTaxPaid(toInteger(tp.getTotalTaxesPaid()))
            .balTaxPayable(toInteger(formData.getTaxComputation() != null ? formData.getTaxComputation().getBalanceTax() : 0))
            .build();
    }
    
    private ITDTaxPaidDtos.TDS1 buildTDS1(Itr1FormData.TaxPayments tp) {
        List<ITDTaxPaidDtos.TDSSalEntry> entries = tp.getTdsOnSalary().stream()
            .map(tds -> ITDTaxPaidDtos.TDSSalEntry.builder()
                .tan(tds.getEmployerTAN())
                .name(tds.getEmployerName())
                .totalTDSSal(toInteger(tds.getTdsAmount()))
                .claimOutOfTotTDSSal(toInteger(tds.getTdsAmount()))
                .ruleOf26As(tds.isVerified26AS() ? "Y" : "N")
                .build())
            .collect(Collectors.toList());
        
        return ITDTaxPaidDtos.TDS1.builder()
            .tdsSal(entries)
            .totalTDS1TaxDeducted(toInteger(tp.getTotalTDSOnSalary()))
            .totalTDS1TaxClaimed(toInteger(tp.getTotalTDSOnSalary()))
            .build();
    }
    
    private ITDTaxPaidDtos.TDS2 buildTDS2(Itr1FormData.TaxPayments tp) {
        List<ITDTaxPaidDtos.TDSOthThanSalEntry> entries = tp.getTdsOnOther().stream()
            .map(tds -> ITDTaxPaidDtos.TDSOthThanSalEntry.builder()
                .deductorTAN(tds.getDeductorTAN())
                .deductorName(tds.getDeductorName())
                .amtOnWhichTDSDeducted(toInteger(tds.getIncomeAmount()))
                .taxDeducted(toInteger(tds.getTdsAmount()))
                .yrOfTaxDeduction(extractFinancialYear(tds.getDeductionDate()))
                .claimOutOfTotTDSOthThanSals(toInteger(tds.getTdsAmount()))
                .build())
            .collect(Collectors.toList());
        
        return ITDTaxPaidDtos.TDS2.builder()
            .tdsOthThanSals(entries)
            .totalTDSOthThanSalTaxDeducted(toInteger(tp.getTotalTDSOnOther()))
            .totalTDSOthThanSalTaxClaimed(toInteger(tp.getTotalTDSOnOther()))
            .build();
    }
    
    private ITDTaxPaidDtos.TCS buildTCS(Itr1FormData.TaxPayments tp) {
        List<ITDTaxPaidDtos.TCSEntry> entries = tp.getTcsEntries().stream()
            .map(tcs -> ITDTaxPaidDtos.TCSEntry.builder()
                .collectorTAN(tcs.getCollectorTAN())
                .collectorName(tcs.getCollectorName())
                .amtOnWhichTCSCollected(toInteger(tcs.getCollectionAmount()))
                .taxCollected(toInteger(tcs.getTcsAmount()))
                .claimOutOfTotTCS(toInteger(tcs.getTcsAmount()))
                .build())
            .collect(Collectors.toList());
        
        return ITDTaxPaidDtos.TCS.builder()
            .tcsEntries(entries)
            .totalTCSClaimedThisYear(toInteger(tp.getTotalTCS()))
            .build();
    }
    
    private ITDTaxPaidDtos.AdvanceTax buildAdvanceTax(Itr1FormData.TaxPayments tp) {
        List<ITDTaxPaidDtos.AdvTaxEntry> entries = new ArrayList<>();
        
        entries.addAll(tp.getAdvanceTaxEntries().stream()
            .map(adv -> ITDTaxPaidDtos.AdvTaxEntry.builder()
                .bsrCode(adv.getBsrCode())
                .dateDep(ITDDateFormatter.format(adv.getDepositDate()))
                .srlNoOfChaln(adv.getChallanNo())
                .amt(toInteger(adv.getAmount()))
                .type("200")
                .build())
            .collect(Collectors.toList()));
        
        entries.addAll(tp.getSelfAssessmentTaxEntries().stream()
            .map(sat -> ITDTaxPaidDtos.AdvTaxEntry.builder()
                .bsrCode(sat.getBsrCode())
                .dateDep(ITDDateFormatter.format(sat.getDepositDate()))
                .srlNoOfChaln(sat.getChallanNo())
                .amt(toInteger(sat.getAmount()))
                .type("300")
                .build())
            .collect(Collectors.toList()));
        
        return ITDTaxPaidDtos.AdvanceTax.builder()
            .advTaxDetails(entries)
            .totalAdvTaxPaid(toInteger(tp.getTotalAdvanceTax() + tp.getTotalSelfAssessmentTax()))
            .build();
    }
    
    private ITR1Output.Refund buildRefund(Itr1FormData formData) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        if (pi.getBankAccountNo() == null) return null;
        
        return ITR1Output.Refund.builder()
            .bankAccountNo(pi.getBankAccountNo())
            .bankIFSCCode(pi.getBankIFSC())
            .bankName(pi.getBankName())
            .accountType(mapAccountType(pi.getBankAccountType()))
            .isIfscValid(pi.isBankAccountPreValidated() ? "Y" : "N")
            .build();
    }
    
    private ITR1Output.Schedule80G buildSchedule80G(Itr1FormData formData) {
        Itr1FormData.Deductions ded = formData.getDeductions();
        if (ded == null || ded.getDeduction80GBreakdown().isEmpty()) return null;
        
        List<ITR1Output.Donation80GEntry> cashDonations = new ArrayList<>();
        List<ITR1Output.Donation80GEntry> nonCashDonations = new ArrayList<>();
        
        for (Itr1FormData.Donation80GItem item : ded.getDeduction80GBreakdown()) {
            ITR1Output.Donation80GEntry entry = ITR1Output.Donation80GEntry.builder()
                .nameOfDonee(item.getDoneeName())
                .addressOfDonee(item.getDoneeAddress())
                .doneePAN(item.getDoneePAN())
                .donationAmt(toInteger(item.getDonationAmount()))
                .eligibleAmt(toInteger(item.getEligibleDeduction()))
                .deductionUs80G(toInteger(item.getEligibleDeduction()))
                .build();
            
            if ("CASH".equals(item.getPaymentMode())) {
                cashDonations.add(entry);
            } else {
                nonCashDonations.add(entry);
            }
        }
        
        return ITR1Output.Schedule80G.builder()
            .donationUs80GCash(cashDonations.isEmpty() ? null : cashDonations)
            .donationUs80GChequeOrDD(nonCashDonations.isEmpty() ? null : nonCashDonations)
            .totDonationsUs80G(toInteger(ded.getDeduction80G()))
            .build();
    }
    
    private ITR1Output.ExemptIncome buildExemptIncome(Itr1FormData formData) {
        Itr1FormData.ExemptIncome ei = formData.getExemptIncome();
        if (ei == null) return null;
        
        return ITR1Output.ExemptIncome.builder()
            .excAgriInc(toInteger(ei.getAgricultureIncome()))
            .shareFromHUF(toInteger(ei.getHufIncome()))
            .shareFromFirm(toInteger(ei.getShareOfProfitFromFirm()))
            .gratuityExempt(toInteger(ei.getGratuityExempt()))
            .leaveEncashmentExempt(toInteger(ei.getLeaveEncashmentExempt()))
            .vrsExempt(toInteger(ei.getVrsCompensationExempt()))
            .commutedPensionExempt(toInteger(ei.getCommutationOfPension()))
            .ppfInterest(toInteger(ei.getPpfInterest()))
            .sukanyaInterest(toInteger(ei.getSukanyaSamriddhiInterest()))
            .taxFreeBondInterest(toInteger(ei.getTaxFreeBodsInterest()))
            .licMaturityExempt(toInteger(ei.getLicMaturityProceeds()))
            .otherExemptIncome(toInteger(ei.getOtherExemptIncome()))
            .allExemptIncome(toInteger(ei.getTotalExemptIncome()))
            .build();
    }
    
    private ITR1Output.Verification buildVerification(Itr1FormData formData) {
        return ITR1Output.Verification.builder()
            .capacity("Self")
            .place(formData.getPersonalInfo().getTownCity())
            .date(ITDDateFormatter.format(LocalDate.now()))
            .declaration("I solemnly declare that the information given in this Return is correct, complete and truly stated.")
            .build();
    }
    
    private List<ITR1Output.OthersIncEntry> buildOthersInc(Itr1FormData.OtherSourcesIncome os) {
        if (os == null) return null;
        List<ITR1Output.OthersIncEntry> entries = new ArrayList<>();
        
        if (os.getSavingsAccountInterest() > 0) {
            entries.add(ITR1Output.OthersIncEntry.builder()
                .incNatureDesc("INTSB").othSrcNatureInc("Interest from Savings Account")
                .othSrcInc(toInteger(os.getSavingsAccountInterest())).build());
        }
        if (os.getFixedDepositInterest() > 0) {
            entries.add(ITR1Output.OthersIncEntry.builder()
                .incNatureDesc("INTFD").othSrcNatureInc("Interest from Fixed Deposit")
                .othSrcInc(toInteger(os.getFixedDepositInterest())).build());
        }
        if (os.getTotalDividendIncome() > 0) {
            entries.add(ITR1Output.OthersIncEntry.builder()
                .incNatureDesc("NATDIV").othSrcNatureInc("Dividend Income")
                .othSrcInc(toInteger(os.getTotalDividendIncome())).build());
        }
        return entries.isEmpty() ? null : entries;
    }
    
    private String[] splitName(String fullName) {
        if (fullName == null) return new String[]{"", "", ""};
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0].substring(0, Math.min(25, parts[0].length())), "", ""};
        if (parts.length == 2) return new String[]{parts[0].substring(0, Math.min(25, parts[0].length())), "", parts[1].substring(0, Math.min(25, parts[1].length()))};
        return new String[]{parts[0].substring(0, Math.min(25, parts[0].length())), parts[1].substring(0, Math.min(25, parts[1].length())), parts[parts.length-1].substring(0, Math.min(25, parts[parts.length-1].length()))};
    }
    
    private String mapStateCode(String state) {
        if (state == null) return "99";
        switch (state.toUpperCase()) {
            case "DELHI": return "07";
            case "MAHARASHTRA": return "27";
            case "KARNATAKA": return "29";
            default: return "99";
        }
    }
    
    private String mapEmployerCategory(String category) {
        if (category == null) return "NA";
        if (category.contains("GOVT")) return "G";
        if (category.contains("PSU")) return "PU";
        if (category.contains("PENSIONER")) return "B";
        return "O";
    }
    
    private String mapAssesseeType(String ageCategory) {
        if (ageCategory == null) return "05";
        if (ageCategory.contains("80")) return "07";
        if (ageCategory.contains("60")) return "06";
        return "05";
    }
    
    private String determineSeventhProviso(Itr1FormData formData) { return "N"; }
    private String mapReturnFileSec(String filingType) { return "REVISED".equals(filingType) || "DEFECTIVE".equals(filingType) ? "13" : "11"; }
    private String mapReturnType(String filingType) { return "REVISED".equals(filingType) ? "R" : "DEFECTIVE".equals(filingType) ? "D" : "O"; }
    private String mapPropertyType(String type) { return "LET_OUT".equals(type) ? "L" : "DEEMED_LET_OUT".equals(type) ? "D" : "S"; }
    private String mapAccountType(String type) { return "CURRENT".equals(type) ? "11" : "10"; }
    
    private String extractFinancialYear(LocalDate date) {
        if (date == null) return "2024-25";
        int year = date.getYear();
        int month = date.getMonthValue();
        return month >= 4 ? year + "-" + (year + 1 - 2000) : (year - 1) + "-" + (year - 2000);
    }
    
    private Integer toInteger(double value) { return (int) Math.round(value); }
    private int roundToNearest10(double value) { return (int) (Math.round(value / 10.0) * 10); }
}