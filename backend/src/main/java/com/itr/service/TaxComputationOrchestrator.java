package com.itr.service;

import com.itr.domain.common.*;
import com.itr.domain.salary.EmployerEntry;
import com.itr.domain.salary.SalaryComputationResult;
import com.itr.domain.salary.SalaryScheduleComputer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TaxComputationOrchestrator — computes tax from form data.
 *
 * NOTE: Salary income is computed using SalaryScheduleComputer
 * (the authoritative CBDT-compliant salary engine). This replaces
 * all inline salary math from the previous implementation.
 *
 * TaxController also calls SalaryScheduleComputer independently
 * to populate the REST response with ITD-tagged salary fields.
 */
@Slf4j
@Service
public class TaxComputationOrchestrator {

    public TaxComputationResult computeTax(Long clientId, String assessmentYear, TaxRegime regime) {
        return TaxComputationResult.builder()
            .grossTotalIncome(0).netTaxableIncome(0).totalTaxLiability(0).taxRegime(regime).build();
    }

    public TaxComputationResult computeTax(Map<String, Object> formData, TaxRegime regime) {
        log.info("Computing tax from form data, regime: {}", regime);

        try {
            // ==================== SALARY INCOME — via SalaryScheduleComputer ====================
            // SalaryScheduleComputer returns values in PAISE.
            // Convert to RUPEES for the rest of the orchestrator (which works in rupees).
            SalaryComputationResult salaryResult = computeSalaryFromFormData(formData, regime);
            long salaryIncome = paiseToRupees(salaryResult.netTaxableSalary());
            log.info("=== SALARY (SalaryScheduleComputer): Gross={}, Exempt={}, Net={}",
                    paiseToRupees(salaryResult.grossSalaryTotal()),
                    paiseToRupees(salaryResult.totalSection10Exempt()),
                    salaryIncome);

            // ==================== TDS from 26AS ====================
            Map<String, Object> imported26AS = getMap(formData, "imported26AS");
            long tdsFrom26AS = 0;
            if (imported26AS != null && !imported26AS.isEmpty()) {
                tdsFrom26AS = getLong(imported26AS, "totalTDS");
                if (tdsFrom26AS == 0) tdsFrom26AS = getLong(imported26AS, "totalTds");
            }

            // TDS from employerEntries (user-entered) — highest priority
            long tdsFromUser = paiseToRupees(salaryResult.totalTDSDeducted());
            if (tdsFromUser == 0) {
                Object employerEntriesObj = formData.get("employerEntries");
                if (employerEntriesObj instanceof List) {
                    for (Object empObj : (List<?>) employerEntriesObj) {
                        if (empObj instanceof Map) {
                            tdsFromUser += getLong((Map<String, Object>) empObj, "tdsDeducted");
                        }
                    }
                }
            }

            // ==================== HOUSE PROPERTY ====================
            long hpIncome = getLong(formData, "hpIncome");
            long homeLoanInt = getLong(formData, "homeLoanInt");
            hpIncome = hpIncome - homeLoanInt; // Deduction u/s 24

            // ==================== CAPITAL GAINS ====================
            long stcgIncome = getLong(formData, "stcgIncome");
            long ltcgIncome = getLong(formData, "ltcgIncome");

            // ==================== BUSINESS INCOME ====================
            long businessIncome = getLong(formData, "businessIncome");

            // ==================== OTHER SOURCES - INTEREST ====================
            long interestSB = getLong(formData, "interestSB");
            long interestFD = getLong(formData, "interestFD");
            long nsc = getLong(formData, "nscInterest");
            long scss = getLong(formData, "scssInterest");
            long totalInterest = interestSB + interestFD + nsc + scss;

            // ==================== OTHER SOURCES - DIVIDEND ====================
            long dividend = 0;
            Object dividendEntries = formData.get("dividendEntries");
            if (dividendEntries instanceof List) {
                for (Object obj : (List<?>) dividendEntries) {
                    if (obj instanceof Map) {
                        Map<String, Object> de = (Map<String, Object>) obj;
                        long amt = getLong(de, "dividendAmount");
                        if (amt == 0) amt = getLong(de, "grossAmount");
                        dividend += amt;
                    }
                }
            }
            if (dividend == 0) dividend = getLong(formData, "dividends");
            if (dividend == 0) dividend = getLong(formData, "totalDividend");

            // ==================== OTHER SOURCES - WINNINGS ====================
            long winnings = getLong(formData, "lotteryIncome")
                         + getLong(formData, "horseRaceIncome")
                         + getLong(formData, "cardGameIncome");
            if (winnings == 0) {
                Object winningsEntries = formData.get("winningsEntries");
                if (winningsEntries instanceof List) {
                    for (Object obj : (List<?>) winningsEntries) {
                        if (obj instanceof Map) {
                            winnings += getLong((Map<String, Object>) obj, "grossAmount");
                        }
                    }
                }
            }

            // ==================== OTHER SOURCES - INTEREST (from bankInterestEntries) ====================
            if (totalInterest == 0) {
                Object bankInterestEntries = formData.get("bankInterestEntries");
                if (bankInterestEntries instanceof List) {
                    for (Object obj : (List<?>) bankInterestEntries) {
                        if (obj instanceof Map) {
                            totalInterest += getLong((Map<String, Object>) obj, "interestAmount");
                        }
                    }
                }
            }

            // ==================== OTHER SOURCES - VDA ====================
            long vdaGains = getLong(formData, "vdaGains");

            // ==================== OTHER SOURCES - OTHERS ====================
            long familyPension = getLong(formData, "familyPension");
            long otherMisc = getLong(formData, "otherMisc");
            long taxableGifts = getLong(formData, "taxableGifts");

            long otherSources = totalInterest + dividend + winnings + vdaGains
                              + familyPension + otherMisc + taxableGifts;

            // ==================== GROSS TOTAL INCOME ====================
            long grossTotalIncome = salaryIncome + hpIncome + stcgIncome
                                 + ltcgIncome + businessIncome + otherSources;

            // ==================== DEDUCTIONS ====================
            long s80C = getLong(formData, "s80C")
                      + getLong(formData, "s80C_epf")
                      + getLong(formData, "s80C_lic")
                      + getLong(formData, "s80C_ppf")
                      + getLong(formData, "s80C_home");
            long s80D = getLong(formData, "s80D")
                      + getLong(formData, "s80D_self");
            long s80TTA = getLong(formData, "s80TTA");
            long s80G = getLong(formData, "s80G");
            long totalDeductions = s80C + s80D + s80TTA + s80G;

            // ==================== STANDARD DEDUCTION ====================
            // For OLD regime, std deduction is already included in salaryResult.totalSection16Deductions
            // For NEW regime, apply ₹75,000 (SalaryScheduleComputer already applied it)
            long standardDeduction = (regime == TaxRegime.NEW) ? 75000 : 0;
            long profTax = (regime == TaxRegime.OLD) ? 0 : Math.min(getLong(formData, "profTax"), 2500);

            // ==================== NET TAXABLE ====================
            long netTaxableIncome = Math.max(0,
                grossTotalIncome - totalDeductions - standardDeduction - profTax);

            // ==================== TAX ====================
            long tax = computeTax(netTaxableIncome, regime);
            long winningsTax = winnings * 30 / 100;
            long vdaTax = vdaGains * 30 / 100;
            long totalTaxBeforeRebate = tax + winningsTax + vdaTax;

            // ==================== REBATE 87A ====================
            long rebate = 0;
            if (regime == TaxRegime.NEW && netTaxableIncome <= 700000) {
                rebate = Math.min(25000, totalTaxBeforeRebate);
            } else if (regime == TaxRegime.OLD && netTaxableIncome <= 500000) {
                rebate = Math.min(12500, totalTaxBeforeRebate);
            }

            long taxAfterRebate = Math.max(0, totalTaxBeforeRebate - rebate);

            // ==================== SURCHARGE & CESS ====================
            long surcharge = 0;
            if (netTaxableIncome > 50000000) surcharge = taxAfterRebate * 25 / 100;
            else if (netTaxableIncome > 10000000) surcharge = taxAfterRebate * 15 / 100;
            else if (netTaxableIncome > 5000000) surcharge = taxAfterRebate * 10 / 100;

            long totalTaxLiability = taxAfterRebate + surcharge
                                 + (long) Math.round((taxAfterRebate + surcharge) * 4.0 / 100.0);

            // ==================== TDS ====================
            long totalTds = 0;
            if (tdsFromUser > 0) {
                totalTds = tdsFromUser;
            } else {
                totalTds = getLong(formData, "totalTds");
                if (totalTds == 0) totalTds = getLong(formData, "tdsS192");
                if (totalTds == 0 && tdsFrom26AS > 0) totalTds = tdsFrom26AS;
            }

            // ==================== BALANCE ====================
            long advanceTax = getLong(formData, "totalAdvanceTax");
            long selfTax = getLong(formData, "totalSelfAssessmentTax");
            long totalTaxPaid = totalTds + advanceTax + selfTax;

            long balanceTaxPayable = Math.max(0, totalTaxLiability - totalTaxPaid);
            long refundAmount = Math.max(0, totalTaxPaid - totalTaxLiability);

            log.info("=== INCOME: Salary={}, HP={}, CG={}, Business={}, Other={}, Gross={}",
                    salaryIncome, hpIncome, stcgIncome + ltcgIncome, businessIncome,
                    otherSources, grossTotalIncome);
            log.info("=== TAX: Normal={}, Special={}, Total={}, TDS={}, Balance={}",
                    tax, winningsTax + vdaTax, totalTaxLiability, totalTds, balanceTaxPayable);

            return TaxComputationResult.builder()
                .grossTotalIncome(grossTotalIncome)
                .netTaxableIncome(netTaxableIncome)
                .totalTaxLiability(totalTaxLiability)
                .balanceTaxPayable(balanceTaxPayable)
                .refundAmount(refundAmount)
                .tdsAmount(totalTds)
                .taxRegime(regime)
                .build();

        } catch (Exception e) {
            log.error("Tax computation error", e);
            return TaxComputationResult.builder()
                .grossTotalIncome(0).netTaxableIncome(0)
                .totalTaxLiability(0).taxRegime(regime).build();
        }
    }

