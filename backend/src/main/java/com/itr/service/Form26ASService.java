package com.itr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Form 26AS Parser Service
 * Handles parsing of Form 26AS TXT/ZIP files with password support.
 */
@Slf4j
@Service
public class Form26ASService {

    /**
     * Parse Form 26AS from ZIP or TXT file
     */
    public Map<String, Object> parseForm26AS(MultipartFile file, String password) throws Exception {
        String content;
        String fileName = file.getOriginalFilename();
        
        log.info("Parsing Form 26AS: {} (password: {})", fileName, password != null ? "provided" : "none");
        
        // Check if it's a ZIP file
        if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
            content = extractFromZip(file, password);
        } else {
            // Direct TXT file
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        
        return parse26ASText(content);
    }
    
    /**
     * Extract TXT from password-protected ZIP
     * Handles: unencrypted ZIP, encrypted ZIP, or ZIP with encrypted TXT inside
     */
    private String extractFromZip(MultipartFile zipFile, String password) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Read the entire ZIP file into memory
        byte[] zipBytes = zipFile.getBytes();
        
        // Save to temp file for Zip4j
        File tempFile = File.createTempFile("26as_", ".zip");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(zipBytes);
        }
        
        log.info("Using Zip4j to extract TXT from ZIP...");
        net.lingala.zip4j.ZipFile zip4j = new net.lingala.zip4j.ZipFile(tempFile);
        
        // Set password if provided (works for both encrypted ZIP and encrypted files inside)
        if (password != null && !password.isEmpty()) {
            log.info("Setting password for ZIP extraction: [{}]", password);
            zip4j.setPassword(password.toCharArray());
        }
        
        // Get list of file headers
        var fileHeaders = zip4j.getFileHeaders();
        
        // Find the TXT file
        for (var fh : fileHeaders) {
            if (!fh.isDirectory() && fh.getFileName().toLowerCase().endsWith(".txt")) {
                log.info("Found TXT file: {} (encrypted: {})", fh.getFileName(), fh.isEncrypted());
                
                try (InputStream is = zip4j.getInputStream(fh)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                }
                
                String content = outputStream.toString(StandardCharsets.UTF_8.name());
                log.info("Extracted {} bytes from TXT file", content.length());
                
                // Verify content is valid 26AS data
                if (isValid26ASContent(content)) {
                    log.info("TXT content validated successfully");
                    tempFile.delete();
                    return content;
                } else {
                    log.warn("Extracted content does not look like valid 26AS data (length: {})", content.length());
                    // If password was provided and content is invalid, the password might be wrong
                    if (password != null && !password.isEmpty()) {
                        tempFile.delete();
                        throw new Exception("Could not extract ZIP file: Wrong password or invalid encrypted content!");
                    }
                    // If no password and content is invalid, the file might be corrupted
                    tempFile.delete();
                    throw new Exception("Could not extract ZIP file: Invalid or corrupted TXT content");
                }
            }
        }
        
        tempFile.delete();
        throw new Exception("Could not extract ZIP file: No TXT file found in archive");
    }
    
    /**
     * Check if the extracted content looks like valid 26AS data
     */
    private boolean isValid26ASContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        // Check for common 26AS markers
        return content.contains("Permanent Account Number") || 
               content.contains("PART-I") || 
               content.contains("^") ||
               (content.length() > 100 && content.contains("Assessment Year"));
    }
    
    /**
     * Parse the 26AS TXT content into structured data
     */
    public Map<String, Object> parse26ASText(String content) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tdsEntries = new ArrayList<>();
        
        String[] lines = content.split("\n");
        
        // Extract header information - search through more lines
        String pan = "";
        String assesseeName = "";
        String financialYear = "";
        String assessmentYear = "";
        
        // First, find the header row to map column positions
        Map<String, Integer> headerPositions = new HashMap<>();
        int headerRowIndex = -1;
        
        // Search through first 10 lines for header row
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String line = lines[i].trim();
            if (line.startsWith("^") && (line.contains("Permanent Account Number") || line.contains("File Creation Date"))) {
                String[] parts = line.split("\\^");
                for (int j = 0; j < parts.length; j++) {
                    String part = parts[j].trim().toLowerCase();
                    if (part.contains("permanent account number") || part.contains("pan")) {
                        headerPositions.put("pan", j);
                    } else if (part.contains("name of assessee")) {
                        headerPositions.put("name", j);
                    } else if (part.contains("financial year")) {
                        headerPositions.put("fy", j);
                    } else if (part.contains("assessment year")) {
                        headerPositions.put("ay", j);
                    }
                }
                headerRowIndex = i;
                log.info("Found header row at line {}: {}", i, headerPositions);
                break;
            }
        }
        
        // Now parse the data row (usually next line after header)
        if (headerRowIndex >= 0 && headerRowIndex + 1 < lines.length) {
            String dataLine = lines[headerRowIndex + 1].trim();
            String[] dataParts = dataLine.split("\\^");
            
            if (headerPositions.containsKey("pan") && headerPositions.get("pan") < dataParts.length) {
                pan = dataParts[headerPositions.get("pan")].trim();
            }
            if (headerPositions.containsKey("name") && headerPositions.get("name") < dataParts.length) {
                assesseeName = dataParts[headerPositions.get("name")].trim();
            }
            if (headerPositions.containsKey("fy") && headerPositions.get("fy") < dataParts.length) {
                financialYear = dataParts[headerPositions.get("fy")].trim();
            }
            if (headerPositions.containsKey("ay") && headerPositions.get("ay") < dataParts.length) {
                assessmentYear = dataParts[headerPositions.get("ay")].trim();
            }
            
            log.info("Extracted from header: PAN={}, Name={}, FY={}, AY={}", pan, assesseeName, financialYear, assessmentYear);
        }
        
        // If we still don't have FY/AY, try direct search
        if (financialYear.isEmpty() || assessmentYear.isEmpty()) {
            // Search for date patterns that indicate FY/AY
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                String line = lines[i].trim();
                
                // Look for patterns like "2025-2026" or "2026-2027"
                if (line.contains("2025-2026")) {
                    financialYear = "2025-2026";
                }
                if (line.contains("2026-2027")) {
                    assessmentYear = "2026-2027";
                }
            }
        }
        
        // Fallback: Search entire content for PAN pattern
        if (pan.isEmpty()) {
            java.util.regex.Pattern panPattern = java.util.regex.Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
            java.util.regex.Matcher matcher = panPattern.matcher(content);
            if (matcher.find()) {
                pan = matcher.group();
                log.info("Found PAN via regex: {}", pan);
            }
        }
        
        result.put("pan", pan);
        result.put("assesseeName", assesseeName);
        result.put("financialYear", financialYear);
        result.put("assessmentYear", assessmentYear);
        
        log.info("26AS Header: PAN={}, Name={}, FY={}, AY={}", pan, assesseeName, financialYear, assessmentYear);
        
        // Process PART-I - Tax Deducted at Source
        boolean inPartI = false;
        
        // Track deductor info - key is TAN
        Map<String, Map<String, Object>> deductorMap = new LinkedHashMap<>();
        
        // Track current deductor being processed
        String currentDeductorTAN = "";
        String currentDeductorName = "";
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Detect PART-I start
            if (line.contains("PART-I") && !line.contains("PART-II")) {
                inPartI = true;
                log.info("Found PART-I at line {}", i);
                continue;
            }
            
            // Detect end of PART-I
            if (line.contains("PART-II") || line.contains("PART - II")) {
                log.info("Found PART-II at line {}, breaking", i);
                break;
            }
            
            if (!inPartI) continue;
            
            // Skip empty lines
            if (line.isEmpty()) continue;
            
            // Skip header/column lines
            if (line.contains("Sr. No.") || line.startsWith("^Sr. No.")) {
                continue;
            }
            
            // Parse deductor summary line:
            // Format: 1^WELLS FARGO INTERNATIONAL SOLUTIONS PRIVATE LIMITED^HYDW00345C^^^^^1964956.60^185112.00^185112.00^
            if (line.matches("^\\d+\\^[^\\^]+\\^[^\\^]{10}\\^.*")) {
                String[] parts = line.split("\\^");
                
                if (parts.length >= 9) {
                    String deductorName = parts[1].trim();
                    String deductorTAN = parts[2].trim();
                    
                    double totalAmount = parseAmount(parts[7].trim());
                    double totalTax = parseAmount(parts[8].trim());
                    double totalTDS = parseAmount(parts[9].trim());
                    
                    // Update current deductor
                    currentDeductorTAN = deductorTAN;
                    currentDeductorName = deductorName;
                    
                    // Check if deductor already exists
                    if (!deductorMap.containsKey(deductorTAN)) {
                        Map<String, Object> ded = new HashMap<>();
                        ded.put("employerName", deductorName);
                        ded.put("employerTAN", deductorTAN);
                        ded.put("totalAmount", totalAmount);
                        ded.put("totalTDS", totalTDS);
                        ded.put("sectionCode", "192"); // Default to salary, will be updated
                        ded.put("transactionCount", 0);
                        ded.put("sectionsFound", new java.util.HashSet<String>());
                        ded.put("transactions", new java.util.ArrayList<Map<String, Object>>()); // Store all transactions
                        deductorMap.put(deductorTAN, ded);
                        
                        log.info("Found deductor: {} (TAN: {}), Amount: {}, TDS: {}", 
                            deductorName, deductorTAN, totalAmount, totalTDS);
                    }
                }
            }
            
            // Parse transaction lines:
            // Format: ^1^192^26-Mar-2026^F^28-May-2026^-^148459.00^11664.00^11664.00^
            if (line.startsWith("^")) {
                String[] parts = line.split("\\^");
                
                // Find section in transaction line
                for (int j = 0; j < parts.length; j++) {
                    String part = parts[j].trim();
                    
                    // Check for valid TDS sections: 192, 192A, 193, 194, 194A, 194B, 194BB, 194C, 194D, 194I, 194IA, 194IB, 194J, 194M, 194N, 194Q, 194R, 194S, 194BA
                    if (part.matches("19[234]\\w?")) {
                        String section = part;
                        
                        // Get transaction details
                        String srNo = j > 0 ? parts[j - 1].trim() : "";
                        String transactionDate = parts.length > j + 1 ? parts[j + 1].trim() : "";
                        String status = parts.length > j + 2 ? parts[j + 2].trim() : "";
                        String bookingDate = parts.length > j + 3 ? parts[j + 3].trim() : "";
                        String remarks = parts.length > j + 4 ? parts[j + 4].trim() : "";
                        
                        // Get amounts
                        if (j + 7 < parts.length) {
                            String amountStr = parts[j + 6].trim().replace("-", "");
                            String taxStr = parts[j + 7].trim().replace("-", "");
                            String tdsDeposited = parts.length > j + 8 ? parts[j + 8].trim().replace("-", "") : "";
                            
                            double amount = parseAmount(amountStr);
                            double tax = parseAmount(taxStr);
                            double tds = parseAmount(tdsDeposited);
                            
                            // Create transaction object with all details
                            Map<String, Object> transaction = new HashMap<>();
                            transaction.put("srNo", srNo);
                            transaction.put("section", section);
                            transaction.put("sectionDescription", getSectionDescription(section));
                            transaction.put("transactionDate", transactionDate);
                            transaction.put("status", status);
                            transaction.put("bookingDate", bookingDate);
                            transaction.put("remarks", remarks);
                            transaction.put("amount", amount);
                            transaction.put("taxDeducted", tax);
                            transaction.put("tdsDeposited", tds);
                            
                            // Track section for current deductor and store transaction
                            if (!currentDeductorTAN.isEmpty() && deductorMap.containsKey(currentDeductorTAN)) {
                                Map<String, Object> ded = deductorMap.get(currentDeductorTAN);
                                @SuppressWarnings("unchecked")
                                java.util.Set<String> sections = (java.util.Set<String>) ded.get("sectionsFound");
                                sections.add(section);
                                
                                @SuppressWarnings("unchecked")
                                java.util.List<Map<String, Object>> transactions = (java.util.List<Map<String, Object>>) ded.get("transactions");
                                transactions.add(transaction);
                                
                                // Update transaction count
                                ded.put("transactionCount", (Integer) ded.get("transactionCount") + 1);
                                
                                log.debug("Transaction for {}: Section={}, Date={}, Amount={}, Tax={}", 
                                    currentDeductorTAN, section, transactionDate, amount, tax);
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        // Update section codes based on sections found
        for (Map.Entry<String, Map<String, Object>> entry : deductorMap.entrySet()) {
            Map<String, Object> ded = entry.getValue();
            @SuppressWarnings("unchecked")
            java.util.Set<String> sections = (java.util.Set<String>) ded.get("sectionsFound");
            
            // Determine primary section (priority order)
            String primarySection;
            if (sections.contains("192") || sections.contains("192A")) {
                primarySection = "192";
            } else if (sections.contains("194") && !sections.contains("194A") && !sections.contains("194B") && !sections.contains("194S")) {
                primarySection = "194";
            } else if (sections.contains("194A")) {
                primarySection = "194A";
            } else if (sections.contains("194B")) {
                primarySection = "194B";
            } else if (sections.contains("194S")) {
                primarySection = "194S";
            } else if (sections.contains("194R")) {
                primarySection = "194R";
            } else if (!sections.isEmpty()) {
                primarySection = sections.iterator().next();
            } else {
                primarySection = "194A"; // Default
            }
            
            ded.put("sectionCode", primarySection);
            log.info("Deductor {} has sections: {}, primary: {}", entry.getKey(), sections, primarySection);
        }
        
        log.info("Total deductor entries found: {}", deductorMap.size());
        
        // Convert deductor map to list of TDS entries
        // Only include entries where TDS was actually deducted (TDS > 0)
        // Entries with TDS = 0 will be reported in respective income heads (Other Sources, etc.)
        for (Map.Entry<String, Map<String, Object>> entry : deductorMap.entrySet()) {
            Map<String, Object> ded = entry.getValue();
            ded.put("sectionDescription", getSectionDescription((String) ded.get("sectionCode")));
            
            double totalTDS = (Double) ded.get("totalTDS");
            if (totalTDS > 0) {
                // Only add entries with actual TDS credit to TDS tab
                tdsEntries.add(ded);
                log.info("TDS entry (TDS > 0): {} - Section {}, TDS: {}", 
                    ded.get("employerName"), ded.get("sectionCode"), totalTDS);
            } else {
                // Log entries with no TDS - these go to respective income heads
                log.info("Income entry (TDS = 0): {} - Section {}, Amount: {}", 
                    ded.get("employerName"), ded.get("sectionCode"), ded.get("totalAmount"));
            }
        }
        
        log.info("Total TDS entries for response (TDS > 0): {}", tdsEntries.size());
        
        result.put("tdsEntries", tdsEntries);
        
        // Build income breakdown by section for reporting in respective ITR heads
        Map<String, Double> incomeBySection = new HashMap<>();
        incomeBySection.put("192", 0.0); // Salary
        incomeBySection.put("194", 0.0); // Dividends
        incomeBySection.put("194A", 0.0); // Interest
        incomeBySection.put("194B", 0.0); // Lottery
        incomeBySection.put("194S", 0.0); // VDA
        incomeBySection.put("OTHER", 0.0);
        
        for (Map.Entry<String, Map<String, Object>> entry : deductorMap.entrySet()) {
            Map<String, Object> ded = entry.getValue();
            String section = (String) ded.get("sectionCode");
            double amount = (Double) ded.get("totalAmount");
            
            if (incomeBySection.containsKey(section)) {
                incomeBySection.put(section, incomeBySection.get(section) + amount);
            } else {
                incomeBySection.put("OTHER", incomeBySection.get("OTHER") + amount);
            }
        }
        
        log.info("Income breakdown by section: {}", incomeBySection);
        
        // Map sections to ITR income heads using the mapping
        Map<String, Double> incomeByHead = new HashMap<>();
        incomeByHead.put("SALARY", 0.0);
        incomeByHead.put("HOUSE_PROPERTY", 0.0);
        incomeByHead.put("CAPITAL_GAINS", 0.0);
        incomeByHead.put("BUSINESS", 0.0);
        incomeByHead.put("OTHER_SOURCES_INTEREST", 0.0);
        incomeByHead.put("OTHER_SOURCES_DIVIDEND", 0.0);
        incomeByHead.put("WINNINGS", 0.0);
        incomeByHead.put("VDA", 0.0);
        incomeByHead.put("ONLINE_GAMING", 0.0);
        incomeByHead.put("TCS", 0.0);
        
        for (Map.Entry<String, Map<String, Object>> entry : deductorMap.entrySet()) {
            Map<String, Object> ded = entry.getValue();
            String section = (String) ded.get("sectionCode");
            double amount = (Double) ded.get("totalAmount");
            
            // Map section to income head
            String incomeHead = getSectionIncomeHead(section);
            incomeByHead.put(incomeHead, incomeByHead.getOrDefault(incomeHead, 0.0) + amount);
        }
        
        log.info("Income breakdown by head: {}", incomeByHead);
        
        // Income breakdown for ITR heads - mapped to proper fields
        Map<String, Object> incomeBreakdown = new HashMap<>();
        incomeBreakdown.put("salaryIncome", incomeByHead.getOrDefault("SALARY", 0.0));
        incomeBreakdown.put("housePropertyIncome", incomeByHead.getOrDefault("HOUSE_PROPERTY", 0.0));
        incomeBreakdown.put("capitalGains", incomeByHead.getOrDefault("CAPITAL_GAINS", 0.0));
        incomeBreakdown.put("businessIncome", incomeByHead.getOrDefault("BUSINESS", 0.0));
        incomeBreakdown.put("interestIncome", incomeByHead.getOrDefault("OTHER_SOURCES_INTEREST", 0.0));
        incomeBreakdown.put("dividendIncome", incomeByHead.getOrDefault("OTHER_SOURCES_DIVIDEND", 0.0));
        incomeBreakdown.put("lotteryIncome", incomeByHead.getOrDefault("WINNINGS", 0.0));
        incomeBreakdown.put("vdaIncome", incomeByHead.getOrDefault("VDA", 0.0));
        incomeBreakdown.put("onlineGamingIncome", incomeByHead.getOrDefault("ONLINE_GAMING", 0.0));
        incomeBreakdown.put("tcsIncome", incomeByHead.getOrDefault("TCS", 0.0));
        incomeBreakdown.put("deductorDetails", new ArrayList<>(deductorMap.values()));
        incomeBreakdown.put("tdsEntries", tdsEntries); // Only entries with TDS > 0
        
        result.put("incomeBreakdown", incomeBreakdown);
        
        // deductorAggregates includes ALL entries (for display)
        List<Map<String, Object>> deductorAggregates = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : deductorMap.entrySet()) {
            Map<String, Object> agg = entry.getValue();
            agg.put("sectionDescription", getSectionDescription((String) agg.get("sectionCode")));
            deductorAggregates.add(agg);
        }
        
        log.info("Aggregated into {} unique deductor entries", deductorAggregates.size());
        
        result.put("deductorAggregates", deductorAggregates);
        
        // Calculate summary by section (for backward compatibility)
        double totalTds192 = 0, totalTds194 = 0, totalTds194A = 0, totalTds194B = 0, 
               totalTds194R = 0, totalTds194S = 0, totalTds194IA = 0, totalTds194IB = 0, 
               totalTds194M = 0, totalTds194BA = 0, totalTdsOther = 0;
        
        for (Map<String, Object> agg : deductorAggregates) {
            String section = (String) agg.get("sectionCode");
            double tds = (Double) agg.get("totalTDS");
            
            switch (section) {
                case "192": totalTds192 += tds; break;
                case "194": totalTds194 += tds; break;
                case "194A": totalTds194A += tds; break;
                case "194B": totalTds194B += tds; break;
                case "194R": totalTds194R += tds; break;
                case "194S": totalTds194S += tds; break;
                case "194IA": totalTds194IA += tds; break;
                case "194IB": totalTds194IB += tds; break;
                case "194M": totalTds194M += tds; break;
                case "194BA": totalTds194BA += tds; break;
                default: totalTdsOther += tds;
            }
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTdsSalary", totalTds192);
        summary.put("totalTdsDividend", totalTds194);
        summary.put("totalTdsInterest", totalTds194A);
        summary.put("totalTdsLottery", totalTds194B);
        summary.put("totalTds194R", totalTds194R);
        summary.put("totalTdsVDA", totalTds194S);
        summary.put("totalTdsProperty", totalTds194IA);
        summary.put("totalTdsRent", totalTds194IB);
        summary.put("totalTdsContractor", totalTds194M);
        summary.put("totalTdsOnlineGaming", totalTds194BA);
        summary.put("totalTdsOther", totalTdsOther);
        summary.put("grandTotalTDS", totalTds192 + totalTds194 + totalTds194A + totalTds194B + 
                   totalTds194R + totalTds194S + totalTds194IA + totalTds194IB + 
                   totalTds194M + totalTds194BA + totalTdsOther);
        
        result.put("summary", summary);
        log.info("26AS Parsing Complete: {} TDS entries, Total TDS: ₹{}", tdsEntries.size(), summary.get("grandTotalTDS"));
        
        return result;
    }
    
    private double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return 0.0;
        String cleaned = amountStr.replaceAll("[^\\d.-]", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public String getSectionDescription(String section) {
        switch (section) {
            // TDS Sections
            case "192": return "Salary";
            case "192A": return "TDS on PF withdrawal";
            case "193": return "Interest on Securities";
            case "194": return "Dividends";
            case "194A": return "Interest other than securities";
            case "194B": return "Lottery or Crossword Puzzle";
            case "194BB": return "Horse Race Winnings";
            case "194BA": return "Online Gaming Winnings";
            case "194C": return "Payments to Contractors";
            case "194D": return "Insurance Commission";
            case "194DA": return "Life Insurance Payment";
            case "194E": return "Non-Resident Sportsmen";
            case "194EE": return "NSS Deposits";
            case "194G": return "Lottery Ticket Commission";
            case "194H": return "Commission or Brokerage";
            case "194I": return "Rent (General)";
            case "194IA": return "Sale of Immovable Property";
            case "194IB": return "Rent by Individuals/HUF";
            case "194IC": return "Specified Agreement";
            case "194J": return "Professional/Technical Services";
            case "194K": return "Mutual Fund/UTI Income";
            case "194LA": return "Compensation on Acquisition";
            case "194LB": return "Infrastructure Debt Fund Interest";
            case "194LC": return "Interest - Bonds/Securities";
            case "194LD": return "Interest - Gov Securities";
            case "194LBA": return "Business Trust Income";
            case "194LBB": return "Investment Fund Income";
            case "194LBC": return "Securitization Trust Income";
            case "194M": return "Sum by Individual/HUF";
            case "194N": return "Cash Payments";
            case "194O": return "E-commerce Operator";
            case "194P": return "Senior Citizen Deduction";
            case "194Q": return "Purchase of Goods";
            case "194R": return "Business Perquisites";
            case "194S": return "Virtual Digital Asset";
            case "194T": return "Partner Payment";
            case "195": return "Non-Resident Payments";
            case "196A": return "Units of Non-Residents";
            case "196B": return "Offshore Fund Units";
            case "196C": return "Foreign Currency Bonds";
            case "196D": return "Foreign Institutional Investor";
            case "196DA": return "Specified Fund Income";
            // TCS Sections
            case "206CA": return "TCS - Alcoholic Liquor";
            case "206CB": return "TCS - Timber (Forest Lease)";
            case "206CC": return "TCS - Timber (Other)";
            case "206CD": return "TCS - Forest Produce";
            case "206CE": return "TCS - Scrap";
            case "206CF": return "TCS - Parking Lot";
            case "206CG": return "TCS - Toll Plaza";
            case "206CH": return "TCS - Mine/Quarry";
            case "206CI": return "TCS - Tendu Leaves";
            case "206CJ": return "TCS - Minerals";
            case "206CK": return "TCS - Bullion/Jewellery";
            case "206CL": return "TCS - Motor Vehicle";
            case "206CM": return "TCS - Goods";
            default: return "Section " + section;
        }
    }
    
    /**
     * Map section to Income Head for ITR
     * Returns the income head category for each section
     */
    public String getSectionIncomeHead(String section) {
        // SALARY INCOME
        if (section.equals("192") || section.equals("192A") || section.equals("194T")) {
            return "SALARY";
        }
        
        // HOUSE PROPERTY INCOME
        if (section.equals("194IB")) {
            return "HOUSE_PROPERTY";
        }
        
        // BUSINESS/PROFESSION INCOME
        if (section.equals("194C") || section.equals("194H") || 
            section.equals("194I") || section.equals("194J") ||
            section.equals("194R") || section.equals("194O") ||
            section.equals("194Q") || section.equals("194M") ||
            section.equals("194N") || section.equals("194D") ||
            section.equals("194DA") || section.equals("194E") ||
            section.equals("194EE") || section.equals("194G") ||
            section.equals("194IC") || section.equals("194P")) {
            return "BUSINESS";
        }
        
        // CAPITAL GAINS
        if (section.equals("194IA") || section.equals("194LA")) {
            return "CAPITAL_GAINS";
        }
        
        // OTHER SOURCES - Interest
        if (section.equals("193") || section.equals("194A") ||
            section.equals("194K") || section.equals("194LB") ||
            section.equals("194LC") || section.equals("194LD") ||
            section.equals("194LBA") || section.equals("194LBB") ||
            section.equals("194LBC") || section.equals("195") ||
            section.equals("196A") || section.equals("196B") ||
            section.equals("196C") || section.equals("196D") ||
            section.equals("196DA")) {
            return "OTHER_SOURCES_INTEREST";
        }
        
        // OTHER SOURCES - Dividends
        if (section.equals("194")) {
            return "OTHER_SOURCES_DIVIDEND";
        }
        
        // WINNINGS (30% Tax)
        if (section.equals("194B") || section.equals("194BB")) {
            return "WINNINGS";
        }
        
        // VDA (30% Tax)
        if (section.equals("194S")) {
            return "VDA";
        }
        
        // Online Gaming (30% Tax)
        if (section.equals("194BA")) {
            return "ONLINE_GAMING";
        }
        
        // TCS (Other Sources)
        if (section.startsWith("206C")) {
            return "TCS";
        }
        
        return "OTHER";
    }
    
    public Map<String, Object> verifyPAN(String clientPAN, Map<String, Object> parsed26AS) {
        Map<String, Object> result = new HashMap<>();
        String form26ASPAN = (String) parsed26AS.get("pan");
        
        boolean matches = clientPAN != null && clientPAN.equalsIgnoreCase(form26ASPAN);
        
        result.put("matches", matches);
        result.put("clientPAN", clientPAN);
        result.put("form26ASPAN", form26ASPAN);
        result.put("assesseeName", parsed26AS.get("assesseeName"));
        
        if (!matches) {
            result.put("error", "PAN Mismatch: Client PAN (" + clientPAN + ") does not match Form 26AS PAN (" + form26ASPAN + ")");
            log.warn("PAN Verification Failed: Client={}, 26AS={}", clientPAN, form26ASPAN);
        } else {
            log.info("PAN Verification Passed: {}", clientPAN);
        }
        
        return result;
    }
}
