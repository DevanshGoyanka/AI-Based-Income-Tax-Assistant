package com.itr.controller;

import com.itr.domain.common.AssessmentYear;
import com.itr.domain.common.TaxComputationResult;
import com.itr.domain.common.TaxRegime;
import com.itr.domain.salary.SalaryComputationResult;
import com.itr.domain.salary.SalaryScheduleComputer;
import com.itr.service.TaxComputationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Tax Computation Controller
 * Handles tax summary computation requests
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TaxController {

    private final TaxComputationOrchestrator taxComputationOrchestrator;

    /**
     * Main tax computation endpoint (called by frontend)
     * POST /api/tax/compute
     * Also mapped as /tax-summary/compute for frontend compatibility
     */
    @PostMapping({"/api/tax/compute", "/tax-summary/compute"})
    public ResponseEntity<Map<String, Object>> computeTaxSummary(
            @RequestBody Map<String, Object> formData,
            @RequestParam(required = false) String regime) {
        // Get regime from formData body OR query param - default to OLD if not provided
        String regimeFromBody = (String) formData.getOrDefault("regime", "OLD");
        String effectiveRegime = (regime != null && !regime.isEmpty()) ? regime : regimeFromBody;
        if (effectiveRegime == null || effectiveRegime.isEmpty()) effectiveRegime = "OLD";
        log.info("Computing tax summary for regime: {} (query={}, body={})", effectiveRegime, regime, regimeFromBody);
        try {
            // Normalize regime: accept "old"/"new" (lowercase) or "OLD"/"NEW" (uppercase)
            String normalizedRegime = effectiveRegime.toUpperCase();
            TaxRegime taxRegime = "OLD".equals(normalizedRegime) ? TaxRegime.OLD : TaxRegime.NEW;
            log.info("Normalized regime: {} -> TaxRegime.{}", effectiveRegime, taxRegime);

            log.info("Form data keys: {}", formData.keySet());
            log.info("Has dividendEntries: {}", formData.get("dividendEntries") instanceof java.util.List ? ((java.util.List<?>)formData.get("dividendEntries")).size() + " entries" : "no");

            TaxComputationResult result = taxComputationOrchestrator.computeTax(formData, taxRegime);

            // ============ CALCULATE SALARY INCOME USING SalaryScheduleComputer ============
            SalaryComputationResult salaryResult = calculateSalaryIncome(formData, taxRegime);
            log.info("Salary computed for regime {}: Gross={}, Exempt={}, Net={}",
                    taxRegime,
                    paiseToRupees(salaryResult.grossSalaryTotal()),
                    paiseToRupees(salaryResult.totalSection10Exempt()),
                    paiseToRupees(salaryResult.netTaxableSalary()));

            // ============ CALCULATE OTHER SOURCES INCOME FOR RESPONSE ============
            // Get computed Other Sources from orchestrator - this has all the arrays and flat fields summed
            // We need to read from formData which may have the 26AS imported entries
            Map<String, Long> osBreakdown = calculateOtherSourcesIncome(formData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("grossTotalIncome", result.getGrossTotalIncome());
            response.put("netTaxableIncome", result.getNetTaxableIncome());
            response.put("totalTaxLiability", result.getTotalTaxLiability());
            response.put("balanceTaxPayable", result.getBalanceTaxPayable());
            response.put("refundAmount", result.getRefundAmount());
            response.put("taxRegime", taxRegime.name());
            response.put("status", "success");
            
            // OTHER SOURCES fields - detailed breakdown for frontend display
            response.put("otherIncome", osBreakdown.get("otherIncome"));
            response.put("totalInterest", osBreakdown.get("interest"));
            response.put("totalDividend", osBreakdown.get("dividend") + osBreakdown.get("dividend22e") + osBreakdown.get("dividend22f"));
            response.put("totalWinnings", osBreakdown.get("winnings"));
            response.put("vdaGains", osBreakdown.get("vda"));
            response.put("familyPensionIncome", osBreakdown.get("familyPension"));
            response.put("familyPensionDed", 0L);
            response.put("taxableGifts", osBreakdown.get("gifts"));
            
            // ITD-specific interest fields
            response.put("intrFrmSavingBank", osBreakdown.get("intrFrmSavingBank"));
            response.put("intrFrmTermDeposit", osBreakdown.get("intrFrmTermDeposit"));
            response.put("intrFrmIncmTaxRefund", osBreakdown.get("intrFrmIncmTaxRefund"));
            response.put("intrSec10XIFirstProviso", osBreakdown.get("intrSec10XIFirstProviso"));
            response.put("intrSec10XISecondProviso", osBreakdown.get("intrSec10XISecondProviso"));
            response.put("intrSec10XIIFirstProviso", osBreakdown.get("intrSec10XIIFirstProviso"));
            
            // Dividend breakdown
            response.put("dividend22e", osBreakdown.get("dividend22e"));
            response.put("dividend22f", osBreakdown.get("dividend22f"));
            response.put("dividend", osBreakdown.get("dividend")); // Regular 194 dividend

            // ============ SALARY INCOME — Schedule S breakdown ============
            // All computed by SalaryScheduleComputer (CBDT-compliant).
            // SalaryScheduleComputer returns values in PAISE — convert to RUPEES for the API.
            response.put("salaryIncome", paiseToRupees(salaryResult.netTaxableSalary()));
            response.put("grossSalary", paiseToRupees(salaryResult.grossSalaryTotal()));
            response.put("salary171", paiseToRupees(salaryResult.grossSalarySection17_1()));
            response.put("salary172", paiseToRupees(salaryResult.grossSalarySection17_2()));
            response.put("salary173", paiseToRupees(salaryResult.grossSalarySection17_3()));
            response.put("hraExempt", paiseToRupees(salaryResult.hraExempt()));
            response.put("ltaExempt", paiseToRupees(salaryResult.ltaExempt()));
            response.put("gratuityExempt", paiseToRupees(salaryResult.gratuityExempt()));
            response.put("leaveEncashmentExempt", paiseToRupees(salaryResult.leaveEncashmentExempt()));
            response.put("pensionCommutationExempt", paiseToRupees(salaryResult.pensionCommutationExempt()));
            response.put("transportExempt", paiseToRupees(salaryResult.transportAllowanceExempt()));
            response.put("childrenEducationExempt", paiseToRupees(salaryResult.childrenEducationExempt()));
            response.put("hostelExempt", paiseToRupees(salaryResult.hostelExpenditureExempt()));
            response.put("uniformExempt", paiseToRupees(salaryResult.uniformAllowanceExempt()));
            response.put("totalSection10Exempt", paiseToRupees(salaryResult.totalSection10Exempt()));
            response.put("standardDeduction", paiseToRupees(salaryResult.standardDeduction()));
            response.put("entertainmentAllowanceDed", paiseToRupees(salaryResult.entertainmentAllowanceDed()));
            response.put("professionalTaxDed", paiseToRupees(salaryResult.professionalTaxDed()));
            response.put("totalSection16Deductions", paiseToRupees(salaryResult.totalSection16Deductions()));
            response.put("salaryTDS", paiseToRupees(salaryResult.totalTDSDeducted()));
            response.put("salaryEmployerCount", salaryResult.employerCount());
            // HRA debug
            response.put("hraCondition1", paiseToRupees(salaryResult.hraCondition1_Actual()));
            response.put("hraCondition2", paiseToRupees(salaryResult.hraCondition2_RentMinus10Pct()));
            response.put("hraCondition3", paiseToRupees(salaryResult.hraCondition3_MetroPct()));
            response.put("hraIsMetro", salaryResult.hraIsMetroCity());
            response.put("hraCityClassified", salaryResult.hraCityClassified());
            
            log.info("OTHER SOURCES RESPONSE: Interest={}, Dividend={}, Winnings={}, VDA={}, FamilyPension={}, Gifts={}, Total={}",
                osBreakdown.get("interest"), osBreakdown.get("dividend"), osBreakdown.get("winnings"), 
                osBreakdown.get("vda"), osBreakdown.get("familyPension"), osBreakdown.get("gifts"), osBreakdown.get("otherIncome"));
            
            log.info("Tax result: gross={}, net={}, tax={}, tds={}, balance={}", 
                result.getGrossTotalIncome(), result.getNetTaxableIncome(), 
                result.getTotalTaxLiability(), result.getTdsAmount(), result.getBalanceTaxPayable());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Tax computation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Alternative endpoint for tax computation
     * POST /api/v1/tax-summary/compute
     */
    @PostMapping("/api/v1/tax-summary/compute")
    public ResponseEntity<Map<String, Object>> computeTaxSummaryV1(
            @RequestBody Map<String, Object> formData,
            @RequestParam(defaultValue = "NEW") String regime) {
        return computeTaxSummary(formData, regime);
    }
    
    /**
     * Helper method to extract Other Sources income from formData
     * Includes: interestEntries, bankInterestEntries, dividendEntries, winningsEntries, etc.
     * Categorizes by ITD tags for proper display
     */
    private Map<String, Long> calculateOtherSourcesIncome(Map<String, Object> formData) {
        Map<String, Long> result = new HashMap<>();
        
        // Interest categories (ITD tags)
        result.put("intrFrmSavingBank", 0L);    // 17A
        result.put("intrFrmTermDeposit", 0L);    // 17B
        result.put("intrFrmIncmTaxRefund", 0L);  // 17C
        result.put("intrSec10XIFirstProviso", 0L); // 17D
        result.put("intrSec10XISecondProviso", 0L); // 17E - NSC exempt
        result.put("intrSec10XIIFirstProviso", 0L); // 17F - SCSS exempt
        result.put("interestOther", 0L);            // 17H - Others
        
        // Other categories
        result.put("dividend", 0L);
        result.put("dividend22e", 0L); // section 2(22)(e)
        result.put("dividend22f", 0L); // section 2(22)(f)
        result.put("winnings", 0L);
        result.put("vda", 0L);
        result.put("familyPension", 0L);
        result.put("gifts", 0L);
        result.put("otherIncome", 0L);
        
        try {
            // ==================== INTEREST - Categorize by ITD tag ====================
            Object interestEntries = formData.get("interestEntries");
            if (interestEntries instanceof java.util.List) {
                for (Object obj : (java.util.List<?>) interestEntries) {
                    if (obj instanceof Map) {
                        Map<String, Object> ie = (Map<String, Object>) obj;
                        String itdTag = (String) ie.get("itdTag");
                        long amt = extractLong(ie, "grossAmount");
                        
                        if (amt == 0) continue;
                        
                        // Categorize by ITD tag
                        if ("SAVINGS_BANK".equals(itdTag) || "IntrstFrmSavingBank".equals(itdTag)) {
                            result.put("intrFrmSavingBank", result.get("intrFrmSavingBank") + amt);
                        } else if ("TERM_DEPOSIT".equals(itdTag) || "IntrstFrmTermDeposit".equals(itdTag)) {
                            result.put("intrFrmTermDeposit", result.get("intrFrmTermDeposit") + amt);
                        } else if ("IT_REFUND".equals(itdTag)) {
                            result.put("intrFrmIncmTaxRefund", result.get("intrFrmIncmTaxRefund") + amt);
                        } else if ("POST_OFFICE".equals(itdTag)) {
                            result.put("intrSec10XIFirstProviso", result.get("intrSec10XIFirstProviso") + amt);
                        } else if ("NSC".equals(itdTag)) {
                            result.put("intrSec10XISecondProviso", result.get("intrSec10XISecondProviso") + amt);
                        } else if ("SCSS".equals(itdTag)) {
                            result.put("intrSec10XIIFirstProviso", result.get("intrSec10XIIFirstProviso") + amt);
                        } else {
                            result.put("interestOther", result.get("interestOther") + amt);
                        }
                    }
                }
            }
            
            // Also check bankInterestEntries (26AS import)
            if (result.get("intrFrmSavingBank") == 0 && result.get("intrFrmTermDeposit") == 0) {
                Object bankEntries = formData.get("bankInterestEntries");
                if (bankEntries instanceof java.util.List) {
                    for (Object obj : (java.util.List<?>) bankEntries) {
                        if (obj instanceof Map) {
                            long amt = extractLong((Map<String, Object>) obj, "interestEarned");
                            if (amt == 0) amt = extractLong((Map<String, Object>) obj, "interestAmount");
                            result.put("intrFrmSavingBank", result.get("intrFrmSavingBank") + amt);
                        }
                    }
                }
            }
            
            // ==================== DIVIDEND ====================
            Object divEntries = formData.get("dividendEntries");
            if (divEntries instanceof java.util.List) {
                for (Object obj : (java.util.List<?>) divEntries) {
                    if (obj instanceof Map) {
                        Map<String, Object> de = (Map<String, Object>) obj;
                        long amt = extractLong(de, "dividendAmount");
                        if (amt == 0) amt = extractLong(de, "grossAmount");
                        String section = (String) de.get("section");
                        
                        if ("2(22)(e)".equals(section)) {
                            result.put("dividend22e", result.get("dividend22e") + amt);
                        } else if ("2(22)(f)".equals(section)) {
                            result.put("dividend22f", result.get("dividend22f") + amt);
                        } else {
                            result.put("dividend", result.get("dividend") + amt);
                        }
                    }
                }
            }
            
            // ==================== WINNINGS ====================
            Object winEntries = formData.get("winningsEntries");
            if (winEntries instanceof java.util.List) {
                for (Object obj : (java.util.List<?>) winEntries) {
                    if (obj instanceof Map) {
                        result.put("winnings", result.get("winnings") + 
                            extractLong((Map<String, Object>) obj, "grossAmount"));
                    }
                }
            }
            
            // ==================== FAMILY PENSION ====================
            // Check familyPensionEntry object first, then flat field
            Object familyPensionObj = formData.get("familyPensionEntry");
            if (familyPensionObj instanceof Map) {
                Map<String, Object> fp = (Map<String, Object>) familyPensionObj;
                result.put("familyPension", extractLong(fp, "grossAmount"));
            } else {
                result.put("familyPension", extractLong(formData, "familyPension"));
            }
            
            // ==================== GIFTS ====================
            // Check giftEntries array first, then flat taxableGifts field
            Object giftEntries = formData.get("giftEntries");
            if (giftEntries instanceof java.util.List) {
                long totalGifts = 0;
                for (Object obj : (java.util.List<?>) giftEntries) {
                    if (obj instanceof Map) {
                        Map<String, Object> ge = (Map<String, Object>) obj;
                        long val = extractLong(ge, "value");
                        boolean fromRelative = Boolean.TRUE.equals(ge.get("fromRelative"));
                        boolean onMarriage = Boolean.TRUE.equals(ge.get("receivedOnMarriage"));
                        if (!fromRelative && !onMarriage) {
                            totalGifts += val;
                        }
                    }
                }
                // Apply Rs 50,000 threshold
                result.put("gifts", totalGifts > 50000 ? totalGifts : 0);
            } else {
                result.put("gifts", extractLong(formData, "taxableGifts"));
            }
            
            // ==================== TOTAL OTHER SOURCES ====================
            long totalInterest = result.get("intrFrmSavingBank") + result.get("intrFrmTermDeposit") + 
                               result.get("intrFrmIncmTaxRefund") + result.get("intrSec10XIFirstProviso") + 
                               result.get("intrSec10XISecondProviso") + result.get("intrSec10XIIFirstProviso") +
                               result.get("interestOther");
            long totalDividend = result.get("dividend") + result.get("dividend22e") + result.get("dividend22f");
            long totalOther = totalInterest + totalDividend + result.get("winnings") + result.get("vda") + 
                           result.get("familyPension") + result.get("gifts");
            
            result.put("interest", totalInterest);
            result.put("otherIncome", totalOther);
            
            log.info("OTHER SOURCES: Intr17A={}, 17B={}, Div={}, Win={}, VDA={}, Total={}",
                result.get("intrFrmSavingBank"), result.get("intrFrmTermDeposit"), 
                totalDividend, result.get("winnings"), result.get("vda"), totalOther);
            
        } catch (Exception e) {
            log.warn("Error calculating other sources income: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Extract long value from map, handling various input types
     */
    private long extractLong(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value == null) return 0;
            if (value instanceof Number) return ((Number) value).longValue();
            try {
                return (long) Double.parseDouble(value.toString().replaceAll("[^0-9.-]", ""));
            } catch (NumberFormatException e) { return 0; }
        } catch (Exception e) { return 0; }
    }

    /**
     * Convert paise (1 rupee = 100 paise) to rupees.
     * SalaryScheduleComputer returns all monetary values in paise.
     * The REST API exposes values in rupees for frontend display.
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

    // ── Salary Income Computation via SalaryScheduleComputer ─────────────────────

    /**
     * Convert formData.employerEntries[] (List of Maps) into
     * List of backend EmployerEntry records and compute via SalaryScheduleComputer.
     *
     * This replaces the inline salary math in TaxComputationOrchestrator and
     * ensures SalaryScheduleComputer (CBDT-compliant) is the single source of truth.
     */
    private SalaryComputationResult calculateSalaryIncome(Map<String, Object> formData, TaxRegime regime) {
        try {
            Object employerEntriesObj = formData.get("employerEntries");
            if (!(employerEntriesObj instanceof List)) {
                log.info("No employerEntries found — returning empty salary result");
                return SalaryComputationResult.empty("2026-27", regime);
            }

            List<?> rawList = (List<?>) employerEntriesObj;
            if (rawList.isEmpty()) {
                return SalaryComputationResult.empty("2026-27", regime);
            }

            List<com.itr.domain.salary.EmployerEntry> employers = new ArrayList<>();
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

            SalaryComputationResult result = SalaryScheduleComputer.compute(employers, stdDed, regime, ay);
            log.info("SalaryScheduleComputer [regime={}]: Gross={}p, Exempt={}p, Net={}p",
                    regime,
                    result.grossSalaryTotal(), result.totalSection10Exempt(), result.netTaxableSalary());
            return result;

        } catch (Exception e) {
            log.error("Error computing salary income: {}", e.getMessage(), e);
            return SalaryComputationResult.empty("2026-27", regime);
        }
    }

    /**
     * Convert a single employer entry Map from formData into a backend EmployerEntry record.
     * Field names in formData match EmployerEntryManager.tsx interface.
     */
    private com.itr.domain.salary.EmployerEntry mapToEmployerEntry(Map<String, Object> m) {
        // Core employer info
        String employerName = str(m, "customEmployerName");
        String tan = str(m, "employerTAN");

        // Section 17(1) components
        // Frontend sends values in RUPEES. Backend EmployerEntry expects PAISE.
        // Multiply by 100 to convert rupees → paise.
        long basic = rupeesToPaise(l(m, "basic"));
        long da = rupeesToPaise(l(m, "da"));
        long commission = rupeesToPaise(l(m, "commission"));
        long hra = rupeesToPaise(l(m, "hra"));
        long lta = rupeesToPaise(l(m, "lta"));
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

        // HRA inputs
        long annualRentPaid = rupeesToPaise(l(m, "rentPaid"));
        String city = str(m, "city");
        if (city == null || city.isEmpty()) city = str(m, "isMetroCity"); // fallback for boolean metro field
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
        long uncommutedPensionMonthly = 0L;
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

        // For city population and employer-owned accommodation — not captured in frontend
        long cityPopulation = 0L;
        boolean employerOwnedAccommodation = false;
        long actualRentByEmployer = 0L;

        return new com.itr.domain.salary.EmployerEntry(
                employerName, tan,
                // Section 17(1)
                basic, da, commission,
                hra, lta,
                transportAllowance, childrenEducationAllowance,
                hostelExpenditureAllowance, uniformAllowance,
                otherAllowances, bonus, arrearSalary,
                // Section 17(2)
                perquisitesValue,
                // Section 17(3)
                profitsInLieu,
                // Additional perquisites (all 0 — frontend uses single aggregate)
                0L, 0L, 0L, 0L,
                // HRA inputs
                annualRentPaid, city, cityPopulation,
                employerOwnedAccommodation, actualRentByEmployer,
                // Taxable benefits
                commutedPensionReceived, gratuityAlsoReceived,
                gratuityReceived, leaveEncashmentReceived,
                averageMonthlySalary, unavailedLeaveDays, uncommutedPensionMonthly,
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

    private long l(Map<String, Object> m, String key) {
        try {
            Object v = m.get(key);
            if (v == null) return 0L;
            if (v instanceof Number) return ((Number) v).longValue();
            return (long) Double.parseDouble(v.toString().replaceAll("[^0-9.-]", ""));
        } catch (Exception e) { return 0L; }
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
}