    // ── Salary computation via SalaryScheduleComputer ─────────────────────────────────

    /**
     * Convert formData.employerEntries[] into backend EmployerEntry records
     * and compute via SalaryScheduleComputer.
     *
     * This is the SAME logic as TaxController.calculateSalaryIncome —
     * kept here so TaxComputationOrchestrator is self-contained.
     */
    private SalaryComputationResult computeSalaryFromFormData(
            Map<String, Object> formData, TaxRegime regime) {

        try {
            Object employerEntriesObj = formData.get("employerEntries");
            if (!(employerEntriesObj instanceof List)) {
                log.info("No employerEntries — returning empty salary result");
                return SalaryComputationResult.empty("2026-27", regime);
            }

            List<?> rawList = (List<?>) employerEntriesObj;
            if (rawList.isEmpty()) {
                return SalaryComputationResult.empty("2026-27", regime);
            }

            List<EmployerEntry> employers = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map) {
                    employers.add(mapToEmployerEntry((Map<String, Object>) item));
                }
            }

            if (employers.isEmpty()) {
                return SalaryComputationResult.empty("2026-27", regime);
            }

            String ay = (String) formData.getOrDefault("assessmentYear", "2026-27");
            long stdDed = regime == TaxRegime.NEW
                    ? AssessmentYear.NEW_REGIME_STANDARD_DEDUCTION
                    : AssessmentYear.OLD_REGIME_STANDARD_DEDUCTION;

