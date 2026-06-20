package com.itr.config;

import com.itr.domain.common.AssessmentYear;
import com.itr.domain.common.TaxRegime;
import com.itr.domain.salary.EmployerEntry;
import com.itr.domain.salary.SalaryComputationResult;
import com.itr.domain.salary.SalaryScheduleComputer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Salary Computation Controller
 * All salary calculations done HERE in backend.
 * Frontend only displays results.
 */
// ========== SALARY CALCULATION CONTROLLER - COMMENTED OUT ==========
// Reason: Salary calculation now handled by TaxComputationOrchestrator
// The employerEntries are saved to formData and used when full tax computation is done
// To re-enable: Uncomment this entire class
/*
@Slf4j
@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
public class SalaryCalculationController {

    @PostMapping("/salary")
    public ResponseEntity<SalaryComputationResponse> calculateSalary(
            @RequestBody SalaryCalculationRequest request) {
        
        String ay = request.getAssessmentYear() != null ? request.getAssessmentYear() : "2026-27";
        log.info("Computing salary for AY: {}", ay);
        
        try {
            // Convert request to EmployerEntry list
            List<EmployerEntry> employers = request.getEmployers().stream()
                .map(SalaryCalculationRequest.EmployerInput::toEmployerEntry)
                .toList();
            
            // Determine regime
            TaxRegime regime = "NEW".equalsIgnoreCase(request.getTaxRegime())
                ? TaxRegime.NEW : TaxRegime.OLD;
            
            long stdDed = regime == TaxRegime.NEW
                ? AssessmentYear.NEW_REGIME_STANDARD_DEDUCTION
                : AssessmentYear.OLD_REGIME_STANDARD_DEDUCTION;
            
            // Compute using domain/salary
            SalaryComputationResult result = SalaryScheduleComputer.compute(
                employers, stdDed, regime, ay
            );
            
            log.info("Salary computed: Gross={}, Exemptions={}, Net={}",
                result.grossSalaryTotal(), result.totalSection10Exempt(), result.netTaxableSalary());
            
            return ResponseEntity.ok(SalaryComputationResponse.fromResult(result));
            
        } catch (Exception e) {
            log.error("Salary calculation failed", e);
            return ResponseEntity.internalServerError()
                .body(SalaryComputationResponse.error(e.getMessage()));
        }
    }
*/

/* — Commented out along with controller above — */

