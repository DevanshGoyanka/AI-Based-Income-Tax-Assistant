package com.itr.service.integration;

import com.itr.dto.AISData;
import com.itr.dto.AISData.*;
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
 * AIS (Annual Information Statement) import service
 * Extracts Part A (General Info) and Part B (Financial Transactions)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AISImportService {

    private final ITDPdfDecryptor pdfDecryptor;

    public AISData importAIS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String pdfText = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        
        log.debug("Extracted PDF text length: {} chars", pdfText.length());
        log.debug("PDF text preview (first 500 chars): {}", pdfText.substring(0, Math.min(500, pdfText.length())));
        
        AISData data = new AISData();
        data.setGeneralInfo(parsePartA(pdfText));
        data.setPartB1(parsePartB1(pdfText));
        data.setPartB2(parsePartB2(pdfText));
        data.setPartB3(parsePartB3(pdfText));
        
        log.info("AIS import complete: {} TDS entries, {} SFT entries (dividend={}, securities={}, MF={}), {} tax payments",
                data.getPartB1().getTdsEntries().size(),
                data.getPartB2().getSecuritiesSale().size(),
                data.getPartB2().getDividendIncome(),
                data.getPartB2().getSecuritiesPurchaseAmount(),
                data.getPartB2().getMutualFundPurchase().size(),
                data.getPartB3().size());
        
        return data;
    }

    /**
     * Part A: General Information (PAN, Aadhaar, Name, DOB, Mobile, Email, Address)
     */
    private AISGeneralInfo parsePartA(String pdfText) {
        AISGeneralInfo info = new AISGeneralInfo();
        
        String partAText = pdfText.substring(0, Math.min(1000, pdfText.length()));
        
        // PAN: 10-char alphanumeric
        Pattern panPattern = Pattern.compile("([A-Z]{5}\\d{4}[A-Z])");
        Matcher pm = panPattern.matcher(partAText);
        if (pm.find()) info.setPan(pm.group(1));
        
        // Aadhaar: masked format
        Pattern aadhaarPattern = Pattern.compile("(\\d{4}\\s\\d{4}\\s\\d{4}|XXXX\\sXXXX\\s\\d{4})");
        Matcher am = aadhaarPattern.matcher(partAText);
        if (am.find()) info.setAadhaar(am.group(1));
        
        // DOB: DD/MM/YYYY
        Pattern dobPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
        Matcher dm = dobPattern.matcher(partAText);
        if (dm.find()) {
            try {
                info.setDob(LocalDate.parse(dm.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } catch (Exception e) {
                log.warn("Failed to parse DOB: {}", dm.group(1));
            }
        }
        
        // Mobile: 10 digits starting with 6-9
        Pattern mobilePattern = Pattern.compile("\\b([6-9]\\d{9})\\b");
        Matcher mm = mobilePattern.matcher(partAText);
        if (mm.find()) info.setMobile(mm.group(1));
        
        // Email
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher em = emailPattern.matcher(partAText);
        if (em.find()) info.setEmail(em.group(1));
        
        // Address: Extract multi-line address after "Address:" keyword
        int addrIdx = pdfText.indexOf("Address:");
        if (addrIdx == -1) addrIdx = pdfText.indexOf("ADDRESS:");
        if (addrIdx != -1) {
            String addrSection = pdfText.substring(addrIdx + 8, Math.min(addrIdx + 300, pdfText.length()));
            // Extract until next section marker or 3 lines
            String[] lines = addrSection.split("\\n");
            StringBuilder addr = new StringBuilder();
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                String line = lines[i].trim();
                if (line.isEmpty() || line.matches("^(Part|PART|PAN|Mobile|Email).*")) break;
                if (addr.length() > 0) addr.append(", ");
                addr.append(line);
            }
            if (addr.length() > 0) info.setAddress(addr.toString());
        }
        
        return info;
    }

    /**
     * Part B1: TDS/TCS Transactions
     * Extract by section code (TDS-192, TDS-194A, etc.)
     */
    private AISPartB1Data parsePartB1(String pdfText) {
        AISPartB1Data data = new AISPartB1Data();
        data.setTdsEntries(new ArrayList<>());
        
        int b1Start = pdfText.indexOf("Part B1");
        if (b1Start == -1) b1Start = pdfText.indexOf("B1-Information relating to");
        if (b1Start == -1) return data;
        
        int b1End = pdfText.indexOf("Part B2", b1Start);
        if (b1End == -1) b1End = pdfText.indexOf("B2-", b1Start);
        if (b1End == -1) b1End = Math.min(b1Start + 20000, pdfText.length());
        
        String b1Text = pdfText.substring(b1Start, b1End);
        
        // Look for SR. NO. INFORMATION CODE pattern which marks each TDS entry
        Pattern entryPattern = Pattern.compile(
            "SR\\.\\s*NO\\.\\s+INFORMATION\\s+CODE\\s+INFORMATION\\s+DESCRIPTION\\s+INFORMATION\\s+SOURCE\\s+COUNT\\s+AMOUNT\\s*" +
            "(\\d+)\\s+(TDS-\\d{3}[A-Z]?)\\s+([^\\n]+?)\\s+([A-Z][A-Z0-9\\s&\\.,'()-]+?)\\s*\\(([A-Z]{4}\\d{5}[A-Z])\\)\\s+(\\d+)\\s+([\\d,]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher em = entryPattern.matcher(b1Text);
        while (em.find()) {
            String sectionCode = em.group(2).replace("TDS-", "");
            String deductorName = em.group(4).trim();
            String tan = em.group(5);
            long amount = parseAmountLong(em.group(7));
            
            // Find the detail table for this entry to get TDS amounts
            int entryStart = em.start();
            int nextEntry = b1Text.indexOf("SR. NO. INFORMATION CODE", entryStart + 10);
            if (nextEntry == -1) nextEntry = b1Text.length();
            
            String entryBlock = b1Text.substring(entryStart, Math.min(nextEntry, entryStart + 2000));
            
            // Extract transaction rows to sum TDS
            Pattern rowPattern = Pattern.compile(
                "(\\d+)\\s+Q[1-4]\\([A-Za-z-]+\\)\\s+\\d{2}/\\d{2}/\\d{4}\\s+" +
                "([\\d,]+)\\s+([\\d,]+)\\s+([\\d,]+)\\s+(?:Active|Inactive)"
            );
            
            Matcher rm = rowPattern.matcher(entryBlock);
            long totalAmtPaid = 0, totalTDS = 0;
            while (rm.find()) {
                totalAmtPaid += parseAmountLong(rm.group(2));
                totalTDS += parseAmountLong(rm.group(3));
            }
            
            // If no detail rows found, use the summary amount
            if (totalAmtPaid == 0) {
                totalAmtPaid = amount;
            }
            
            AISTDSEntry entry = new AISTDSEntry();
            entry.setSection(sectionCode);
            entry.setDeductorName(deductorName);
            entry.setDeductorTAN(tan);
            entry.setTotalAmountPaid(totalAmtPaid);
            entry.setTotalTDSDeducted(totalTDS);
            
            if (totalAmtPaid > 0) {
                data.getTdsEntries().add(entry);
            }
        }
        
        return data;
    }

    /**
     * Part B2: Specified Financial Transactions (SFT)
     */
    private AISPartB2Data parsePartB2(String pdfText) {
        AISPartB2Data data = new AISPartB2Data();
        data.setSecuritiesSale(new ArrayList<>());
        data.setMutualFundPurchase(new ArrayList<>());
        
        // Try multiple section markers
        int b2Start = pdfText.indexOf("Part B2");
        if (b2Start == -1) b2Start = pdfText.indexOf("B2-");
        if (b2Start == -1) b2Start = pdfText.indexOf("Part B2-");
        if (b2Start == -1) b2Start = pdfText.indexOf("PartB2");
        if (b2Start == -1) {
            log.warn("Part B2 section not found in PDF text");
            return data;
        }
        
        int b2End = pdfText.indexOf("Part B3", b2Start);
        if (b2End == -1) b2End = pdfText.indexOf("B3-", b2Start);
        if (b2End == -1) b2End = pdfText.indexOf("Part B7", b2Start);
        if (b2End == -1) b2End = pdfText.indexOf("B7-", b2Start);
        if (b2End == -1) b2End = Math.min(b2Start + 30000, pdfText.length());
        
        String b2Text = pdfText.substring(b2Start, b2End);
        log.debug("Part B2 text length: {} chars, preview: {}", b2Text.length(), 
            b2Text.substring(0, Math.min(300, b2Text.length())));
        
        // SFT-015: Dividend - look for the entry header pattern
        data.setDividendIncome(extractSFTDividend(b2Text));
        
        // SFT-017: Securities sale (listed equity)
        data.setSecuritiesSale(extractSFTSaleDetails(b2Text));
        
        // SFT-017(Pur): Securities purchase
        data.setSecuritiesPurchaseAmount(extractSFTPurchaseAmount(b2Text));
        
        // SFT-018: Mutual fund purchases
        data.setMutualFundPurchase(extractMFPurchases(b2Text));
        
        // SFT-016: Interest on securities
        data.setInterestOnSecurities(extractSFTAmount(b2Text, "SFT-016", "SFT-16"));
        
        log.debug("Part B2 extraction complete: dividend={}, securities sale entries={}, purchase amount={}, MF entries={}", 
            data.getDividendIncome(), data.getSecuritiesSale().size(), 
            data.getSecuritiesPurchaseAmount(), data.getMutualFundPurchase().size());
        
        return data;
    }

    private long extractSFTDividend(String text) {
        // Look for SFT-015 Dividend income pattern
        Pattern dividendPattern = Pattern.compile(
            "SFT-015\\s+Dividend income[^\\n]*\\s+" +
            "([A-Z][A-Z0-9\\s&\\.,'()-]+?)\\s*\\([A-Z0-9.]+\\)\\s+(\\d+)\\s+([\\d,]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = dividendPattern.matcher(text);
        if (m.find()) {
            long amount = parseAmountLong(m.group(3));
            log.debug("Found dividend via header pattern: {}", amount);
            return amount;
        }
        
        // Fallback: look for "Dividend" section with REPORTED ON pattern
        int divIdx = text.indexOf("Dividend");
        if (divIdx != -1) {
            String divBlock = text.substring(divIdx, Math.min(divIdx + 1000, text.length()));
            log.debug("Dividend block found, searching for REPORTED ON pattern");
            Pattern amtPattern = Pattern.compile("REPORTED ON\\s+\\d{2}/\\d{2}/\\d{4}\\s+([\\d,]+)\\s+Active");
            Matcher am = amtPattern.matcher(divBlock);
            if (am.find()) {
                long amount = parseAmountLong(am.group(1));
                log.debug("Found dividend via REPORTED ON pattern: {}", amount);
                return amount;
            }
        }
        
        log.debug("No dividend found in Part B2");
        return 0;
    }

    private long extractSFTPurchaseAmount(String text) {
        // Look for SFT-17(Pur) Purchase of securities pattern
        Pattern purchasePattern = Pattern.compile(
            "SFT-17\\(Pur\\)[^\\n]*Purchase of securities[^\\n]*\\s+" +
            "([A-Z][A-Z0-9\\s&\\.,'()-]+?)\\s*\\([A-Z0-9.]+\\)\\s+(\\d+)\\s+([\\d,]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = purchasePattern.matcher(text);
        if (m.find()) {
            return parseAmountLong(m.group(3));
        }
        
        // Fallback: look for CLIENT ID and MARKET PURCHASE columns
        int purIdx = text.indexOf("Purchase of securities");
        if (purIdx != -1) {
            String purBlock = text.substring(purIdx, Math.min(purIdx + 1500, text.length()));
            Pattern amtPattern = Pattern.compile("CLIENT ID.*?MARKET PURCHASE\\s+MARKET SALES[^\\n]*\\s+\\d+-\\d+\\s+First\\s+([\\d,]+)\\s+([\\d,]+)\\s+Active");
            Matcher am = amtPattern.matcher(purBlock);
            if (am.find()) {
                return parseAmountLong(am.group(1));
            }
        }
        
        return 0;
    }

    private long extractSFTAmount(String text, String... codes) {
        for (String code : codes) {
            int idx = text.indexOf(code);
            if (idx == -1) continue;
            
            String block = text.substring(idx, Math.min(idx + 1000, text.length()));
            Pattern amtPattern = Pattern.compile("([\\d,]+)\\s+(?:Active|Inactive)");
            Matcher m = amtPattern.matcher(block);
            if (m.find()) {
                return parseAmountLong(m.group(1));
            }
        }
        return 0;
    }

    private List<SFTSaleEntry> extractSFTSaleDetails(String text) {
        List<SFTSaleEntry> entries = new ArrayList<>();
        
        int saleIdx = text.indexOf("Sale of listed equity share");
        if (saleIdx == -1) saleIdx = text.indexOf("SFT-17-LES");
        if (saleIdx == -1) {
            log.debug("No securities sale section found");
            return entries;
        }
        
        String saleBlock = text.substring(saleIdx, Math.min(saleIdx + 3000, text.length()));
        log.debug("Securities sale block (first 500 chars): {}", saleBlock.substring(0, Math.min(500, saleBlock.length())));
        
        // Actual format: "1 07/07/2025 RELIANCE INDUSTRIES LIMITED EQUITY Listed Equity Market Market Short 1.00 1,523.45 1,523 1,517.00 961.15 961.15 0 Active"
        // Note: "term" may be missing, asset type might just be "Short" or "Long"
        Pattern salePattern = Pattern.compile(
            "(\\d+)\\s+" +                                   // SR. NO.
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +                   // Date
            "([A-Z][A-Z0-9\\s]+?)\\s+EQUITY\\s+" +           // Security name + EQUITY
            "Listed\\s+Equity\\s+" +                         // Listed Equity
            "Market\\s+Market\\s+" +                         // Debit/Credit type
            "(Short|Long)(?:\\s+term)?\\s+" +                // Asset type (term is optional)
            "([\\d.]+)\\s+" +                                // Quantity
            "([\\d,]+\\.\\d+)\\s+" +                         // Sale price per unit
            "([\\d,]+)\\s+" +                                // Sales consideration
            "([\\d,]+\\.\\d+)\\s+" +                         // Cost of acquisition
            "([\\d,]+\\.\\d+)\\s+" +                         // Unit FMV
            "([\\d,]+\\.\\d+)\\s+" +                         // Fair market value
            "(\\d+)\\s+" +                                   // Indexed cost
            "Active",                                        // Status
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = salePattern.matcher(saleBlock);
        if (!m.find()) {
            log.debug("Securities sale pattern did not match");
            return entries;
        }
        
        do {
            SFTSaleEntry entry = new SFTSaleEntry();
            entry.setTransferDate(parseDate(m.group(2)));
            entry.setSecurityName(m.group(3).trim());
            entry.setAssetType(m.group(4).toLowerCase().contains("short") ? "STCG" : "LTCG");
            entry.setQuantity(new BigDecimal(m.group(5)));
            entry.setSalePricePerUnit(parseAmount(m.group(6)));
            entry.setSalesConsideration(parseAmount(m.group(7)));
            entry.setCostOfAcquisition(parseAmount(m.group(8)));
            entry.setFmvPerUnit(parseAmount(m.group(9)));
            entry.setIndexedCostOfAcquisition(parseAmount(m.group(11)));
            entries.add(entry);
            log.debug("Found securities sale: {} on {}, type={}, consideration={}, cost={}", 
                entry.getSecurityName(), entry.getTransferDate(), entry.getAssetType(),
                entry.getSalesConsideration(), entry.getCostOfAcquisition());
        } while (m.find());
        
        return entries;
    }

    private List<MFPurchase> extractMFPurchases(String text) {
        List<MFPurchase> purchases = new ArrayList<>();
        
        // Look for SFT-18(Pur) Purchase of mutual funds entries
        Pattern mfHeaderPattern = Pattern.compile(
            "SFT-18\\(Pur\\)[^\\n]*Purchase of mutual funds[^\\n]*\\s+" +
            "([A-Z][A-Za-z0-9\\s&\\.,'()-]+?)\\s*\\([A-Z0-9.]+\\)\\s+(\\d+)\\s+([\\d,]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher hm = mfHeaderPattern.matcher(text);
        while (hm.find()) {
            String amcSource = hm.group(1).trim();
            long totalAmount = parseAmountLong(hm.group(3));
            
            // Extract AMC name from the source (e.g., "Computer Age Management Services Limited - ICICI Prudential Mutual Fund(P)")
            String amcName = amcSource;
            if (amcSource.contains(" - ")) {
                amcName = amcSource.substring(amcSource.indexOf(" - ") + 3).trim();
            }
            
            // Look for detail rows after this header
            int headerPos = hm.start();
            int nextHeader = text.indexOf("SFT-", headerPos + 10);
            if (nextHeader == -1) nextHeader = text.length();
            
            String detailBlock = text.substring(headerPos, Math.min(nextHeader, headerPos + 1500));
            
            // Sum up all quarters for this AMC
            Pattern detailPattern = Pattern.compile(
                "Q[1-4]\\([A-Za-z-]+\\)\\s+\\d+\\s+" +
                "([A-Za-z0-9\\s&\\.,'()-]+?)\\s+" +
                "First\\s+([\\d,]+)\\s+([\\d,]+)\\s+Active"
            );
            
            Matcher dm = detailPattern.matcher(detailBlock);
            long totalPurchase = 0, totalSales = 0;
            while (dm.find()) {
                totalPurchase += parseAmountLong(dm.group(2));
                totalSales += parseAmountLong(dm.group(3));
            }
            
            // If no detail rows, use header amount
            if (totalPurchase == 0) {
                totalPurchase = totalAmount;
            }
            
            MFPurchase purchase = new MFPurchase();
            purchase.setAmcName(amcName);
            purchase.setTotalPurchase(totalPurchase);
            purchase.setTotalSales(totalSales);
            purchases.add(purchase);
        }
        
        return purchases;
    }

    /**
     * Part B3: Tax Payments (Self-Assessment + Advance Tax)
     */
    private List<TaxPaymentAIS> parsePartB3(String pdfText) {
        List<TaxPaymentAIS> payments = new ArrayList<>();
        
        int b3Start = pdfText.indexOf("Part B3");
        if (b3Start == -1) b3Start = pdfText.indexOf("B3-");
        if (b3Start == -1) b3Start = pdfText.indexOf("Part B3-");
        if (b3Start == -1) b3Start = pdfText.indexOf("PartB3");
        if (b3Start == -1) {
            log.warn("Part B3 section not found in PDF text");
            return payments;
        }
        
        int b3End = pdfText.indexOf("Part B4", b3Start);
        if (b3End == -1) b3End = pdfText.indexOf("B4-", b3Start);
        if (b3End == -1) b3End = pdfText.indexOf("Part B7", b3Start);
        if (b3End == -1) b3End = Math.min(b3Start + 10000, pdfText.length());
        
        String b3Text = pdfText.substring(b3Start, b3End);
        log.debug("Part B3 text length: {} chars, full text:\n{}", b3Text.length(), b3Text);
        
        if (b3Text.contains("No Transactions Present") || b3Text.contains("No Transaction")) {
            log.debug("Part B3 contains 'No Transactions Present'");
            return payments;
        }
        
        // The actual format has "Self" on one line and "Assessment" on another line after "(Other than Companies)"
        // Example: "1 2024-25 Income Tax Self 37,772 0 1,511 0 39,283 0002271 31/08/2025 34945 25083100181019SBIN"
        //          "(Other than Assessment"
        //          "Companies)"
        Pattern taxPayPattern = Pattern.compile(
            "(\\d+)\\s+" +                                    // SR. NO.
            "(\\d{4}-\\d{2})\\s+" +                          // FINANCIAL YEAR
            "Income\\s+Tax\\s+" +                            // MAJOR HEAD start
            "(?:[^\\n]*?\\s+)?" +                            // Optional text like "(Other than Companies)"
            "Self\\s+" +                                      // "Self" part of "Self Assessment"
            "([\\d,]+)\\s+" +                                // TAX (A)
            "(\\d+)\\s+" +                                   // SURCHARGE (B)
            "([\\d,]+)\\s+" +                                // EDUCATION CESS (C)
            "(\\d+)\\s+" +                                   // OTHERS (D)
            "([\\d,]+)\\s+" +                                // TOTAL
            "(\\d{5,})\\s+" +                                // BSR CODE
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +                  // DATE OF DEPOSIT
            "(\\d{5,})",                                     // CHALLAN SERIAL NUMBER
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = taxPayPattern.matcher(b3Text);
        while (m.find()) {
            TaxPaymentAIS payment = new TaxPaymentAIS();
            payment.setFinancialYear(m.group(2));
            payment.setMinorHead("Self Assessment"); // Hardcode since it's split in PDF
            payment.setTaxAmount(parseAmountLong(m.group(3)));
            payment.setSurcharge(parseAmountLong(m.group(4)));
            payment.setEducationCess(parseAmountLong(m.group(5)));
            payment.setTotalAmount(parseAmountLong(m.group(7)));
            payment.setBsrCode(m.group(8));
            payment.setDepositDate(parseDate(m.group(9)));
            payment.setChallanSerialNo(m.group(10));
            payments.add(payment);
            log.debug("Found tax payment: FY={}, type={}, amount={}, BSR={}, challan={}", 
                payment.getFinancialYear(), payment.getMinorHead(), payment.getTotalAmount(),
                payment.getBsrCode(), payment.getChallanSerialNo());
        }
        
        log.debug("Part B3 extraction complete: {} tax payments found", payments.size());
        return payments;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return BigDecimal.ZERO;
        String cleaned = amountStr.replaceAll("[,\\s]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private long parseAmountLong(String amountStr) {
        return parseAmount(amountStr).longValue();
    }

    private LocalDate parseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
                return LocalDate.parse(dateStr, formatter2);
            } catch (Exception e2) {
                log.warn("Failed to parse date: {}", dateStr);
                return LocalDate.now();
            }
        }
    }
}