            return SalaryScheduleComputer.compute(employers, stdDed, regime, ay);

        } catch (Exception e) {
            log.error("Error computing salary: {}", e.getMessage(), e);
            return SalaryComputationResult.empty("2026-27", regime);
        }
    }

    /**
     * Convert a single employer Map from formData into a backend EmployerEntry record.
     * Field names match EmployerEntryManager.tsx interface.
     *
     * Frontend sends values in RUPEES. Backend EmployerEntry expects PAISE.
     * Multiply by 100 to convert rupees → paise.
     */
    private EmployerEntry mapToEmployerEntry(Map<String, Object> m) {
        String employerName = str(m, "customEmployerName");
        String tan = str(m, "employerTAN");

        // Section 17(1)
        long basic = rupeesToPaise(l(m, "basic"));
        long da = rupeesToPaise(l(m, "da"));
        long commission = rupeesToPaise(l(m, "commission"));
        long hraReceived = rupeesToPaise(l(m, "hra"));
        long ltaReceived = rupeesToPaise(l(m, "lta"));
        long transportAllowance = rupeesToPaise(l(m, "transportAllowance"));
        long childrenEducationAllowance = rupeesToPaise(l(m, "childrenEducationAllowance"));
        long hostelExpenditureAllowance = rupeesToPaise(l(m, "hostelExpenditureAllowance"));
        long uniformAllowance = rupeesToPaise(l(m, "uniformAllowance"));
        long otherAllowances = rupeesToPaise(l(m, "otherAllowance"));
        long bonus = rupeesToPaise(l(m, "bonus"));
        long arrearSalary = rupeesToPaise(l(m, "arrearSalary"));

        // 17(2) & 17(3)
        long perquisitesValue = rupeesToPaise(l(m, "perquisites"));
        long profitsInLieu = rupeesToPaise(l(m, "profitsInLieu"));

        // HRA
        long annualRentPaid = rupeesToPaise(l(m, "rentPaid"));
        String city = str(m, "city");
        boolean isMetroCity = Boolean.TRUE.equals(m.get("isMetroCity"));
        // FIX: Map natureOfEmployment ('GOV'/'NGOV'/'PSU') to isGovernmentEmployee boolean
        String natureOfEmployment = str(m, "natureOfEmployment");
        boolean isGovernmentEmployee = "GOV".equalsIgnoreCase(natureOfEmployment)
                                    || "PSU".equalsIgnoreCase(natureOfEmployment)
                                    || Boolean.TRUE.equals(m.get("isGovernmentEmployee"));
        boolean isDisabledEmployee = Boolean.TRUE.equals(m.get("isDisabledEmployee"));

        // Retirement
        long commutedPensionReceived = rupeesToPaise(l(m, "commutedPension"));
        // FIX: Read gratuityAlsoReceived from frontend (was hardcoded to false)
        boolean gratuityAlsoReceived = Boolean.TRUE.equals(m.get("gratuityAlsoReceived"));
        long gratuityReceived = rupeesToPaise(l(m, "gratuity"));
        long leaveEncashmentReceived = rupeesToPaise(l(m, "leaveEncashment"));
        long averageMonthlySalary = rupeesToPaise(l(m, "averageMonthlySalary"));
        int unavailedLeaveDays = i(m, "unavailedLeaveDays");
        long actualLtaFare = rupeesToPaise(l(m, "actualLtaFare"));
        int numberOfChildren = i(m, "numberOfChildren");
        // FIX: Read isDomesticTravel from frontend (was hardcoded to true)
        boolean isDomesticTravel = m.get("isDomesticTravel") == null ? true : Boolean.TRUE.equals(m.get("isDomesticTravel"));
        int journeysInBlock = i(m, "journeysInBlock");
        int yearsOfService = i(m, "yearsOfService");

        // Deductions
        long professionalTax = rupeesToPaise(l(m, "professionalTax"));
        long entertainmentAllowanceReceived = rupeesToPaise(l(m, "entertainmentAllowance"));
        long employerNPSContribution = rupeesToPaise(l(m, "employerNPS"));

        // TDS
        long tdsDeducted = rupeesToPaise(l(m, "tdsDeducted"));

        return new EmployerEntry(
                employerName, tan,
                basic, da, commission,
                hraReceived, ltaReceived,
                transportAllowance, childrenEducationAllowance,
                hostelExpenditureAllowance, uniformAllowance,
                otherAllowances, bonus, arrearSalary,
                perquisitesValue,
                profitsInLieu,
                // Additional perquisites (0 — frontend uses single aggregate)
                0L, 0L, 0L, 0L,
                // HRA inputs
                annualRentPaid, city, 0L, false, 0L,
                // Taxable benefits
                commutedPensionReceived, gratuityAlsoReceived,
                gratuityReceived, leaveEncashmentReceived,
                averageMonthlySalary, unavailedLeaveDays, 0L,
                actualLtaFare, numberOfChildren, isDomesticTravel,
                journeysInBlock, yearsOfService,
                // Employer details
                isGovernmentEmployee, isDisabledEmployee,
                // Deductions
                professionalTax, entertainmentAllowanceReceived, employerNPSContribution,
                // TDS
                tdsDeducted
        );
    }

    // ── Tax slab calculation ───────────────────────────────────────────────────────

    private long computeTax(long income, TaxRegime regime) {
        if (income <= 0) return 0;
        if (regime == TaxRegime.NEW) {
            if (income <= 300000) return 0;
            else if (income <= 700000) return (income - 300000) * 5 / 100;
            else if (income <= 1000000) return 20000 + (income - 700000) * 10 / 100;
            else if (income <= 1200000) return 50000 + (income - 1000000) * 15 / 100;
            else if (income <= 1500000) return 80000 + (income - 1200000) * 20 / 100;
            else return 140000 + (income - 1500000) * 30 / 100;
        } else {
            if (income <= 250000) return 0;
            else if (income <= 500000) return (income - 250000) * 10 / 100;
            else if (income <= 1000000) return 25000 + (income - 500000) * 20 / 100;
            else return 125000 + (income - 1000000) * 30 / 100;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Convert paise (1 rupee = 100 paise) to rupees.
     * SalaryScheduleComputer returns all monetary values in paise.
     */
    private long paiseToRupees(long paise) {
        return paise / 100L;
    }

    /**
     * Convert rupees to paise (1 rupee = 100 paise).
     * Frontend sends monetary values in rupees. Backend EmployerEntry
     * record expects paise. Multiply by 100.
     */
    private long rupeesToPaise(long rupees) {
        return rupees * 100L;
    }

    private long getLong(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value == null) return 0;
            if (value instanceof Number) return ((Number) value).longValue();
            return (long) Double.parseDouble(value.toString().replaceAll("[^0-9.-]", ""));
        } catch (Exception e) { return 0; }
    }

    private long l(Map<String, Object> m, String key) {
        return getLong(m, key);
    }

    private int i(Map<String, Object> m, String key) {
        try {
            Object v = m.get(key);
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).intValue();
            return Integer.parseInt(v.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 0; }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value instanceof Map) return (Map<String, Object>) value;
            return null;
        } catch (Exception e) { return null; }
    }
}
