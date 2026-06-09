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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class Form26ASImportService {

    private final ITDPdfDecryptor pdfDecryptor;

    public Form26ASData import26AS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String pdfText = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        log.info("Starting 26AS parsing for PAN: {}", pan);
        
        Form26ASData data = Form26ASData.builder()
                .assessePAN(pan)
                .tdsOnInterest(new ArrayList<>())
                .tdsOnContractor(new ArrayList<>())
                .tdsOnProfessional(new ArrayList<>())
                .tdsOnCommission(new ArrayList<>())
                .tdsOnRent(new ArrayList<>())
                .tdsOnInsurance(new ArrayList<>())
                .tdsOnProperty(new ArrayList<>())
                .tdsOnOther(new ArrayList<>())
                .build();
        
        data.setAssesseName(extractName(pdfText, pan));
        
        // Split into non-empty lines
        List<String> lines = new ArrayList<>();
        for (String line : pdfText.split("\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) lines.add(t);
        }
        
        // Find PART-I
        int startIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("PART-I")) { startIdx = i; break; }
        }
        if (startIdx == -1) { log.warn("PART-I not found"); return data; }
        
        // TWO-PASS: First collect all summary entries (name+TAN+amount)
        // Then for each summary, scan forward for the FIRST detail line containing a section code
        // to determine which section this deductor maps to
        
        // PASS 1: Collect all summary entries
        List<SummaryEntry> summaries = new ArrayList<>();
        
        for (int i = startIdx; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Skip headers and legends
            if (line.contains("Sr. No.") || line.contains("Legend") || 
                line.contains("PART-") || line.contains("Notes for") ||
                line.contains("Assessee PAN") || line.contains("Glossary") ||
                line.contains("Abbreviation") || line.contains("Minor Head") ||
                line.contains("Major Head") || line.contains("Legend Description")) {
                continue;
            }
            
            // Check for TAN in line
            Pattern tanPat = Pattern.compile("[A-Z]{4}\\d{5}[A-Z]");
            Matcher tanMatcher = tanPat.matcher(line);
            
            if (!tanMatcher.find()) continue;
            
            String tan = tanMatcher.group();
            int tanIdx = tanMatcher.start();
            
            // Must have a number at beginning (Sr.No) and text before TAN (name)
            if (!Character.isDigit(line.charAt(0))) continue;
            
            String name = line.substring(line.indexOf(' ') + 1, tanIdx).trim();
            if (name.length() < 3) continue;
            
            // Extract amounts after TAN
            String afterTan = line.substring(tanMatcher.end()).trim();
            String[] parts = afterTan.split("\\s+");
            
            BigDecimal amount = parseAmount(parts.length > 0 ? parts[0] : null);
            BigDecimal tax = parseAmount(parts.length > 1 ? parts[1] : null);
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;
            
            // PASS 2: Scan forward (up to 15 lines) to find first section code
            String section = "OTHER";
            for (int j = i + 1; j < Math.min(i + 15, lines.size()); j++) {
                String next = lines.get(j);
                // Look for pattern: number, section code (like 194, 192, 194A), date
                // but skip if it contains "Sr." or "Section" header
                if (next.contains("Sr.") || next.contains("Section1") || next.contains("Section Transaction")) continue;
                
                // Try to match: number followed by section code (194, 192, 194A, 194C, etc.)
                Pattern secPat = Pattern.compile("^\\d+\\s+(\\d{3,5})\\s+");
                Matcher secMatcher = secPat.matcher(next);
                if (secMatcher.find()) {
                    String code = secMatcher.group(1);
                    // Must start with 192 or 194 or 195 or 196 or 206
                    if (code.startsWith("192") || code.startsWith("194") || 
                        code.startsWith("195") || code.startsWith("196") || 
                        code.startsWith("206") || code.startsWith("193")) {
                        section = code;
                        log.debug("  Found section {} for deductor '{}' in line: {}", code, name, next.substring(0, Math.min(50, next.length())));
                        break;
                    }
                }
            }
            
            summaries.add(new SummaryEntry(name, tan, amount, tax, section));
            log.info("Summary: {} TAN:{} Sec:{} Amt:₹{} Tax:₹{}", name, tan, section, amount, tax);
        }
        
        log.info("Total: {} deductor summary entries", summaries.size());
        
        // Classify each summary entry
        for (SummaryEntry se : summaries) {
            TDSOtherThanSalary entry = TDSOtherThanSalary.builder()
                .deductorName(se.name)
                .deductorTAN(se.tan)
                .section(se.section)
                .amountPaid(se.amount)
                .taxDeducted(se.tax != null ? se.tax : BigDecimal.ZERO)
                .build();
            classifyEntry(data, entry);
        }
        
        // Calculate totals
        data.setTotalTDSInterest(calculateTotal(data.getTdsOnInterest()));
        data.setTotalTDSContractor(calculateTotal(data.getTdsOnContractor()));
        data.setTotalTDSProfessional(calculateTotal(data.getTdsOnProfessional()));
        data.setTotalTDSOther(calculateTotal(data.getTdsOnOther()));
        
        log.info("26AS Complete: Interest={} Contract={} Prof={} Other={}", 
            data.getTdsOnInterest().size(), data.getTdsOnContractor().size(),
            data.getTdsOnProfessional().size(), data.getTdsOnOther().size());
        
        return data;
    }
    
    private void classifyEntry(Form26ASData data, TDSOtherThanSalary e) {
        if (e == null) return;
        String s = e.getSection();
        if (s == null) s = "OTHER";
        
        switch (s) {
            case "194A": data.getTdsOnInterest().add(e); break;
            case "194C": data.getTdsOnContractor().add(e); break;
            case "194J": data.getTdsOnProfessional().add(e); break;
            case "194H": data.getTdsOnCommission().add(e); break;
            case "194I": data.getTdsOnRent().add(e); break;
            case "194D": data.getTdsOnInsurance().add(e); break;
            case "194IA": case "194IB": data.getTdsOnProperty().add(e); break;
            default: data.getTdsOnOther().add(e); break;
        }
    }
    
    private String extractName(String text, String pan) {
        Pattern p = Pattern.compile("Name of Assessee\\s+([A-Za-z ]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "Unknown";
    }
    
    private BigDecimal parseAmount(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try { return new BigDecimal(str.replace(",", "").replace("₹", "").trim()); } 
        catch (Exception e) { return null; }
    }
    
    private BigDecimal calculateTotal(List<TDSOtherThanSalary> list) {
        BigDecimal t = BigDecimal.ZERO;
        for (TDSOtherThanSalary e : list) if (e.getTaxDeducted() != null) t = t.add(e.getTaxDeducted());
        return t;
    }
    
    private static class SummaryEntry {
        String name, tan, section; BigDecimal amount, tax;
        SummaryEntry(String n, String t, BigDecimal a, BigDecimal tx, String s) {
            name=n; tan=t; amount=a; tax=tx; section=s;
        }
    }
}
