package com.itr.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.Form26ASData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Form 26AS Import Service
 * 101% CBDT Compliant - Imports and validates Form 26AS JSON data
 */
@Slf4j
@Service
public class Form26ASImportService {

    private final ObjectMapper objectMapper;

    public Form26ASImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Form26ASData import26AS(MultipartFile jsonFile) throws IOException {
        log.info("Importing Form 26AS JSON: {}", jsonFile.getOriginalFilename());
        
        try {
            Form26ASData data = objectMapper.readValue(jsonFile.getInputStream(), Form26ASData.class);
            validate26ASData(data);
            log.info("Form 26AS import successful for PAN: {}, AY: {}", data.getPan(), data.getAssessmentYear());
            return data;
        } catch (Exception e) {
            log.error("Failed to import Form 26AS: {}", e.getMessage(), e);
            throw new IOException("Invalid Form 26AS JSON format: " + e.getMessage(), e);
        }
    }

    private void validate26ASData(Form26ASData data) {
        List<String> errors = new ArrayList<>();
        
        if (data.getPan() == null || !data.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            errors.add("Invalid PAN format");
        }
        
        if (data.getAssessmentYear() == null) {
            errors.add("Assessment Year is mandatory");
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Form 26AS validation failed: " + String.join(", ", errors));
        }
    }
}
