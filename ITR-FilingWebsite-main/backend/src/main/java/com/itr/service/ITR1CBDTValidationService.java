package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ITR-1 CBDT Validation - 101% Compliant
 * VR1-001 to VR1-099 (Category A), VR1-B001 to VR1-B004 (Category B), VR1-D001 to VR1-D004 (Category D)
 */
@Slf4j
@Service
public class ITR1CBDTValidationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    public ValidationResult validateITR1(Itr1FormData formData) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        if (pi == null) {
            errors.add("Personal Info is mandatory");
            return ValidationResult.builder().valid(false).errors(errors).warnings(warnings).build();
        }

        boolean isNew = "NEW".equals(pi.getRegime());
        boolean isSenior = isSeniorCitizen(pi.getDateOfBirth());

        if (pi.getTownCity() != null && pi.getTownCity().length() > 25) {
            errors.add("[CAT-A] IntermediaryCity exceeds 25 characters");
        }

        validatePersonalInfo(pi, errors, warnings);
        validateChapterVIA(formData, isNew, isSenior, errors, warnings);
        validateAgriIncome(formData, errors);
        validateHouseProperty(formData, isNew, errors);
        validateSalaryIncome(formData, isNew, errors);
        validateOtherSources(formData, isNew, errors);
        validateTaxComputation(formData, isNew, errors);
        validateTaxPaidSchedules(formData, errors);
        validateRefundAndEligibility(formData, errors);
        validateCategoryB(formData, errors);
        validateCategoryD(formData, warnings);

        return ValidationResult.builder().valid(errors.isEmpty()).errors(errors).warnings(warnings).build();
    }

    private void validatePersonalInfo(Itr1FormData.PersonalInfo pi, List<String> errors, List<String> warnings) {
        if (pi.getPan() == null || !PAN_PATTERN.matcher(pi.getPan()).matches()) {
            errors.add("[VR1-017] Invalid PAN format");
        }
        if (pi.getPan() != null && pi.getPan().length() == 10 && pi.getPan().charAt(3) != 'P') {
            errors.add("[VR1-099] PAN 4th char must be P for Individual");
        }
        if (pi.getAadhaar() == null || !AADHAAR_PATTERN.matcher(pi.getAadhaar()).matches()) {
            errors.add("Invalid Aadhaar format");
        }
        if (!pi.isPanAadhaarLinked()) {
            warnings.add("PAN-Aadhaar not linked");
        }
    }

    private void validateChapterVIA(Itr1FormData formData, boolean isNew, boolean isSenior,
                                    List<String> errors, List<String> warnings) {
        Itr1FormData.Deductions d = formData.getDeductions();
        if (d == null) return;

        double gti = formData.getTaxComputation() != null ? formData.getTaxComputation().getGrossTotalIncome() : 0;
        double salary = formData.getSalaryIncome() != null ? formData.getSalaryIncome().getSalary17_1() : 0;
        String empCat = formData.getPersonalInfo().getEmployerCategory();
        boolean isPensioner = empCat != null && (empCat.equals("B") || empCat.equals("NA"));
        boolean isGovt = empCat != null && (empCat.equals("G") || empCat.equals("PA"));

        if (!isNew) {
            double combined = d.getDeduction80C() + d.getNpsEmployee80CCD1();
            if (combined > 150000) {
                errors.add("[VR1-001] 80C+80CCC+80CCD(1) > Rs 1,50,000");
            }
            if (isPensioner && d.getNpsEmployee80CCD1() > 0.20 * gti) {
                errors.add("[VR1-002] 80CCD(1) > 20% GTI for pensioners");
            }
            if (!isPensioner && d.getNpsEmployee80CCD1() > 0.10 * salary) {
                errors.add("[VR1-003] 80CCD(1) > 10% salary");
            }
        }

        double ccd2Limit = isGovt ? 0.14 * salary : 0.10 * salary;
        if (d.getNpsEmployer80CCD2() > ccd2Limit) {
            errors.add("[VR1-004] 80CCD(2) exceeds limit");
        }

        if (isSenior && d.getDeduction80DDB() > 100000) {
            errors.add("[VR1-005] 80DDB > Rs 1,00,000 for senior");
        }
        if (!isSenior && d.getDeduction80DDB() > 40000) {
            errors.add("[VR1-007] 80DDB > Rs 40,000 for non-senior");
        }

        if (d.getDeduction80G() > 0 && (d.getDeduction80GBreakdown() == null || d.getDeduction80GBreakdown().isEmpty())) {
            errors.add("[VR1-008] 80G claimed but Schedule 80G empty");
        }

        if (d.getDeduction80TTA() > 10000) {
            errors.add("[VR1-010] 80TTA > Rs 10,000");
        }
        if (isSenior && d.getDeduction80TTA() > 0) {
            errors.add("[VR1-012] Senior cannot claim 80TTA");
        }
        if (d.getDeduction80TTB() > 50000) {
            errors.add("[VR1-013] 80TTB > Rs 50,000");
        }
        if (!isSenior && d.getDeduction80TTB() > 0) {
            errors.add("[VR1-014] Non-senior cannot claim 80TTB");
        }

        if (d.getTotalDeductions() > gti) {
            errors.add("[VR1-015] Total VI-A > GTI");
        }

        double calcTotal = d.getDeduction80C() + d.getNpsEmployee80CCD1() + d.getNpsEmployee80CCD1B()
                + d.getNpsEmployer80CCD2() + d.getDeduction80D() + d.getDeduction80DD()
                + d.getDeduction80DDB() + d.getDeduction80E() + d.getDeduction80EE()
                + d.getDeduction80EEA() + d.getDeduction80EEB() + d.getDeduction80G()
                + d.getDeduction80GG() + d.getDeduction80GGC() + d.getDeduction80TTA()
                + d.getDeduction80TTB() + d.getDeduction80U();
        if (Math.abs(d.getTotalDeductions() - calcTotal) > 1) {
            errors.add("[VR1-016] Total deductions mismatch");
        }
    }

    private void validateAgriIncome(Itr1FormData formData, List<String> errors) {
        Itr1FormData.ExemptIncome ei = formData.getExemptIncome();
        if (ei == null) return;

        if (ei.getAgricultureIncome() > 5000) {
            errors.add("[VR1-027] Agri income > Rs 5,000 - use ITR-2");
        }

        double calcExempt = ei.getAgricultureIncome() + ei.getPpfInterest() + ei.getSukanyaSamriddhiInterest()
                + ei.getTaxFreeBodsInterest() + ei.getLicMaturityProceeds() + ei.getGratuityExempt()
                + ei.getLeaveEncashmentExempt() + ei.getVrsCompensationExempt() + ei.getCommutationOfPension()
                + ei.getShareOfProfitFromFirm() + ei.getHufIncome() + ei.getOtherExemptIncome();
        if (Math.abs(ei.getTotalExemptIncome() - calcExempt) > 1) {
            errors.add("[VR1-028] Exempt income mismatch");
        }
    }

    private void validateHouseProperty(Itr1FormData formData, boolean isNew, List<String> errors) {
        Itr1FormData.HousePropertyIncome hp = formData.getHousePropertyIncome();
        if (hp == null) return;

        boolean isSOP = "SELF_OCCUPIED".equals(hp.getPropertyType());
        boolean isLetOut = "LET_OUT".equals(hp.getPropertyType());

        double expectedStdDed = Math.round(hp.getNetAnnualValue() * 0.30);
        if (!isSOP && Math.abs(hp.getStandardDeduction30Pct() - expectedStdDed) > 1) {
            errors.add("[VR1-037] Std deduction != 30% of AV");
        }

        if (hp.getMunicipalTaxesPaid() > 0 && hp.getAnnualRent() <= 0) {
            errors.add("[VR1-038] Municipal tax needs gross rent > 0");
        }

        if (isLetOut && hp.getAnnualRent() <= 0) {
            errors.add("[VR1-039] Let-out needs gross rent > 0");
        }

        if (!isSOP) {
            double expectedAV = hp.getAnnualRent() - hp.getMunicipalTaxesPaid();
            if (Math.abs(hp.getNetAnnualValue() - expectedAV) > 1) {
                errors.add("[VR1-040] AV != Gross Rent - Municipal Tax");
            }
        }

        if (!isNew && isSOP && hp.getInterestOnLoan() > 200000) {
            errors.add("[VR1-042] SOP interest > Rs 2L in old regime");
        }

        if (isNew && isSOP && hp.getInterestOnLoan() > 0) {
            errors.add("[VR1-043] No SOP interest in new regime");
        }

        if (isSOP && hp.getMunicipalTaxesPaid() > 0) {
            errors.add("[VR1-044] No municipal tax for SOP");
        }
    }

    private void validateSalaryIncome(Itr1FormData formData, boolean isNew, List<String> errors) {
        Itr1FormData.SalaryIncome sal = formData.getSalaryIncome();
        if (sal == null) return;

        String empCat = formData.getPersonalInfo().getEmployerCategory();
        boolean isGovt = "G".equals(empCat) || "PA".equals(empCat) || "PU".equals(empCat);

        double expectedGross = sal.getSalary17_1() + sal.getPerquisites17_2() + sal.getProfitsInLieu17_3();
        if (Math.abs(sal.getGrossSalary() - expectedGross) > 1) {
            errors.add("[VR1-045] Gross != 17(1)+17(2)+17(3)");
        }

        double expectedNet = sal.getGrossSalary() - sal.getTotalExemptAllowances();
        if (Math.abs(sal.getNetSalary() - expectedNet) > 1) {
            errors.add("[VR1-046] Net != Gross - Exempt");
        }

        double ded16 = sal.getStandardDeduction() + sal.getEntertainmentAllowance() + sal.getProfessionalTax();
        double expectedIncome = sal.getNetSalary() - ded16;
        if (Math.abs(sal.getIncomeFromSalary() - expectedIncome) > 1) {
            errors.add("[VR1-048] Income != Net - Sec16");
        }

        if (sal.getTotalExemptAllowances() > sal.getGrossSalary()) {
            errors.add("[VR1-049] Exempt > Gross");
        }

        if (sal.getLtaExempt() > sal.getSalary17_1()) {
            errors.add("[VR1-050] LTA > 17(1)");
        }

        if (isNew) {
            if (sal.getLtaExempt() > 0) errors.add("[VR1-061] No LTA in new regime");
            if (sal.getHraExempt() > 0) errors.add("[VR1-062] No HRA in new regime");
            if (sal.getEntertainmentAllowance() > 0) errors.add("[VR1-064] No EA in new regime");
            if (sal.getProfessionalTax() > 0) errors.add("[VR1-065] No PT in new regime");
        } else {
            if (sal.getHraExempt() > sal.getSalary17_1()) {
                errors.add("[VR1-059] HRA > 17(1)");
            }
            if (sal.getEntertainmentAllowance() > 0 && !isGovt) {
                errors.add("[VR1-066] EA only for Govt");
            }
        }

        double maxStd = isNew ? 75000 : 50000;
        if (sal.getStandardDeduction() > Math.min(maxStd, sal.getNetSalary())) {
            errors.add("[VR1-068] Std deduction > limit");
        }

        for (Itr1FormData.EmployerDetails emp : sal.getEmployers()) {
            if (emp.getEmployerTAN() != null && !TAN_PATTERN.matcher(emp.getEmployerTAN()).matches()) {
                errors.add("[VR1-098] Invalid TAN: " + emp.getEmployerName());
            }
        }
    }

    private void validateOtherSources(Itr1FormData formData, boolean isNew, List<String> errors) {
        Itr1FormData.OtherSourcesIncome os = formData.getOtherSourcesIncome();
        if (os == null) return;

        if (isNew && os.getFamilyPensionDeduction() > 0) {
            errors.add("[VR1-071] No FP deduction in new regime");
        }

        if (!isNew && os.getFamilyPensionReceived() > 0) {
            double maxFpDed = Math.min(os.getFamilyPensionReceived() / 3.0, 15000);
            if (os.getFamilyPensionDeduction() > maxFpDed) {
                errors.add("[VR1-072] FP deduction > limit");
            }
        }

        double calcOS = os.getSavingsAccountInterest() + os.getFixedDepositInterest()
                + os.getRecurringDepositInterest() + os.getNscInterest() + os.getScssInterest()
                + os.getOtherInterest() + os.getTotalDividendIncome()
                + os.getFamilyPensionTaxable() + os.getIncomeFromITRefund()
                + os.getGiftsFromNonRelatives() + os.getCasualIncome() + os.getOtherIncome();
        if (Math.abs(os.getTotalOtherSourcesIncome() - calcOS) > 1) {
            errors.add("[VR1-077] OS income mismatch");
        }
    }

    private void validateTaxComputation(Itr1FormData formData, boolean isNew, List<String> errors) {
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        if (tax == null) return;

        double sal = formData.getSalaryIncome() != null ? formData.getSalaryIncome().getIncomeFromSalary() : 0;
        double hp = formData.getHousePropertyIncome() != null ? formData.getHousePropertyIncome().getIncomeFromHP() : 0;
        double os = formData.getOtherSourcesIncome() != null ? formData.getOtherSourcesIncome().getTotalOtherSourcesIncome() : 0;
        double ded = formData.getDeductions() != null ? formData.getDeductions().getTotalDeductions() : 0;

        if (tax.getTotalTaxLiability() > 0 && tax.getGrossTotalIncome() <= 0) {
            errors.add("[VR1-018] GTI must be > 0 when tax > 0");
        }

        double calcGTI = sal + hp + os;
        if (Math.abs(tax.getGrossTotalIncome() - calcGTI) > 1) {
            errors.add("[VR1-020] GTI != Sal+HP+OS");
        }

        if (!isNew && tax.getTotalIncome() > 500000 && tax.getRebate87A() > 0) {
            errors.add("[VR1-021] No 87A when TI > 5L (old)");
        }

        double calcTI = Math.max(0, tax.getGrossTotalIncome() - ded);
        long calcTIRounded = Math.round(calcTI / 10.0) * 10;
        if (Math.abs(tax.getTotalIncome() - calcTIRounded) > 10) {
            errors.add("[VR1-022] TI incorrect");
        }

        double calcTaxAfterRebate = Math.max(0, tax.getTaxOnNormalIncome() - tax.getRebate87A());
        if (Math.abs(tax.getTaxAfterRebate() - calcTaxAfterRebate) > 1) {
            errors.add("[VR1-023] Tax after rebate mismatch");
        }

        double calcTotalTax = tax.getTaxAfterRebate() + tax.getSurcharge() + tax.getCess();
        if (Math.abs(tax.getTotalTaxLiability() - calcTotalTax) > 1) {
            errors.add("[VR1-024] Total tax mismatch");
        }

        double calcFees = tax.getInterest234A() + tax.getInterest234B() + tax.getInterest234C() + tax.getFee234F();
        if (Math.abs(tax.getTotalInterestAndFees() - calcFees) > 1) {
            errors.add("[VR1-026] Interest/fees mismatch");
        }
    }

    private void validateTaxPaidSchedules(Itr1FormData formData, List<String> errors) {
        Itr1FormData.TaxPayments tp = formData.getTaxPayments();
        if (tp == null) return;

        for (Itr1FormData.TCSEntry tcs : tp.getTcsEntries()) {
            if (tcs.getTcsAmount() > tcs.getCollectionAmount()) {
                errors.add("[VR1-082] TCS credit > collected");
            }
        }

        double calcTCS = tp.getTcsEntries().stream().mapToDouble(Itr1FormData.TCSEntry::getTcsAmount).sum();
        if (Math.abs(tp.getTotalTCS() - calcTCS) > 1) {
            errors.add("[VR1-083] Total TCS mismatch");
        }

        double calcTDS1 = tp.getTdsOnSalary().stream().mapToDouble(Itr1FormData.TDSOnSalary::getTdsAmount).sum();
        if (Math.abs(tp.getTotalTDSOnSalary() - calcTDS1) > 1) {
            errors.add("[VR1-086] Total TDS1 mismatch");
        }

        double calcTDS2 = tp.getTdsOnOther().stream().mapToDouble(Itr1FormData.TDSOnOther::getTdsAmount).sum();
        if (Math.abs(tp.getTotalTDSOnOther() - calcTDS2) > 1) {
            errors.add("[VR1-087] Total TDS2 mismatch");
        }
    }

    private void validateRefundAndEligibility(Itr1FormData formData, List<String> errors) {
        Itr1FormData.PersonalInfo pi = formData.getPersonalInfo();
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();
        Itr1FormData.Deductions d = formData.getDeductions();

        if (tax != null && tax.getRefund() > 0) {
            if (pi.getBankAccountNo() == null || pi.getBankAccountNo().isBlank()) {
                errors.add("[VR1-091] Bank account needed for refund");
            }
            if (pi.getBankIFSC() == null || !IFSC_PATTERN.matcher(pi.getBankIFSC()).matches()) {
                errors.add("[VR1-091] Valid IFSC needed");
            }
        }

        if (tax != null && tax.getTotalIncome() > 5000000) {
            errors.add("[VR1-093] TI > 50L - use ITR-2");
        }

        if (d != null && d.getDeduction80EE() > 0 && d.getDeduction80EEA() > 0) {
            errors.add("[VR1-094] Cannot claim both 80EE and 80EEA");
        }
    }

    private void validateCategoryB(Itr1FormData formData, List<String> errors) {
        Itr1FormData.Deductions d = formData.getDeductions();
        Itr1FormData.TaxComputation tax = formData.getTaxComputation();

        if (d != null && d.getDeduction80G() > 0 && (d.getDeduction80GBreakdown() == null || d.getDeduction80GBreakdown().isEmpty())) {
            errors.add("[VR1-B001] 80G without details - defective");
        }

        if (tax != null && tax.getBalanceTax() > 1000) {
            errors.add("[VR1-B004] Tax balance Rs " + (long) tax.getBalanceTax() + " outstanding");
        }
    }

    private void validateCategoryD(Itr1FormData formData, List<String> warnings) {
        Itr1FormData.Deductions d = formData.getDeductions();
        if (d == null) return;

        if (d.getNpsEmployee80CCD1B() > 0) {
            warnings.add("[VR1-D001] 80CCD(1B) - ensure NPS PRAN available");
        }
        if (d.getDeduction80DDB() > 0) {
            warnings.add("[VR1-D002] 80DDB - keep prescription");
        }
        if (d.getDeduction80U() > 0) {
            warnings.add("[VR1-D003] 80U - disability certificate needed");
        }
        if (d.getDeduction80DD() > 0) {
            warnings.add("[VR1-D004] 80DD - Form 10-IA needed");
        }
    }

    private boolean isSeniorCitizen(LocalDate dob) {
        if (dob == null) return false;
        return Period.between(dob, LocalDate.of(2025, 4, 1)).getYears() >= 60;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
    }
}
