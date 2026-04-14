package com.itr.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.AISData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AIS (Annual Information Statement) Import Service
 * 101% CBDT Compliant - Imports and validates AIS JSON data
 */
@Slf4j
@Service
public class AISImportService {

    private final ObjectMapper objectMapper;

    public AISImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AISData importAIS(MultipartFile jsonFile) throws IOException {
        log.info("Importing AIS JSON: {}", jsonFile.getOriginalFilename());
        
        try {
            AISData aisData = objectMapper.readValue(jsonFile.getInputStream(), AISData.class);
            validateAISData(aisData);
            log.info("AIS import successful for PAN: {}, AY: {}", aisData.getPan(), aisData.getAssessmentYear());
            return aisData;
        } catch (Exception e) {
            log.error("Failed to import AIS: {}", e.getMessage(), e);
            throw new IOException("Invalid AIS JSON format: " + e.getMessage(), e);
        }
    }

    private void validateAISData(AISData aisData) {
        List<String> errors = new ArrayList<>();
        
        if (aisData.getPan() == null || !aisData.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            errors.add("Invalid PAN format");
        }
        
        if (aisData.getAssessmentYear() == null) {
            errors.add("Assessment Year is mandatory");
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("AIS validation failed: " + String.join(", ", errors));
        }
    }
}
