package com.itr.service.integration;

import com.itr.dto.TISData;
import com.itr.util.ITDPdfDecryptor;
import com.itr.util.PIIMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TIS (Tax Information Statement) import service
 * TIS shows reconciled/verified amounts - use as "source of truth"
 * 
 * TIS is password-protected: {pan_lowercase}{ddmmyyyy}
 * Example: PAN=ACUPG3482G, DOB=14/06/1974 → Password = acupg3482g14061974
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TISImportService {

    private final ITDPdfDecryptor pdfDecryptor;
    private final PIIMaskingUtil piiMasking;

    /**
     * Import TIS PDF (encrypted with PAN + DOB password)
     */
    public TISData importTIS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String pdfText = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        
        log.info("Starting TIS parsing for PAN: {}", piiMasking.maskPAN(pan));
        
        TISData data = parseTIS(pdfText);
        
        log.info("TIS import complete: Dividend={}, Interest={}, Securities Sale={}, Salary={}", 
                data.getDividendIncome(), data.getInterestFromDeposit(), 
                data.getSecuritiesSaleConsideration(), data.getSalaryAmount());
        
        return data;
    }

    /**
     * Parse TIS document and extract verified incomes
     */
    private TISData parseTIS(String pdfText) {
        TISData data = TISData.builder()
                .dividendIncome(0)
                .interestFromDeposit(0)
                .securitiesSaleConsideration(0)
                .securitiesPurchaseAmount(0)
                .interestOnSecurities(0)
                .salaryAmount(0)
                .rentIncome(0)
                .build();
        
        // Find the Information Category section (TIS summary)
        int summaryStart = -1;
        for (String marker : new String[]{"INFORMATION CATEGORY", "Information Category", "INCOME DETAILS"}) {
            summaryStart = pdfText.indexOf(marker);
            if (summaryStart != -1) break;
        }
        
        if (summaryStart == -1) {
            log.warn("TIS summary section not found");
            // Try to extract individual sections
            extractFromIndividualSections(pdfText, data);
            return data;
        }
        
        // Find end of summary (next section or 5000 chars max)
        int summaryEnd = pdfText.indexOf("The information details", summaryStart);
        if (summaryEnd == -1) summaryEnd = textIndexOfAny(pdfText, new String[]{"PART-", "PART ", "The details"}, summaryStart);
        if (summaryEnd == -1) summaryEnd = Math.min(summaryStart + 8000, pdfText.length());
        
        String summaryText = pdfText.substring(summaryStart, summaryEnd);
        
        log.debug("TIS Summary text (first 1000 chars): {}", summaryText.substring(0, Math.min(1000, summaryText.length())));
        
        // Pattern: SR NO | Category | Processed by System | Accepted by Taxpayer/Source
        // TIS shows "Accepted by Taxpayer" column as the authoritative value
        Pattern summaryRow = Pattern.compile(
            "(\\d+)\\s+([A-Za-z][A-Za-z\\s,/()]+?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = summaryRow.matcher(summaryText);
        while (m.find()) {
            try {
                String category = m.group(2).trim().toLowerCase();
                String acceptedAmount = m.group(4).trim();
                long accepted = parseAmountLong(acceptedAmount);
                
                log.debug("TIS Category: '{}' = {} (Accepted)", category, accepted);
                
                // Match income categories
                if (category.contains("dividend")) {
                    data.setDividendIncome(accepted);
                    log.info("TIS Dividend Income: {}", accepted);
                } else if (category.contains("interest from deposit") || category.contains("interest on deposits")) {
                    data.setInterestFromDeposit(accepted);
                    log.info("TIS Interest from Deposit: {}", accepted);
                } else if (category.contains("sale of securities") || category.contains("securities sale")) {
                    data.setSecuritiesSaleConsideration(accepted);
                    log.info("TIS Securities Sale: {}", accepted);
                } else if (category.contains("purchase of securities") || category.contains("securities purchase")) {
                    data.setSecuritiesPurchaseAmount(accepted);
                } else if (category.contains("interest on securities")) {
                    data.setInterestOnSecurities(accepted);
                } else if (category.contains("salary")) {
                    data.setSalaryAmount(accepted);
                    log.info("TIS Salary: {}", accepted);
                } else if (category.contains("rent") || category.contains("property")) {
                    data.setRentIncome(accepted);
                    log.info("TIS Rent Income: {}", accepted);
                }
            } catch (Exception e) {
                log.debug("Error parsing TIS row: {}", e.getMessage());
            }
        }
        
        // If no data found in summary, try individual sections
        if (data.getDividendIncome() == 0 && data.getInterestFromDeposit() == 0) {
            extractFromIndividualSections(pdfText, data);
        }
        
        return data;
    }
    
    /**
     * Extract data from individual section headers if summary not found
     */
    private void extractFromIndividualSections(String text, TISData data) {
        log.info("Extracting TIS data from individual sections");
        
        // Salary (Section 192)
        extractAmount(text, "SALARY", "Salary", amount -> data.setSalaryAmount(amount));
        
        // Interest
        extractAmount(text, "194A", "Interest", amount -> data.setInterestFromDeposit(amount));
        extractAmount(text, "Interest on Securities", null, amount -> data.setInterestOnSecurities(amount));
        
        // Dividends  
        extractAmount(text, "DIVIDEND", "Dividend", amount -> data.setDividendIncome(amount));
        
        // Securities
        extractAmount(text, "Sale of Securities", null, amount -> data.setSecuritiesSaleConsideration(amount));
        
        // Rent
        extractAmount(text, "194I", "Rent", amount -> data.setRentIncome(amount));
    }
    
    /**
     * Helper to extract amounts from specific section keywords
     */
    private void extractAmount(String text, String sectionKeyword, String label, java.util.function.Consumer<Long> setter) {
        int pos = text.indexOf(sectionKeyword);
        if (pos == -1) return;
        
        // Look for amount after keyword
        int searchStart = pos;
        int searchEnd = Math.min(pos + 500, text.length());
        String section = text.substring(searchStart, searchEnd);
        
        Pattern amountPattern = Pattern.compile("[₹]?\\s*([\\d,]+(?:\\.\\d{1,2})?)");
        Matcher m = amountPattern.matcher(section);
        if (m.find()) {
            long amount = parseAmountLong(m.group(1));
            if (amount > 0) {
                setter.accept(amount);
                log.debug("TIS {} ({}) = {}", label != null ? label : sectionKeyword, sectionKeyword, amount);
            }
        }
    }
    
    private int textIndexOfAny(String text, String[] keywords, int fromIndex) {
        int result = -1;
        for (String kw : keywords) {
            int pos = text.indexOf(kw, fromIndex);
            if (pos != -1 && (result == -1 || pos < result)) {
                result = pos;
            }
        }
        return result;
    }

    private long parseAmountLong(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return 0;
        String cleaned = amountStr.replaceAll("[,\\s₹]", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            try {
                return (long) Double.parseDouble(cleaned);
            } catch (NumberFormatException e2) {
                log.warn("Failed to parse amount: {}", amountStr);
                return 0;
            }
        }
    }
}
