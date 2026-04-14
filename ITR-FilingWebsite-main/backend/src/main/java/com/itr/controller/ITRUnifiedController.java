package com.itr.controller;

import com.itr.dto.*;
import com.itr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/clients/{clientId}/itr/{year}")
@RequiredArgsConstructor
public class ITRUnifiedController {

    private final ITDJSONExportService itdJsonExportService;
    private final ITR2JSONExportService itr2JsonExportService;
    private final ITR3JSONExportService itr3JsonExportService;
    private final ITR4JSONExportService itr4JsonExportService;
    private final Itr1FormService itr1FormService;
    private final Itr1ReportService itr1ReportService;
    private final ITRValidationService validationService;

    @GetMapping
    public ResponseEntity<?> getFormData(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String itrType = determineITRType(clientId, year, userId);
        
        switch (itrType) {
            case "ITR-1":
                return ResponseEntity.ok(itr1FormService.getFormData(clientId, year, userId));
            default:
                return ResponseEntity.ok(itr1FormService.getFormData(clientId, year, userId));
        }
    }

    @PutMapping
    public ResponseEntity<?> saveFormData(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody Object formData,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(formData);
    }

    @PostMapping("/compute")
    public ResponseEntity<?> computeForm(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody Itr1FormData formData,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Itr1FormData computed = itr1FormService.computeForm(clientId, year, formData, userId);
        return ResponseEntity.ok(computed);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateForm(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody Itr1FormData formData,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        
        java.util.List<String> errors = validationService.validateItr1Form(formData);
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        // Add warnings for missing optional but recommended fields
        if (formData.getPersonalInfo() != null) {
            if (formData.getPersonalInfo().getEmail() == null) {
                warnings.add("Email is recommended for e-filing communication");
            }
            if (formData.getPersonalInfo().getMobile() == null) {
                warnings.add("Mobile number is recommended for OTP verification");
            }
        }
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("valid", errors.isEmpty());
        response.put("errors", errors);
        response.put("warnings", warnings);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadITRJson(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String itrType = determineITRType(clientId, year, userId);
            String json;
            String filename;

            switch (itrType) {
                case "ITR-1":
                    Itr1FormData itr1Data = itr1FormService.getFormData(clientId, year, userId);
                    json = itdJsonExportService.exportITR1ToITDJson(itr1Data);
                    filename = buildFilename(itr1Data.getPersonalInfo().getAssesseeName(), clientId, year, "ITR1");
                    break;
                case "ITR-2":
                    Itr2FormData itr2Data = getItr2FormData(clientId, year, userId);
                    json = itr2JsonExportService.export(itr2Data);
                    filename = buildFilename(itr2Data.getFirstName(), clientId, year, "ITR2");
                    break;
                case "ITR-3":
                    Itr3FormData itr3Data = getItr3FormData(clientId, year, userId);
                    json = itr3JsonExportService.export(itr3Data);
                    filename = buildFilename("Client", clientId, year, "ITR3");
                    break;
                case "ITR-4":
                    Itr4FormData itr4Data = getItr4FormData(clientId, year, userId);
                    json = itr4JsonExportService.export(itr4Data);
                    filename = buildFilename("Client", clientId, year, "ITR4");
                    break;
                default:
                    return ResponseEntity.badRequest().body("{\"error\":\"Unsupported ITR type\"}".getBytes());
            }

            log.info("Generated CBDT-compliant JSON for client {} year {} type {}", clientId, year, itrType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.getBytes());
        } catch (Exception e) {
            log.error("JSON export failed for client {} year {}", clientId, year, e);
            return ResponseEntity.internalServerError()
                    .body(("{\"error\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    @GetMapping("/download-pdf")
    public ResponseEntity<byte[]> downloadPdfReport(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Itr1FormData formData = itr1FormService.getFormData(clientId, year, userId);
            byte[] pdfBytes = itr1ReportService.generatePdfReport(formData);

            String filename = buildFilename(
                formData.getPersonalInfo().getAssesseeName(), 
                clientId, 
                year, 
                "Report"
            ).replace(".json", ".pdf");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineITRType(Long clientId, String year, Long userId) {
        return "ITR-1";
    }

    private Itr2FormData getItr2FormData(Long clientId, String year, Long userId) {
        return Itr2FormData.builder().build();
    }

    private Itr3FormData getItr3FormData(Long clientId, String year, Long userId) {
        return Itr3FormData.builder().build();
    }

    private Itr4FormData getItr4FormData(Long clientId, String year, Long userId) {
        return Itr4FormData.builder().build();
    }

    private String buildFilename(String name, Long clientId, String year, String itrType) {
        String clientName = name != null 
            ? name.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_")
            : "Client_" + clientId;
        return clientName + "_" + year + "_" + itrType + "_CBDT.json";
    }
}
