package com.itr.service.integration;

import com.itr.dto.Form26ASData;
import com.itr.dto.Form26ASData.*;
import com.itr.util.ITDPdfDecryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Form 26AS (TRACES) import service with part-wise extraction
 * Uses SUMMARY ROWS (not detail rows) to avoid G-entry double-counting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class Form26ASImportService {

    private final ITDPdfDecryptor pdfDecryptor;

    public Form26ASData import26AS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String pdfText = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        
        Form26ASData data = Form26ASData.builder()
                .partIEntries(new ArrayList<>())
                .partIVEntries(new ArrayList<>())
                .partVIIEntries(new ArrayList<>())
                .partXEntries(new ArrayList<>())
                .build();

        data.setPartIEntries(parsePartI(pdfText));
        data.setPartIVEntries(parsePartIV(pdfText));
        data.setPartVIIEntries(parsePartVII(pdfText));
        
        log.info("26AS import complete: {} Part-I entries, {} Part-IV entries", 
                data.getPartIEntries().size(), data.getPartIVEntries().size());
        
        return data;
    }

    /**
     * PART-I: TDS on Salary and Other Income
     * Extract SUMMARY ROWS only (deductor name + TAN + totals)
     */
    private List<TDSEntry26AS> parsePartI(String pdfText) {
        List<TDSEntry26AS> entries = new ArrayList<>();

        int partIStart = -1;
        for (String m : new String[]{"PART-I -", "PART-I-", "PART-I ", "Part I", "PART-I"}) {
            partIStart = pdfText.indexOf(m);
            if (partIStart != -1) break;
        }
        if (partIStart == -1) { log.warn("PART-I not found in 26AS"); return entries; }

        int partIIStart = pdfText.indexOf("PART-II", partIStart + 10);
        if (partIIStart == -1) partIIStart = pdfText.indexOf("Part II", partIStart + 10);

        String partIText = pdfText.substring(partIStart,
            partIIStart != -1 ? partIIStart : Math.min(partIStart + 15000, pdfText.length()));

        // Flexible pattern: Name (2+ spaces) TAN (10 chars) amounts
        Pattern pat = Pattern.compile(
            "([A-Z][A-Z0-9 &.,'/()-]{3,60}?)\\s{2,}([A-Z]{4}\\d{5}[A-Z])\\s+" +
            "([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)"
        );

        Matcher m = pat.matcher(partIText);
        while (m.find()) {
            String name = m.group(1).trim();
            String tan  = m.group(2);
            BigDecimal amt = parseAmount(m.group(3));
            BigDecimal ded = parseAmount(m.group(4));
            BigDecimal dep = parseAmount(m.group(5));

            if (amt.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Avoid duplicate TAN
            boolean dup = entries.stream().anyMatch(e -> e.getTan().equals(tan));
            if (dup) continue;

            int ctxStart = Math.max(0, m.start() - 500);
            String ctx = partIText.substring(ctxStart, m.end());
            String section = detectSection(ctx);

            entries.add(TDSEntry26AS.builder()
                .deductorName(name).tan(tan).section(section)
                .amountPaid(amt).taxDeducted(ded).taxDeposited(dep).build());

            log.info("26AS Entry: {} TAN:{} Sec:{} Amt:{} TDS:{}", name, tan, section, amt, dep);
        }

        if (entries.isEmpty()) log.warn("No 26AS entries extracted — PDF format may differ");
        return entries;
    }

    private String detectSection(String ctx) {
        if (ctx.contains("192A")) return "192A";
        if (ctx.contains("194A")) return "194A";
        if (ctx.contains("194IB")) return "194IB";
        if (ctx.contains("194IA")) return "194IA";
        if (ctx.contains("194I")) return "194I";
        if (ctx.contains("194C")) return "194C";
        if (ctx.contains("194D")) return "194D";
        if (ctx.contains("194J")) return "194J";
        if (ctx.contains("194H")) return "194H";
        if (ctx.contains("194")) return "194";
        if (ctx.contains("192")) return "192";
        return "OTHER";
    }

    /**
     * PART-IV: TDS on property sale (Section 194IA - Seller side)
     */
    private List<PropertyTDS26AS> parsePartIV(String pdfText) {
        List<PropertyTDS26AS> entries = new ArrayList<>();
        
        int partIVStart = pdfText.indexOf("PART-IV");
        if (partIVStart == -1) partIVStart = pdfText.indexOf("Part IV");
        if (partIVStart == -1) return entries;
        
        int partIVEnd = pdfText.indexOf("PART-V", partIVStart);
        if (partIVEnd == -1) partIVEnd = pdfText.indexOf("Part V", partIVStart);
        
        String partIVText = pdfText.substring(partIVStart,
                partIVEnd != -1 ? partIVEnd : Math.min(partIVStart + 5000, pdfText.length()));

        if (partIVText.contains("No Transactions Present") || 
            partIVText.contains("No transactions")) return entries;

        // Pattern: Ack No + Buyer Name + Buyer PAN + Date + Transaction Amt + TDS
        Pattern propPattern = Pattern.compile(
            "(\\d{15})\\s+([A-Z ]{5,50})\\s+([A-Z]{5}\\d{4}[A-Z])\\s+" +
            "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})"
        );
        
        Matcher m = propPattern.matcher(partIVText);
        while (m.find()) {
            PropertyTDS26AS entry = PropertyTDS26AS.builder()
                    .acknowledgementNo(m.group(1))
                    .buyerName(m.group(2).trim())
                    .buyerPAN(m.group(3))
                    .transactionDate(parseDate(m.group(4)))
                    .transactionAmount(parseAmount(m.group(5)))
                    .tdsDeposited(parseAmount(m.group(6)))
                    .build();
            entries.add(entry);
        }
        
        return entries;
    }

    /**
     * PART-VII: Refunds paid by Income Tax Department
     * Interest on refund (Section 244A) is taxable income
     */
    private List<RefundEntry26AS> parsePartVII(String pdfText) {
        List<RefundEntry26AS> entries = new ArrayList<>();
        
        int partVIIStart = pdfText.indexOf("PART-VII");
        if (partVIIStart == -1) partVIIStart = pdfText.indexOf("Part VII");
        if (partVIIStart == -1) return entries;
        
        int partVIIEnd = pdfText.indexOf("PART-VIII", partVIIStart);
        if (partVIIEnd == -1) partVIIEnd = pdfText.indexOf("Part VIII", partVIIStart);
        
        String partVIIText = pdfText.substring(partVIIStart,
                partVIIEnd != -1 ? partVIIEnd : Math.min(partVIIStart + 3000, pdfText.length()));

        if (partVIIText.contains("No Transactions Present")) return entries;

        // Pattern: AY + Refund Amount + Interest + Date
        Pattern refundPattern = Pattern.compile(
            "(\\d{4}-\\d{2})\\s+([\\d,]+)\\s+([\\d,]+)\\s+(\\d{2}-[A-Za-z]{3}-\\d{4})"
        );
        
        Matcher m = refundPattern.matcher(partVIIText);
        while (m.find()) {
            RefundEntry26AS entry = RefundEntry26AS.builder()
                    .assessmentYear(m.group(1))
                    .refundAmount(parseAmount(m.group(2)))
                    .interestAmount(parseAmount(m.group(3)))
                    .refundDate(parseDate(m.group(4)))
                    .build();
            entries.add(entry);
        }
        
        return entries;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return BigDecimal.ZERO;
        String cleaned = amountStr.replaceAll("[,\\s]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return LocalDate.now();
        }
    }
}
