package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Itr1ReportService {
    
    public byte[] generatePDF(Long clientId, String year) {
        log.warn("Itr1ReportService.generatePDF - stub implementation");
        return new byte[0];
    }
    
    public byte[] generateExcelReport(Itr1FormData formData) {
        log.warn("Itr1ReportService.generateExcelReport - stub implementation");
        return new byte[0];
    }
    
    public byte[] generatePdfReport(Itr1FormData formData) {
        log.warn("Itr1ReportService.generatePdfReport - stub implementation");
        return new byte[0];
    }
}
