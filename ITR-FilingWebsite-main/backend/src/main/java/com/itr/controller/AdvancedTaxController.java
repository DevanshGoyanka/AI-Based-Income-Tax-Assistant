package com.itr.controller;

import com.itr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/advanced-tax")
@RequiredArgsConstructor
public class AdvancedTaxController {

    private final HRAComputationService hraService;
    private final Section14AService section14AService;
    private final Section50CValidatorService section50CService;
    private final Relief89Service relief89Service;
    private final DepreciationService depreciationService;
    private final MultiEmployerConsolidationService multiEmployerService;
    private final LTCG112AGrandfatheringService ltcgGrandfatheringService;
    private final EPFTaxationService epfService;
    private final ClubbingService clubbingService;
    private final FOTradingService foTradingService;
    private final BreakEvenAnalysisService breakEvenService;

    @PostMapping("/hra")
    public ResponseEntity<HRAComputationService.HRAResult> computeHRA(
            @RequestBody HRAComputationService.HRAInput input) {
        log.info("Computing HRA exemption");
        return ResponseEntity.ok(hraService.computeHRAExemption(input));
    }

    @PostMapping("/section14a")
    public ResponseEntity<Section14AService.Section14AResult> computeSection14A(
            @RequestBody Section14AService.Section14AInput input) {
        log.info("Computing Section 14A disallowance");
        return ResponseEntity.ok(section14AService.calculateDisallowance(input));
    }

    @PostMapping("/section50c")
    public ResponseEntity<Section50CValidatorService.Section50CResult> validateSection50C(
            @RequestBody Section50CValidatorService.Section50CInput input) {
        log.info("Validating Section 50C");
        return ResponseEntity.ok(section50CService.validate(input));
    }

    @PostMapping("/relief89")
    public ResponseEntity<Relief89Service.Relief89Result> computeRelief89(
            @RequestBody Relief89Service.Relief89Input input) {
        log.info("Computing Relief under Section 89");
        return ResponseEntity.ok(relief89Service.calculateRelief89(input));
    }

    @PostMapping("/depreciation")
    public ResponseEntity<DepreciationService.DepreciationResult> computeDepreciation(
            @RequestBody DepreciationService.DepreciationInput input) {
        log.info("Computing depreciation");
        return ResponseEntity.ok(depreciationService.calculateDepreciation(input));
    }

    @PostMapping("/multi-employer")
    public ResponseEntity<MultiEmployerConsolidationService.ConsolidatedSalary> consolidateMultiEmployer(
            @RequestBody List<MultiEmployerConsolidationService.EmployerData> employers) {
        log.info("Consolidating multi-employer data");
        return ResponseEntity.ok(multiEmployerService.consolidateMultipleEmployers(employers));
    }

    @PostMapping("/ltcg-grandfathering")
    public ResponseEntity<LTCG112AGrandfatheringService.GrandfatheringResult> computeLTCGGrandfathering(
            @RequestBody LTCG112AGrandfatheringService.GrandfatheringInput input) {
        log.info("Computing LTCG grandfathering");
        return ResponseEntity.ok(ltcgGrandfatheringService.computeCostBasis(input));
    }

    @PostMapping("/epf-taxation")
    public ResponseEntity<EPFTaxationService.EPFTaxationResult> computeEPFTaxation(
            @RequestBody EPFTaxationService.EPFTaxationInput input) {
        log.info("Computing EPF taxation");
        return ResponseEntity.ok(epfService.calculateTaxableEPFInterest(input));
    }

    @PostMapping("/clubbing/minor-child")
    public ResponseEntity<ClubbingService.ClubbingResult> computeMinorChildClubbing(
            @RequestBody ClubbingService.MinorChildInput input) {
        log.info("Computing minor child clubbing");
        return ResponseEntity.ok(clubbingService.calculateMinorChildClubbing(
            input.getChildIncome(), 
            input.getNumberOfMinorChildren(),
            input.getParent1Income(),
            input.getParent2Income()
        ));
    }

    @PostMapping("/clubbing/spouse")
    public ResponseEntity<ClubbingService.ClubbingResult> computeSpouseClubbing(
            @RequestBody ClubbingService.SpouseInput input) {
        log.info("Computing spouse clubbing");
        return ResponseEntity.ok(clubbingService.calculateSpouseClubbing(
            input.getSpouseIncome(),
            input.getTransferType(),
            input.isAdequateConsideration()
        ));
    }

    @PostMapping("/fo-trading")
    public ResponseEntity<FOTradingService.FOTradingResult> computeFOTrading(
            @RequestBody FOTradingService.FOTradingInput input) {
        log.info("Computing F&O trading income");
        return ResponseEntity.ok(foTradingService.calculateFOIncome(input));
    }

    @PostMapping("/break-even")
    public ResponseEntity<BreakEvenAnalysisService.BreakEvenResult> analyzeBreakEven(
            @RequestBody BreakEvenAnalysisService.BreakEvenInput input) {
        log.info("Analyzing break-even");
        return ResponseEntity.ok(breakEvenService.analyzeBreakEven(
            input.getGrossIncome(),
            input.getCurrentDeductions()
        ));
    }

    @GetMapping("/depreciation/rates")
    public ResponseEntity<java.util.Map<String, Double>> getDepreciationRates() {
        log.info("Fetching depreciation rates");
        java.util.Map<String, Double> rates = new java.util.HashMap<>();
        rates.put("BUILDING_RCC_RESIDENTIAL", 5.0);
        rates.put("BUILDING_RCC_NON_RESIDENTIAL", 10.0);
        rates.put("FURNITURE_FITTINGS", 10.0);
        rates.put("PLANT_MACHINERY_GENERAL", 15.0);
        rates.put("COMPUTERS_PERIPHERALS", 40.0);
        rates.put("MOTOR_CAR_GENERAL", 15.0);
        rates.put("MOTOR_BUS_LORRY_HIRE", 30.0);
        return ResponseEntity.ok(rates);
    }
}
