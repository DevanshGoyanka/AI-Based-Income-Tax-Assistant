package com.itr.service;

import com.itr.dto.Form16PartAData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Form 16 Part A Parser - TRACES TDS Certificate
 * Extracts structured data from PDF using PDFBox text extraction
 */
@Slf4j
@Service
public class Form16PartAParser {

    public Form16PartAData parse(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            
            if (text == null || text.trim().length() < 100) {
                throw new IOException("PDF appears to be empty or scanned - text extraction failed");
            }
            
            return parseFromText(text);
        }
    }

    private Form16PartAData parseFromText(String text) {
        Form16PartAData data = new Form16PartAData();

        data.setTanOfEmployer(extractRegex(text, "TAN.*?Employer[:\\s]+([A-Z]{4}[0-9]{5}[A-Z])", 1));
        data.setPanOfEmployee(extractRegex(text, "PAN.*?Employee[:\\s]+([A-Z]{5}[0-9]{4}[A-Z])", 1));
        data.setEmployerName(extractRegex(text, "Name.*?Employer[:\\s]+([A-Za-z0-9 &.,()-]{1,100})", 1));
        data.setAssessmentYear(extractRegex(text, "Assessment Year[:\\s]+(20[0-9]{2}-[0-9]{2})", 1));
        data.setFinancialYear(extractRegex(text, "Financial Year[:\\s]+(20[0-9]{2}-[0-9]{2})", 1));
        
        String salaryRaw = extractRegex(text, "(?:Aggregate|Total).*?salary.*?paid[\\s:]+(Rs\\.?\\s?)?([0-9,]+)", 2);
        data.setAggregateSalaryPaid(parseCurrency(salaryRaw));
        
        String tdsRaw = extractRegex(text, "(?:Aggregate|Total).*?TDS.*?deposited[\\s:]+(Rs\\.?\\s?)?([0-9,]+)", 2);
        data.setAggregateTdsDeposited(parseCurrency(tdsRaw));
        
        data.setCertificateNumber(extractRegex(text, "Certificate No\\.?[:\\s]+([A-Z0-9]{10,20})", 1));
        data.setAcknowledgementNumberTraces(extractRegex(text, "Acknowledgement.*?No[:\\s]+([0-9]{15})", 1));
        data.setPeriodFrom(extractRegex(text, "From[:\\s]+([0-3][0-9]/[0-1][0-9]/20[0-9]{2})", 1));
        data.setPeriodTo(extractRegex(text, "To[:\\s]+([0-3][0-9]/[0-1][0-9]/20[0-9]{2})", 1));
        
        data.setQuarterlyBreakup(parseQuarterlyTable(text));

        log.info("Parsed Form 16 Part A: PAN={}, TAN={}, TDS={}", 
                 data.getPanOfEmployee(), data.getTanOfEmployer(), data.getAggregateTdsDeposited());
        
        return data;
    }

    private List<Form16PartAData.QuarterlyTDS> parseQuarterlyTable(String text) {
        List<Form16PartAData.QuarterlyTDS> result = new ArrayList<>();
        
        Pattern rowPattern = Pattern.compile(
            "(Q[1-4])[\\s|]+(\\d{2}/\\d{2}/20\\d{2})[\\s|]+(\\d{2}/\\d{2}/20\\d{2})[\\s|]+(\\d{7})[\\s|]+(\\d+)[\\s|]+([0-9,]+)");
        Matcher m = rowPattern.matcher(text);
        
        while (m.find()) {
            Form16PartAData.QuarterlyTDS q = new Form16PartAData.QuarterlyTDS();
            q.setQuarter(m.group(1));
            q.setDateOfPaymentCredit(m.group(2));
            q.setDateOfTdsDeposit(m.group(3));
            q.setBsrCode(m.group(4));
            q.setChallanSerialNumber(m.group(5));
            q.setAmountOfTaxDeposited(parseCurrency(m.group(6)));
            result.add(q);
        }
        
        return result;
    }

    private String extractRegex(String text, String pattern, int group) {
        try {
            Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
            if (m.find() && m.groupCount() >= group) {
                return m.group(group).trim();
            }
        } catch (Exception e) {
            log.debug("Regex extraction failed for pattern: {}", pattern);
        }
        return null;
    }

    private long parseCurrency(String s) {
        if (s == null || s.isEmpty()) return 0L;
        return Long.parseLong(s.replaceAll("[^0-9]", ""));
    }
}
