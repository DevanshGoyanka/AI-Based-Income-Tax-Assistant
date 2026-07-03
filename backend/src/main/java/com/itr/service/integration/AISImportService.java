package com.itr.service.integration;

import com.itr.dto.AISData;
import com.itr.dto.AISData.*;
import com.itr.util.ITDPdfDecryptor;
import com.itr.util.PIIMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AISImportService {

    private final ITDPdfDecryptor pdfDecryptor;
    private final PIIMaskingUtil piiMasking;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AISData importAIS(byte[] pdfBytes, String pan, LocalDate dob) throws IOException {
        String text = pdfDecryptor.decryptAndExtractText(pdfBytes, pan, dob);
        log.info("Starting AIS parsing for PAN: {}", piiMasking.maskPAN(pan));
        
        AISData data = AISData.builder()
                .pan(pan)
                .generalInfo(parsePartA(text))
                .partB1(parseB1(text))
                .partB2(parseB2(text))
                .partB3(parseB3(text))
                .tdsSalary(new ArrayList<>())
                .tdsOther(new ArrayList<>())
                .build();
        
        if (data.getGeneralInfo() != null) {
            log.info("AIS: {} B1={} B2Div={} B2Sale={} B2MF={} B3={}",
                data.getGeneralInfo().getName(),
                data.getPartB1() != null ? data.getPartB1().getTdsEntries().size() : 0,
                data.getPartB2() != null ? data.getPartB2().getDividendIncome() : 0,
                data.getPartB2() != null ? data.getPartB2().getSecuritiesSale().size() : 0,
                data.getPartB2() != null ? data.getPartB2().getMutualFundPurchase().size() : 0,
                data.getPartB3() != null ? data.getPartB3().size() : 0);
        }
        return data;
    }

    private AISGeneralInfo parsePartA(String text) {
        AISGeneralInfo info = new AISGeneralInfo();
        int end = text.indexOf("Part B");
        String a = text.substring(0, end > 0 ? Math.min(end, 2500) : Math.min(text.length(), 2500));
        Matcher m = Pattern.compile("[A-Z]{5}\\d{4}[A-Z]").matcher(a);
        if (m.find()) info.setPan(m.group());
        m = Pattern.compile("[A-Z]{5}\\d{4}[A-Z]\\s+(?:XXXX\\s+XXXX\\s+\\d+\\s+)?([A-Z][A-Za-z .]{3,40})").matcher(a);
        if (m.find()) info.setName(m.group(1).trim());
        m = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})").matcher(a);
        while (m.find()) { int y = Integer.parseInt(m.group(3)); if (y > 1900 && y < 2010) { try { info.setDob(LocalDate.parse(m.group(0), DTF)); break; } catch (Exception e) {} } }
        m = Pattern.compile("(?<!\\d)(\\d{10})(?!\\d)").matcher(a);
        if (m.find()) info.setMobile(m.group(1));
        m = Pattern.compile("[\\w.%-]+@[\\w.-]+\\.[A-Za-z]{2,}").matcher(a);
        if (m.find()) info.setEmail(m.group());
        return info;
    }

    private AISPartB1Data parseB1(String text) {
        List<AISTDSEntry> entries = new ArrayList<>();
        int b1 = findSec(text, "Part B1");
        int b2 = findSec(text, "Part B2");
        if (b1 < 0) return new AISPartB1Data();
        if (b2 < b1) b2 = text.length();
        
        String s = text.substring(b1, Math.min(b1 + 15000, b2));
        String[] lines = s.split("\\n");
        
        // Remove (Section XXX) artifacts
        s = s.replaceAll("\\(Section\\s+\\d+[A-Za-z]*\\)", "");
        lines = s.split("\\n");
        
        // State machine
        String curSection = null;
        String curName = null;
        String curTAN = null;
        long curAmt = 0;
        long curTDS = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("SR.") || line.startsWith("Part") || 
                line.startsWith("Note") || line.startsWith("Interest") || 
                line.startsWith("All") || line.startsWith("-")) continue;
            
            // Debug: log lines with TAN to see what state machine is processing
            if (line.contains("(NGPA") || line.contains("NGPA")) {
                log.info("SM DEBUG line[{}]: '{}'", i, piiMasking.sanitizeForLog(line));
            }
            
            // 1) Info code line: "1 TDS-194A ..." or "1 TDS-192 ..."
            Matcher infoM = Pattern.compile("^\\d+\\s+(TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(line);
            if (infoM.find()) {
                // Save previous entry if exists
                if (curName != null && curAmt > 0) {
                    entries.add(buildEntry(curSection, curName, curTAN, curAmt, curTDS));
                }
                curSection = infoM.group(2).replaceAll("[^\\dA-Za-z]", "");
                
                // If this SAME line also has a deductor name, capture state and extract amount
                // Strategy: extract SECTION from (Section XXX) bracket, extract NAME from between section bracket and (TAN)
                Matcher tanM = Pattern.compile("\\(([A-Z]{4}\\d{5}[A-Z])\\)").matcher(line);
                if (tanM.find() && !line.contains("Q(")) {
                    curTAN = tanM.group(1);
                    
                    // Extract section from (Section XXX) pattern in the line
                    int secStart = -1, secEnd = -1;
                    Matcher secM = Pattern.compile("\\((?:Section\\s*)?(\\d+[A-Za-z]*)\\)", Pattern.CASE_INSENSITIVE).matcher(line);
                    if (secM.find() && (curSection == null || "OTHER".equals(curSection))) {
                        curSection = secM.group(1).replaceAll("[^\\dA-Za-z]", "");
                        secStart = secM.start();
                        secEnd = secM.end();
                    }
                    
                    // Extract deductor name: text between section bracket and (TAN)
                    String nameText;
                    if (secStart >= 0 && secStart < tanM.start()) {
                        // section bracket is BEFORE (TAN) in this line
                        nameText = line.substring(secEnd, tanM.start()).trim();
                    } else {
                        // No section bracket in this line - use everything before (TAN) and strip TDS-XXX prefix
                        String beforeTAN = line.substring(0, tanM.start()).trim();
                        beforeTAN = beforeTAN.replaceFirst("^\\d+\\s+", "");
                        beforeTAN = beforeTAN.replaceFirst("(?i)^TDS-\\d+[A-Za-z]*\\s+", "");
                        beforeTAN = beforeTAN.replaceFirst("(?i)^TCS-\\d+[A-Za-z]*\\s+", "");
                        nameText = beforeTAN;
                    }
                    
                    // Extract deductor name: strip quoted text and stop words
                    String cleanName = extractDeductorName(nameText);
                    log.debug("B1 DEBUG extractDeductorName: '{}'", piiMasking.sanitizeForLog(nameText));
                    if (cleanName != null && cleanName.length() >= 3) {
                        curName = cleanName;
                    }
                    
                    // Extract amount from after (TAN): "... (TAN) CNT AMOUNT"
                    String after = line.substring(tanM.end()).replaceAll("\\r", " ").trim();
                    Matcher amM = Pattern.compile("(\\d+)\\s+([\\d,]+(?:\\.\\d{1,2})?)").matcher(after);
                    if (amM.find()) curAmt = pAmt(amM.group(2));
                    
                    curTDS = 0;
                    continue;
                }
                
                curName = null; curTAN = null; curAmt = 0; curTDS = 0;
                continue;
            }
            
            // 2) Deductor line: skip if we already processed this line in infoM
            if ("__PROCESSED__".equals(curSection)) {
                curSection = null; // reset for next entry
                continue;
            }
            
            // 2) Deductor line: find (TAN), extract section code from original line, deductor name is text after section code
            Matcher tanM = Pattern.compile("\\(([A-Z]{4}\\d{5}[A-Z])\\)").matcher(line);
            if (tanM.find() && !line.contains("Q(")) {
                log.debug("SM TAN MATCH: line[{}]", i);
                // Get text before (TAN), handle PDF carriage returns
                String before = line.substring(0, tanM.start()).replaceAll("\\r", " ").trim();
                before = before.replaceFirst("^\\d+\\s+", "");
                curTAN = tanM.group(1);
                
                // Extract section code from the ORIGINAL line
                Matcher secM = Pattern.compile("(?:TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(line);
                if (secM.find() && (curSection == null || "OTHER".equals(curSection))) {
                    curSection = secM.group(1).replaceAll("[^\\dA-Za-z]", "");
                }
                
                // Get deductor name: all text after the section code
                // IMPORTANT: Search for section code in the ORIGINAL line (not `before`)
                // because `before` starts from 0, so the section code IS in the line
                String cleanName = before;
                Matcher secInLine = Pattern.compile("(?:TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(line);
                if (secInLine.find()) {
                    // Find position of section code in line, take everything after it
                    int secPos = secInLine.start();
                    String afterSec = line.substring(secPos).trim();
                    // Remove section code itself
                    afterSec = afterSec.replaceFirst("^(?:TDS-|TCS-)\\d+[A-Za-z]*\\s+", "");
                    cleanName = afterSec;
                }
                
                // Remove description text and keep only the deductor name
                // IMPORTANT: Also handle \r (carriage returns from PDF) in cleanName
                cleanName = cleanName.replaceAll("\\r", " ")
                    .replaceFirst("(?i)^Interest\\s+other\\s+than\\s+\"[^\"]+\"\\s+received\\s+", "")
                    .replaceFirst("(?i)^TDS-\\d+[A-Za-z]*\\s+[A-Za-z ]+?\\s+", "")
                    .replaceFirst("(?i)^TCS-\\d+[A-Za-z]*\\s+[A-Za-z ]+?\\s+", "")
                    .replaceFirst("(?i)\\s+(received|from|paid|credited|deposited|reflected)\\s+(?=[A-Z])", " ")
                    .trim();
                
                if (cleanName.length() >= 3) {
                    if (curName != null && curAmt > 0) {
                        entries.add(buildEntry(curSection, curName, curTAN, curAmt, curTDS));
                    }
                    curName = cleanName;
                    
                    String after = line.substring(tanM.end()).replaceAll("\\r", " ").trim();
                    Matcher amM = Pattern.compile("(\\d+)\\s+([\\d,]+(?:\\.\\d{1,2})?)").matcher(after);
                    if (amM.find()) curAmt = pAmt(amM.group(2));
                    continue;
                }
            }
            
            // 3) Detail row: "1 Q3(Oct-Dec) 31/12/2025 15,000 1,500 1,500 Active"
            Matcher detM = Pattern.compile("^\\d+\\s+(Q[1-4]\\([A-Za-z-]+\\))\\s+(\\d{2}/\\d{2}/\\d{4})\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+(Active|Inactive)").matcher(line);
            if (detM.find()) {
                curTDS += pAmt(detM.group(4));
                continue;
            }
            
            // 4) Line starting with "(Section" -> skip
            if (line.startsWith("(Section")) continue;
        }
        
        // Save last entry if still pending
        if (curName != null && curAmt > 0) {
            entries.add(buildEntry(curSection, curName, curTAN, curAmt, curTDS));
        }
        
        // If still no entries, use line-by-line fallback
        if (entries.isEmpty()) {
            log.debug("B1 state machine found nothing, trying line-by-line fallback...");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim().replaceAll("\\r", " ");
                if (line.isEmpty()) continue;
                
                // Debug: log first few lines of each section to understand format
                if (line.contains("ANAND") || line.contains("TDS-194A")) {
                    log.debug("B1 DEBUG raw line[{}]", i);
                }
                
                String tan = null;
                int tanStart = -1;
                
                Matcher tanParen = Pattern.compile("\\(([A-Z]{4}\\d{5}[A-Z])\\)").matcher(line);
                if (tanParen.find()) {
                    tan = tanParen.group(1); tanStart = tanParen.start();
                } else {
                    Matcher tanLabel = Pattern.compile("\\bTAN:([A-Z]{4}\\d{5}[A-Z])\\b").matcher(line);
                    if (tanLabel.find()) { tan = tanLabel.group(1); tanStart = tanLabel.start(); }
                }
                if (tan == null) continue;
                
                // Extract section from current line AND surrounding lines
                String sec = "OTHER";
                Matcher secCurM = Pattern.compile("(?:TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(line);
                if (secCurM.find()) {
                    sec = secCurM.group(1).replaceAll("[^\\dA-Za-z]", "");
                }
                if ("OTHER".equals(sec)) {
                    for (int j = Math.max(0, i - 5); j < i; j++) {
                        Matcher sM = Pattern.compile("(?:TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(lines[j].trim());
                        if (sM.find()) { sec = sM.group(1).replaceAll("[^\\dA-Za-z]", ""); break; }
                    }
                }
                
                // Extract deductor name from text before (TAN)
                String beforeTAN = line.substring(0, tanStart).trim().replaceFirst("^\\d+\\s+", "");
                
                // Strategy: find the deductor name as the FIRST uppercase-starting word sequence
                // that appears AFTER all description words. Description words are lowercase/mixed.
                String cleanName = beforeTAN;
                
                // Try regex approach first (handles quoted text with any quote char)
                String beforeCleaned = beforeTAN
                    // Strip quoted description (any quote char: straight, curly, guillemet)
                    .replaceAll("(?i)^Interest\\s+other\\s+than\\s+[\\x22\\x201C\\x201D\\x00AB\\x00BB]+[^\\x22\\x201C\\x201D\\x00AB\\x00BB]+[\\x22\\x201C\\x201D\\x00AB\\x00BB]+\\s*", "")
                    // Strip TDS/TCS prefix + description
                    .replaceAll("(?i)^TDS-\\d+[A-Za-z]*\\s+[A-Za-z ]+?\\s+", "")
                    .replaceAll("(?i)^TCS-\\d+[A-Za-z]*\\s+[A-Za-z ]+?\\s+", "")
                    .trim();
                
                // If regex didn't help, use word-by-word extraction
                if (beforeCleaned.contains("other") || beforeCleaned.contains("Interest")) {
                    String[] words = beforeCleaned.split("\\s+");
                    StringBuilder sb = new StringBuilder();
                    boolean foundName = false;
                    for (int w = 0; w < words.length; w++) {
                        String word = words[w].replaceAll("[^\\p{L}\\p{N} ]", "");
                        if (word.isEmpty()) continue;
                        
                        // Stop at common description words
                        if (word.matches("(?i)^(other|than|from|received|paid|credited|on|the|and|of|in|for|with)$")) {
                            foundName = false; // reset if we hit description word
                            sb.setLength(0); // clear accumulated words
                            continue;
                        }
                        
                        // Name starts when we find a word starting with uppercase
                        if (Character.isUpperCase(word.charAt(0))) {
                            foundName = true;
                        }
                        
                        if (foundName) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(word);
                        }
                    }
                    if (sb.length() > 0) cleanName = sb.toString();
                } else {
                    cleanName = beforeCleaned;
                }
                
                // Extract amount after (TAN)
                long amt = 0;
                String afterTAN = line.substring(tanStart + tan.length() + 2).trim()
                    .replaceFirst("^TAN:\\s*", "");
                Matcher amM = Pattern.compile("(\\d+)\\s+([\\d,]+(?:\\.\\d{1,2})?)").matcher(afterTAN);
                if (amM.find()) amt = pAmt(amM.group(2));
                
                if (!cleanName.isEmpty() && cleanName.length() >= 3 && tan.length() == 10) {
                    entries.add(buildEntry(sec, cleanName, tan, amt, 0));
                    log.debug("B1(fb): '{}' TAN:{} Sec:{} Amt:{}", cleanName, piiMasking.maskTAN(tan), sec, amt);
                    break;
                }
            }
        }
        
        log.info("Part B1: {} entries", entries.size());
        return AISPartB1Data.builder().tdsEntries(entries).build();
    }

    private AISTDSEntry buildEntry(String s, String n, String t, long a, long td) {
        log.debug("B1 entry: deductor={} TAN={}", n, piiMasking.maskTAN(t));
        return AISTDSEntry.builder().section(s != null ? s : "OTHER").deductorName(n)
            .deductorTAN(t).totalAmountPaid(a).totalTDSDeducted(td).build();
    }
    
    /**
     * Extract deductor name from text before the TAN.
     * Strips known description patterns first, then extracts name words.
     */
    private String extractDeductorName(String beforeTAN) {
        // Step 1: strip TDS/TCS prefix only
        String cleaned = beforeTAN
            .replaceFirst("(?i)^TDS-\\d+[A-Za-z]*\\s+", "")
            .replaceFirst("(?i)^TCS-\\d+[A-Za-z]*\\s+", "")
            .trim();
        
        // Step 2: find "received" and take everything after it - that's the deductor name
        // This is the most reliable approach since "received" is consistently present in AIS
        int receivedIdx = cleaned.toLowerCase().indexOf("received");
        String nameText = cleaned;
        if (receivedIdx >= 0) {
            nameText = cleaned.substring(receivedIdx + "received".length()).trim();
        }
        
        // If no "received", try "from"
        if (nameText.isEmpty() || receivedIdx < 0) {
            int fromIdx = cleaned.toLowerCase().indexOf("from");
            if (fromIdx >= 0) {
                nameText = cleaned.substring(fromIdx + "from".length()).trim();
            }
        }
        
        // Step 3: clean up - remove any remaining description words and extra spaces
        nameText = nameText.replaceAll("[\\x22\\x201C\\x201D\\x00AB\\x00BB]", ""); // remove all quote chars
        nameText = nameText.replaceAll("(?i)^(on|than|other|interest)\\s+", "");
        nameText = nameText.replaceAll("\\s+", " ").trim();
        
        return nameText;
    }
    
    private String findSec(String ctx) {
        Matcher m = Pattern.compile("(?:TDS-|TCS-)(\\d+[A-Za-z]*)").matcher(ctx);
        if (m.find()) return m.group(1).replaceAll("[^\\dA-Za-z]", "");
        String u = ctx.toUpperCase();
        if (u.contains("SALARY") || ctx.contains("192")) return "192";
        if (ctx.contains("194A") || u.contains("INTEREST")) return "194A";
        if (ctx.contains("194C")) return "194C";
        if (ctx.contains("194J")) return "194J";
        if (ctx.contains("194H")) return "194H";
        if (ctx.contains("194I")) return "194I";
        if (ctx.contains("194D")) return "194D";
        if (ctx.contains("194")) return "194";
        return "OTHER";
    }

    private AISPartB2Data parseB2(String text) {
        AISPartB2Data b2 = new AISPartB2Data();
        b2.setDividendIncome(0); b2.setSecuritiesSale(new ArrayList<>());
        b2.setMutualFundPurchase(new ArrayList<>()); b2.setSecuritiesPurchaseAmount(0);
        
        int start = findSec(text, "Part B2");
        int end = findSec(text, "Part B3");
        if (end < start) end = findSec(text, "Part B7");
        if (end < start) end = text.length();
        if (start < 0) return b2;
        
        String sec = text.substring(start, Math.min(start + 20000, end));
        String flat = sec.replaceAll("\\s{2,}", " ");
        
        // Dividend - look for the actual transaction amount, not header numbers
        int di = sec.indexOf("Dividend");
        if (di >= 0) {
            // Find detail row: SR.NO DATE AMOUNT Active
            // Example: "1 08/04/2026 180 Active"
            Matcher dm = Pattern.compile("(?<!SR\\.\\s*NO\\.)(\\d{1,2})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+([\\d,]+(?:\\.\\d+)?)\\s+(Active|Inactive)").matcher(sec.substring(di, Math.min(sec.length(), di + 300)));
            while (dm.find()) {
                long divAmt = pAmt(dm.group(3));
                if (divAmt > 0) {
                    b2.setDividendIncome(Math.max(b2.getDividendIncome(), divAmt));
                    log.info("Div: ₹{}", divAmt);
                }
            }
        }
        
        // Securities Sale - strip ISIN codes first, then extract numbers
        String[] saleLines = sec.split("\\n");
        for (String saleLine : saleLines) {
            // Sale lines have a date, long text, and Active status
            if (saleLine.contains("/202") && saleLine.length() > 100) {
                try {
                    // Strip parenthetical codes (ISIN, etc.)
                    String clean = saleLine.replaceAll("\\([A-Z0-9]+\\)", " ");
                    clean = clean.replaceAll("\\s{2,}", " ");
                    
                    Matcher dtM = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})").matcher(clean);
                    if (!dtM.find()) continue;
                    String date = dtM.group(1);
                    String after = clean.substring(dtM.end());
                    
                    // Extract security name: text from after date up to a column keyword
                    String name = "";
                    Matcher nameM = Pattern.compile("^(.*?)(Listed Equity|Equity Share|Equity|Debt|Preference)").matcher(after);
                    if (nameM.find()) name = nameM.group(1).trim();
                    
                    // Extract numbers from the rest (after the asset type column)
                    List<BigDecimal> nums = new ArrayList<>();
                    Matcher nm = Pattern.compile("([\\d,]+(?:\\.\\d+)?)").matcher(after);
                    while (nm.find()) {
                        try { nums.add(new BigDecimal(nm.group(1).replace(",", ""))); } catch (Exception e) {}
                    }
                    
                    // After stripping ISIN, the numbers should be: QTY PRICE CONS COST FMV INDEXED 0
                    // For: RELIANCE ... Listed Equity Share Market Market Short term 1.00 1,523.45 1,523 1,517.00 961.15 961.15 0 Active
                    // nums: [1.00] [1,523.45] [1,523] [1,517.00] [961.15] [961.15] [0]
                    if (nums.size() >= 4 && nums.get(2).longValue() >= 100) {
                        String gt = clean.contains("Short") ? "STCG" : "LTCG";
                        b2.getSecuritiesSale().add(SFTSaleEntry.builder()
                            .transferDate(LocalDate.parse(date, DTF))
                            .securityName(name.replaceAll("\\s+", " ")).assetType(gt)
                            .quantity(nums.get(0)).salePricePerUnit(nums.get(1))
                            .salesConsideration(nums.get(2)).costOfAcquisition(nums.get(3)).build());
                        log.info("Sale: {} {} Qty:{} Cons:{} Cost:{}", name, gt, nums.get(0), nums.get(2), nums.get(3));
                    }
                } catch (Exception e) {
                    log.debug("Sale err: {}", e.getMessage());
                }
            }
        }
        
        // Securities Purchase - scan all B2 lines for amount near code
        if (sec.contains("SFT-17(Pur") || sec.contains("Purchase of securities")) {
            long maxAmt = 0;
            String[] b2lines = sec.split("\\n");
            for (int i = 0; i < b2lines.length; i++) {
                String line = b2lines[i].trim();
                if ((line.contains("SFT-17(Pur") || line.contains("Purchase of securities")) && !line.contains("SR.")) {
                    // Check this line and next 3 lines for amount with comma
                    for (int j = i; j < Math.min(i + 4, b2lines.length); j++) {
                        Matcher m2 = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?)").matcher(b2lines[j]);
                        long lastVal = 0;
                        while (m2.find()) { lastVal = pAmt(m2.group(1)); }
                        if (lastVal > 100) maxAmt = Math.max(maxAmt, lastVal);
                    }
                }
            }
            b2.setSecuritiesPurchaseAmount(maxAmt);
            if (maxAmt > 0) log.info("SecPur: ₹{}", maxAmt);
        }
        
        // MF Purchase
        Pattern mp = Pattern.compile("(Q[1-4]\\([A-Za-z-]+\\))\\s+([\\w-]+)\\s+([A-Z][A-Za-z0-9 &.,'()/-]{5,70}?)\\s+(First|Second|Third|Joint|Single)\\s+([\\d,]+(?:\\.\\d+)?)");
        Matcher mm = mp.matcher(flat);
        while (mm.find()) {
            String amc = mm.group(3).trim();
            long amt = pAmt(mm.group(5));
            if (amc.length() > 5 && amt > 0) {
                boolean exists = false;
                for (MFPurchase m : b2.getMutualFundPurchase()) {
                    if (amc.contains(m.getAmcName().substring(0, Math.min(10, m.getAmcName().length()))) || m.getAmcName().contains(amc.substring(0, Math.min(10, amc.length())))) { exists = true; break; }
                }
                if (!exists) { b2.getMutualFundPurchase().add(MFPurchase.builder().amcName(amc).totalPurchase(amt).build()); log.info("MF: {} ₹{}", amc, amt); }
            }
        }
        
        return b2;
    }

    private List<TaxPaymentAIS> parseB3(String text) {
        List<TaxPaymentAIS> payments = new ArrayList<>();
        int start = findSec(text, "Part B3");
        int end = findSec(text, "Part B4");
        if (end < start) end = findSec(text, "Part B7");
        if (end < start) end = text.length();
        if (start < 0) return payments;
        
        String sec = text.substring(start, Math.min(start + 5000, end));
        String flat = sec.replaceAll("\\s{2,}", " ");
        
        Pattern p = Pattern.compile("(\\d{4}-\\d{2})\\s+(Income Tax[^\\d]+?)\\s+([A-Za-z ]{3,40}?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+(\\d+)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+(\\d{7})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\S+)");
        Matcher m = p.matcher(flat);
        while (m.find()) {
            try { payments.add(TaxPaymentAIS.builder().financialYear(m.group(1)).minorHead(m.group(3).trim()).taxAmount(pAmt(m.group(4))).surcharge(pAmt(m.group(5))).educationCess(pAmt(m.group(6))).totalAmount(pAmt(m.group(8))).bsrCode(m.group(9)).depositDate(LocalDate.parse(m.group(10), DTF)).challanSerialNo(m.group(11)).build()); } catch (Exception e) {}
        }
        if (payments.isEmpty()) {
            Pattern p2 = Pattern.compile("(\\d{4}-\\d{2})\\s+([A-Za-z ]{10,50}?)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s+(\\d{7})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)");
            m = p2.matcher(flat);
            while (m.find()) { payments.add(TaxPaymentAIS.builder().financialYear(m.group(1)).minorHead(m.group(2).trim()).totalAmount(pAmt(m.group(3))).bsrCode(m.group(4)).depositDate(LocalDate.parse(m.group(5), DTF)).challanSerialNo(m.group(6)).build()); }
        }
        return payments;
    }

    private int findSec(String text, String sec) {
        for (String s : new String[]{"\n", "\r", " ", "-"}) {
            int i = text.indexOf(sec + s); if (i >= 0) return i;
        }
        int i = text.indexOf(sec); return i;
    }

    private long pAmt(String str) {
        if (str == null || str.trim().isEmpty()) return 0;
        try { return (long) Double.parseDouble(str.replace(",", "").replace("₹", "").trim()); } catch (Exception e) { return 0; }
    }
}
