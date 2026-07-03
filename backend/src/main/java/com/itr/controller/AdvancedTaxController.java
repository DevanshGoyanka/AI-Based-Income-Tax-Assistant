package com.itr.controller;

import com.itr.domain.advancetax.*;
import com.itr.domain.businessincome.*;
import com.itr.domain.capitalgains.*;
import com.itr.domain.houseproperty.*;
import com.itr.domain.salary.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * REST controller for advanced tax computation operations.
 * Provides endpoints for HRA exemption, depreciation, section 14A, relief 89, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/advanced-tax")
@RequiredArgsConstructor
public class AdvancedTaxController {

    /**
     * Calculate HRA exemption.
     * Validates against actual HRA received, rent paid, and metro city status.
     */
    @PostMapping("/hra")
    public ResponseEntity<Map<String, Object>> calculateHRA(@RequestBody Map<String, Object> request) {
        log.debug("Calculating HRA exemption");

        try {
            Double hraReceived = convertToDouble(request.get("hraReceived"));
            Double rentPaid = convertToDouble(request.get("rentPaid"));
            String cityType = (String) request.getOrDefault("cityType", "NON_METRO");
            Double basicSalary = convertToDouble(request.get("basicSalary"));
            
            boolean isMetro = "METRO".equalsIgnoreCase(cityType);
            long hraPaise = (long) (hraReceived * 100);
            long basicPaise = (long) (basicSalary * 100);
            long rentPaise = (long) (rentPaid * 100);
            
            long exemptAmount = HRAExemption.compute(hraPaise, basicPaise, rentPaise, isMetro);
            long taxableAmount = hraPaise - exemptAmount;

            Map<String, Object> response = new HashMap<>();
            response.put("hraReceived", hraReceived);
            response.put("rentPaid", rentPaid);
            response.put("cityType", cityType);
            response.put("basicSalary", basicSalary);
            response.put("exemptAmount", exemptAmount / 100.0);
            response.put("taxableAmount", taxableAmount / 100.0);
            response.put("component", "HRA");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("HRA calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "HRA calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate Section 14A disallowance for tax-free income.
     */
    @PostMapping("/section14a")
    public ResponseEntity<Map<String, Object>> calculateSection14A(@RequestBody Map<String, Object> request) {
        log.debug("Calculating Section 14A disallowance");

        try {
            Double taxFreeIncome = convertToDouble(request.get("taxFreeIncome"));
            Double actualExpense = convertToDouble(request.get("actualExpense"));
            
            double disallowance = Math.max(taxFreeIncome * 0.3, actualExpense);

            Map<String, Object> response = new HashMap<>();
            response.put("taxFreeIncome", taxFreeIncome);
            response.put("actualExpense", actualExpense);
            response.put("disallowance", disallowance);
            response.put("component", "Section14A");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Section 14A calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Section 14A calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate Section 50C deemed sale for property transactions.
     */
    @PostMapping("/section50c")
    public ResponseEntity<Map<String, Object>> calculateSection50C(@RequestBody Map<String, Object> request) {
        log.debug("Calculating Section 50C deemed sale");

        try {
            Double saleConsideration = convertToDouble(request.get("saleConsideration"));
            Double stampDutyValue = convertToDouble(request.get("stampDutyValue"));
            
            double deemedSale = Math.max(saleConsideration, stampDutyValue);

            Map<String, Object> response = new HashMap<>();
            response.put("saleConsideration", saleConsideration);
            response.put("stampDutyValue", stampDutyValue);
            response.put("deemedSaleConsideration", deemedSale);
            response.put("capitalGainReduction", saleConsideration - deemedSale);
            response.put("component", "Section50C");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Section 50C calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Section 50C calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate Section 89 relief for advance salary/arrears.
     */
    @PostMapping("/relief89")
    public ResponseEntity<Map<String, Object>> calculateRelief89(@RequestBody Map<String, Object> request) {
        log.debug("Calculating Section 89 relief");

        try {
            Double totalArrears = convertToDouble(request.get("totalArrears"));
            Double taxOnArrearsCurrentYear = convertToDouble(request.get("taxOnArrearsCurrentYear"));
            Double taxOnArrearsIfSpread = convertToDouble(request.get("taxOnArrearsIfSpread"));
            
            double relief = Math.max(0, taxOnArrearsCurrentYear - taxOnArrearsIfSpread);

            Map<String, Object> response = new HashMap<>();
            response.put("totalArrears", totalArrears);
            response.put("taxOnArrearsCurrentYear", taxOnArrearsCurrentYear);
            response.put("taxOnArrearsIfSpread", taxOnArrearsIfSpread);
            response.put("relief", relief);
            response.put("component", "Section89");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Section 89 relief calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Section 89 relief calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate depreciation on assets.
     */
    @PostMapping("/depreciation")
    public ResponseEntity<Map<String, Object>> calculateDepreciation(@RequestBody Map<String, Object> request) {
        log.debug("Calculating depreciation");

        try {
            String assetClass = (String) request.get("assetClass");
            Double blockValue = convertToDouble(request.get("blockValue"));
            Double additions = convertToDouble(request.get("additions"));
            Double sales = convertToDouble(request.get("sales"));
            
            double rate = DepreciationRates.getRate(assetClass);
            double depAmount = (blockValue + additions - sales) * (rate / 100);

            Map<String, Object> response = new HashMap<>();
            response.put("assetClass", assetClass);
            response.put("blockValue", blockValue);
            response.put("additions", additions);
            response.put("sales", sales);
            response.put("depreciationRate", rate);
            response.put("depreciationAmount", depAmount);
            response.put("writtenDownValue", blockValue + additions - sales - depAmount);
            response.put("component", "Depreciation");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Depreciation calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Depreciation calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Get depreciation rates as per IT Act Section 32.
     */
    @GetMapping("/depreciation/rates")
    public ResponseEntity<Map<String, Object>> getDepreciationRates() {
        log.debug("Getting depreciation rates");

        Map<String, Object> response = new HashMap<>();
        response.put("buildings", DepreciationRates.getRate("BUILDINGS"));
        response.put("plantMachinery", DepreciationRates.getRate("PLANT_MACHINERY"));
        response.put("furniture", DepreciationRates.getRate("FURNITURE"));
        response.put("computers", DepreciationRates.getRate("COMPUTERS"));
        response.put("vehicles", DepreciationRates.getRate("VEHICLES"));
        response.put("intangibles", DepreciationRates.getRate("INTANGIBLES"));

        return ResponseEntity.ok(response);
    }

    /**
     * Consolidate income from multiple employers.
     */
    @PostMapping("/multi-employer")
    public ResponseEntity<Map<String, Object>> consolidateMultiEmployer(@RequestBody Map<String, Object> request) {
        log.debug("Consolidating multi-employer salary");

        try {
            List<Map<String, Object>> employers = (List<Map<String, Object>>) request.get("employers");
            
            double totalGrossSalary = 0;
            double totalTds = 0;
            
            for (Map<String, Object> emp : employers) {
                totalGrossSalary += convertToDouble(emp.get("grossSalary"));
                totalTds += convertToDouble(emp.get("tds"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("employerCount", employers.size());
            response.put("totalGrossSalary", totalGrossSalary);
            response.put("totalTds", totalTds);
            response.put("consolidated", true);
            response.put("component", "MultiEmployer");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Multi-employer consolidation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Multi-employer consolidation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate LTCG 112A grandfathering for listed equity shares.
     */
    @PostMapping("/ltcg-grandfathering")
    public ResponseEntity<Map<String, Object>> calculateLtcgGrandfathering(@RequestBody Map<String, Object> request) {
        log.debug("Calculating LTCG 112A grandfathering");

        try {
            Double purchasePrice = convertToDouble(request.get("purchasePrice"));
            Double fairMarketValue = convertToDouble(request.get("fairMarketValue"));
            Double salePrice = convertToDouble(request.get("salePrice"));
            
            double costOfAcquisition = Math.max(purchasePrice, fairMarketValue);
            long LTCG = (long) Math.max(0, salePrice - costOfAcquisition);
            long taxableLTCG = Math.max(0, LTCG - 125000);
            long tax = (long) (taxableLTCG * 0.125);

            Map<String, Object> response = new HashMap<>();
            response.put("purchasePrice", purchasePrice);
            response.put("fairMarketValue", fairMarketValue);
            response.put("costOfAcquisition", costOfAcquisition);
            response.put("salePrice", salePrice);
            response.put("LTCG", LTCG);
            response.put("exemption", 125000);
            response.put("taxableLTCG", taxableLTCG);
            response.put("taxRate", 0.125);
            response.put("tax", tax);
            response.put("component", "LTCG_112A");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("LTCG grandfathering calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "LTCG grandfathering calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate EPF taxation on contributions > 2.5L.
     */
    @PostMapping("/epf-taxation")
    public ResponseEntity<Map<String, Object>> calculateEpfTaxation(@RequestBody Map<String, Object> request) {
        log.debug("Calculating EPF taxation");

        try {
            Double totalContribution = convertToDouble(request.get("totalContribution"));
            
            double taxableAmount = Math.max(0, totalContribution - 250000);

            Map<String, Object> response = new HashMap<>();
            response.put("totalContribution", totalContribution);
            response.put("exemptionLimit", 250000);
            response.put("taxableAmount", taxableAmount);
            response.put("component", "EPF");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("EPF taxation calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "EPF taxation calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate minor child income clubbing.
     */
    @PostMapping("/clubbing/minor-child")
    public ResponseEntity<Map<String, Object>> calculateMinorChildClubbing(@RequestBody Map<String, Object> request) {
        log.debug("Calculating minor child income clubbing");

        try {
            Double minorIncome = convertToDouble(request.get("minorIncome"));
            Double parentIncome = convertToDouble(request.get("parentIncome"));
            
            double exemptAmount = Math.min(minorIncome, 1500);
            double taxableInMinorsHands = Math.max(0, minorIncome - 1500);
            double totalClubbed = parentIncome + taxableInMinorsHands;

            Map<String, Object> response = new HashMap<>();
            response.put("minorIncome", minorIncome);
            response.put("parentIncome", parentIncome);
            response.put("exemptAmount", exemptAmount);
            response.put("taxableInMinorsHands", taxableInMinorsHands);
            response.put("totalClubbedWithParent", totalClubbed);
            response.put("component", "MinorChildClubbing");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Minor child clubbing calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Minor child clubbing calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate spouse income clubbing.
     */
    @PostMapping("/clubbing/spouse")
    public ResponseEntity<Map<String, Object>> calculateSpouseClubbing(@RequestBody Map<String, Object> request) {
        log.debug("Calculating spouse income clubbing");

        try {
            Double spouseIncome = convertToDouble(request.get("spouseIncome"));
            String assetType = (String) request.get("assetType");

            Map<String, Object> response = new HashMap<>();
            response.put("spouseIncome", spouseIncome);
            response.put("assetType", assetType);
            response.put("clubbedAmount", spouseIncome);
            response.put("component", "SpouseClubbing");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Spouse clubbing calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Spouse clubbing calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate F&O trading tax.
     */
    @PostMapping("/fo-trading")
    public ResponseEntity<Map<String, Object>> calculateFoTrading(@RequestBody Map<String, Object> request) {
        log.debug("Calculating F&O trading tax");

        try {
            Double speculativeIncome = convertToDouble(request.get("speculativeIncome"));
            Double nonSpeculativeIncome = convertToDouble(request.get("nonSpeculativeIncome"));
            
            double stcg = nonSpeculativeIncome > 0 ? nonSpeculativeIncome : 0;
            double ltcg = nonSpeculativeIncome < 0 ? Math.abs(nonSpeculativeIncome) : 0;

            Map<String, Object> response = new HashMap<>();
            response.put("speculativeIncome", speculativeIncome);
            response.put("nonSpeculativeIncome", nonSpeculativeIncome);
            response.put("stcg30", stcg * 0.30);
            response.put("ltcg125", ltcg * 0.125);
            response.put("totalTax", (stcg * 0.30) + (ltcg * 0.125));
            response.put("component", "FOTtrading");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("F&O trading tax calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "F&O trading tax calculation failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate break-even analysis.
     */
    @PostMapping("/break-even")
    public ResponseEntity<Map<String, Object>> calculateBreakEven(@RequestBody Map<String, Object> request) {
        log.debug("Calculating break-even analysis");

        try {
            Double oldRegimeTax = convertToDouble(request.get("oldRegimeTax"));
            Double newRegimeTax = convertToDouble(request.get("newRegimeTax"));
            
            String recommendedRegime = newRegimeTax <= oldRegimeTax ? "NEW" : "OLD";
            double savings = Math.abs(oldRegimeTax - newRegimeTax);

            Map<String, Object> response = new HashMap<>();
            response.put("oldRegimeTax", oldRegimeTax);
            response.put("newRegimeTax", newRegimeTax);
            response.put("recommendedRegime", recommendedRegime);
            response.put("savings", savings);
            response.put("component", "BreakEven");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Break-even calculation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Break-even calculation failed: " + e.getMessage()));
        }
    }

    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return 0L;
    }
}

