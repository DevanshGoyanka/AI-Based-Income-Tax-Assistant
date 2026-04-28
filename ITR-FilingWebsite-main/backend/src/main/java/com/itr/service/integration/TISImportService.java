package com.itr.service.integration;

import com.itr.dto.TISData;
import com.itr.util.ITDPdfDecryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TIS (Taxpayer Information Summary) import service
 * TIS shows reconciled/verified amounts - use as "source of truth"
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TISImportService {

    private final ITDPdfDecryptor pdfDecryptor;

    public TISData importTIS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String pdfText = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        
        TISData data = parseTIS(pdfText);
        
        log.info("TIS import complete: Dividend={}, Interest={}, Securities Sale={}", 
                data.getDividendIncome(), data.getInterestFromDeposit(), 
                data.getSecuritiesSaleConsideration());
        
        return data;
    }

    private TISData parseTIS(String pdfText) {
        TISData data = new TISData();
        
        int summaryStart = pdfText.indexOf("INFORMATION CATEGORY");
        if (summaryStart == -1) summaryStart = pdfText.indexOf("Information Category");
        if (summaryStart == -1) return data;
        
        int summaryEnd = pdfText.indexOf("The information details", summaryStart);
        if (summaryEnd == -1) summaryEnd = Math.min(summaryStart + 3000, pdfText.length());
        
        String summaryText = pdfText.substring(summaryStart, summaryEnd);
        
        // Pattern: SR NO | Category | Processed by System | Accepted by Taxpayer/Source
        Pattern summaryRow = Pattern.compile(
            "(\\d+)\\s+([A-Za-z][A-Za-z\\s,/()]+?)\\s+([\\d,]+)\\s+([\\d,]+)"
        );
        
        Matcher m = summaryRow.matcher(summaryText);
        while (m.find()) {
            String category = m.group(2).trim().toLowerCase();
            long acceptedAmount = parseAmountLong(m.group(4)); // Use "Accepted" column
            
            if (category.contains("dividend")) {
                data.setDividendIncome(acceptedAmount);
            } else if (category.contains("interest from deposit")) {
                data.setInterestFromDeposit(acceptedAmount);
            } else if (category.contains("sale of securities")) {
                data.setSecuritiesSaleConsideration(acceptedAmount);
            } else if (category.contains("purchase of securities")) {
                data.setSecuritiesPurchaseAmount(acceptedAmount);
            } else if (category.contains("interest on securities")) {
                data.setInterestOnSecurities(acceptedAmount);
            } else if (category.contains("salary")) {
                data.setSalaryAmount(acceptedAmount);
            } else if (category.contains("rent")) {
                data.setRentIncome(acceptedAmount);
            }
        }
        
        return data;
    }

    private long parseAmountLong(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return 0;
        String cleaned = amountStr.replaceAll("[,\\s]", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return 0;
        }
    }
}
