package com.itr.controller;

import com.itr.dto.AISData;
import com.itr.dto.Form16Data;
import com.itr.dto.Form26ASData;
import com.itr.dto.ITDPrefillData;
import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import com.itr.service.integration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Integration Controller - 101% CBDT Compliant
 * Handles Form 16 PDF, AIS, 26AS, and ITD Prefill imports
 */
@Slf4j
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private final Form16ExtractionService form16Service;
    private final AISImportService aisService;
    private final Form26ASImportService form26ASService;
    private final ITDPrefillImportService prefillService;
    private final AutoPopulationService autoPopulationService;

    public IntegrationController(Form16ExtractionService form16Service,
                                AISImportService aisService,
                                Form26ASImportService form26ASService,
                                ITDPrefillImportService prefillService,
                                AutoPopulationService autoPopulationService) {
        this.form16Service = form16Service;
        this.aisService = aisService;
        this.form26ASService = form26ASService;
        this.prefillService = prefillService;
        this.autoPopulationService = autoPopulationService;
    }

    @PostMapping("/form16/extract")
    public ResponseEntity<Form16Data> extractForm16(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received Form 16 PDF upload: {}", file.getOriginalFilename());
            Form16Data data = form16Service.extractForm16(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Form 16 extraction failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ais/import")
    public ResponseEntity<AISData> importAIS(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received AIS JSON upload: {}", file.getOriginalFilename());
            AISData data = aisService.importAIS(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("AIS import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/26as/import")
    public ResponseEntity<Form26ASData> import26AS(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received Form 26AS JSON upload: {}", file.getOriginalFilename());
            Form26ASData data = form26ASService.import26AS(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Form 26AS import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/prefill/import")
    public ResponseEntity<ITDPrefillData> importPrefill(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received ITD Prefill JSON upload: {}", file.getOriginalFilename());
            ITDPrefillData data = prefillService.importPrefillData(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("ITD Prefill import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/form16")
    public ResponseEntity<Itr1FormData> autoPopulateFromForm16(
            @RequestBody Itr1FormData formData,
            @RequestParam("form16") Form16Data form16Data) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFromForm16(formData, form16Data);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from Form 16 failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/ais")
    public ResponseEntity<Itr1FormData> autoPopulateFromAIS(
            @RequestBody Itr1FormData formData,
            @RequestParam("ais") AISData aisData) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFromAIS(formData, aisData);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from AIS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/26as")
    public ResponseEntity<Itr1FormData> autoPopulateFrom26AS(
            @RequestBody Itr1FormData formData,
            @RequestParam("data26as") Form26ASData data26AS) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFrom26AS(formData, data26AS);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from Form 26AS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/prefill")
    public ResponseEntity<Itr1FormData> autoPopulateFromPrefill(
            @RequestBody Itr1FormData formData,
            @RequestParam("prefill") ITDPrefillData prefillData) {
        try {
            Itr1FormData populated = prefillService.autoPopulateFromPrefill(formData, prefillData);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from ITD Prefill failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/itr2/ais")
    public ResponseEntity<Itr2FormData> autoPopulateITR2FromAIS(
            @RequestBody Itr2FormData formData,
            @RequestParam("ais") AISData aisData) {
        try {
            Itr2FormData populated = autoPopulationService.autoPopulateITR2FromAIS(formData, aisData);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population ITR-2 from AIS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/itr2/26as")
    public ResponseEntity<Itr2FormData> autoPopulateITR2From26AS(
            @RequestBody Itr2FormData formData,
            @RequestParam("data26as") Form26ASData data26AS) {
        try {
            Itr2FormData populated = autoPopulationService.autoPopulateITR2From26AS(formData, data26AS);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population ITR-2 from Form 26AS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
