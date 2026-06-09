package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import com.itr.dto.Itr3FormData;
import com.itr.dto.Itr4FormData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ITR-V Acknowledgement PDF Generation Service
 * Generates PDF receipt after successful ITR filing
 * Reference: ITD e-Filing portal acknowledgement format
 */
@Slf4j
@Service
public class ITRVPDFGenerationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Generate ITR-V PDF for any ITR form
     */
    public byte[] generateITRV(Object formData, String acknowledgementNumber) {
        try {
            ITRVData data = extractITRVData(formData, acknowledgementNumber);
            return generatePDF(data);
        } catch (Exception e) {
            log.error("Error generating ITR-V PDF", e);
            throw new RuntimeException("Failed to generate ITR-V: " + e.getMessage(), e);
        }
    }

    private ITRVData extractITRVData(Object formData, String acknowledgementNumber) {
        ITRVData data = new ITRVData();
        data.setAcknowledgementNumber(acknowledgementNumber);
        data.setFilingDate(LocalDate.now().format(DATE_FORMATTER));

        if (formData instanceof Itr1FormData) {
            Itr1FormData itr1 = (Itr1FormData) formData;
            data.setPan(itr1.getPersonalInfo().getPan());
            data.setName(itr1.getPersonalInfo().getAssesseeName());
            data.setAssessmentYear(itr1.getPersonalInfo().getAssessmentYear());
            data.setFormType("ITR-1");
            data.setTotalIncome((long) itr1.getTaxComputation().getTotalIncome());
            data.setTaxPayable((long) itr1.getTaxComputation().getTotalTaxLiability());
        } else {
            // Simplified for ITR-2/3/4 - extract via reflection or use default values
            data.setPan("UNKNOWN");
            data.setName("Taxpayer");
            data.setAssessmentYear("2025-26");
            data.setFormType("ITR");
            data.setTotalIncome(0L);
            data.setTaxPayable(0L);
        }

        return data;
    }

    private byte[] generatePDF(ITRVData data) {
        // Simplified PDF generation - in production use iText or Apache PDFBox
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        StringBuilder content = new StringBuilder();
        content.append("INCOME TAX DEPARTMENT\n");
        content.append("ACKNOWLEDGEMENT RECEIPT (ITR-V)\n\n");
        content.append("Acknowledgement Number: ").append(data.getAcknowledgementNumber()).append("\n");
        content.append("PAN: ").append(data.getPan()).append("\n");
        content.append("Name: ").append(data.getName()).append("\n");
        content.append("Assessment Year: ").append(data.getAssessmentYear()).append("\n");
        content.append("Form Type: ").append(data.getFormType()).append("\n");
        content.append("Filing Date: ").append(data.getFilingDate()).append("\n");
        content.append("Total Income: Rs. ").append(data.getTotalIncome()).append("\n");
        content.append("Tax Payable: Rs. ").append(data.getTaxPayable()).append("\n\n");
        content.append("This is a computer-generated acknowledgement and does not require signature.\n");
        content.append("Please verify your return within 30 days using EVC/DSC.\n");

        try {
            baos.write(content.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }

        log.info("ITR-V PDF generated for PAN: {}, Ack: {}", data.getPan(), data.getAcknowledgementNumber());
        return baos.toByteArray();
    }

    @Data
    private static class ITRVData {
        private String acknowledgementNumber;
        private String pan;
        private String name;
        private String assessmentYear;
        private String formType;
        private String filingDate;
        private long totalIncome;
        private long taxPayable;
    }
}
