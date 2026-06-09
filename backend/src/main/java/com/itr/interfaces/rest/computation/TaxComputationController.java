package com.itr.interfaces.rest.computation;

import com.itr.application.computation.RegimeComparisonUseCase;
import com.itr.application.computation.TaxComputationUseCase;
import com.itr.domain.common.TaxComputationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TaxComputationController — /api/v1/computation/*
 */
@RestController
@RequestMapping("/api/v1/computation")
public class TaxComputationController {

    private final TaxComputationUseCase taxComputationUseCase;
    private final RegimeComparisonUseCase regimeComparisonUseCase;

    public TaxComputationController(TaxComputationUseCase taxComputationUseCase,
                                      RegimeComparisonUseCase regimeComparisonUseCase) {
        this.taxComputationUseCase = taxComputationUseCase;
        this.regimeComparisonUseCase = regimeComparisonUseCase;
    }

    @PostMapping("/calculate")
    public ResponseEntity<TaxComputationResult> calculate(
            @RequestBody TaxComputationUseCase.ComputationInput input) {
        TaxComputationResult result = taxComputationUseCase.compute(input);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/regime-compare")
    public ResponseEntity<RegimeComparisonUseCase.RegimeComparisonResult> regimeCompare(
            @RequestBody TaxComputationUseCase.ComputationInput input) {
        var result = regimeComparisonUseCase.compare(input);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/quick-estimate")
    public ResponseEntity<TaxComputationResult> quickEstimate(
            @RequestBody TaxComputationUseCase.ComputationInput input) {
        TaxComputationResult result = taxComputationUseCase.compute(input);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{clientId}/{ay}")
    public ResponseEntity<TaxComputationResult> getSavedComputation(
            @PathVariable Long clientId,
            @PathVariable String ay) {
        // Placeholder — would fetch from DB via ClientYearDataRepository
        return ResponseEntity.ok(null);
    }
}
