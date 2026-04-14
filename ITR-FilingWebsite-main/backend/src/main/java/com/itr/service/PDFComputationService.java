package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PDFComputationService {
    
    public byte[] generateITR1PDF(Long clientId, String year) {
        log.warn("PDFComputationService.generateITR1PDF - stub implementation");
        return new byte[0];
    }
    
    public byte[] generatePDF(Object formData, String itrType) {
        log.warn("PDFComputationService.generatePDF - stub implementation for ITR type: {}", itrType);
        return new byte[0];
    }
}
