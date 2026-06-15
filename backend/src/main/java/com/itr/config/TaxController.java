package com.itr.config;

import com.itr.domain.common.TaxComputationResult;
import com.itr.domain.common.TaxRegime;
import com.itr.service.TaxComputationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
            @RequestParam(defaultValue = "NEW") String regime) {
        log.info("Computing tax summary for regime: {}", regime);
        try {
            TaxRegime taxRegime = "OLD".equalsIgnoreCase(regime) ? TaxRegime.OLD : TaxRegime.NEW;
            
            log.info("Form data keys: {}", formData.keySet());
            log.info("Has dividendEntries: {}", formData.get("dividendEntries") instanceof java.util.List ? ((java.util.List<?>)formData.get("dividendEntries")).size() + " entries" : "no");
            
            TaxComputationResult result = taxComputationOrchestrator.computeTax(formData, taxRegime);
            
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
            response.put("taxRegime", result.getTaxRegime());
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
}
