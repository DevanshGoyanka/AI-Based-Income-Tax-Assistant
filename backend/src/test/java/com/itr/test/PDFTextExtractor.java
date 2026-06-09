package com.itr.test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.io.MemoryUsageSetting;

import java.io.File;
import java.io.IOException;

/**
 * Test to extract 26AS PDF text and understand its structure
 */
public class PDFTextExtractor {
    
    public static void main(String[] args) throws IOException {
        // 26AS for ACUPG3482G with DOB 14-06-1974
        String pdfPath = "C:/Users/Devansh/Desktop/ITR-FilingWebsite-main/import_prefill/ACUPG3482G-2025 (1).pdf";
        String password = "acupg3482g14061974";
        
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password, MemoryUsageSetting.setupTempFileOnly())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            String text = stripper.getText(doc);
            
            System.out.println("========== FULL PDF TEXT ==========");
            System.out.println(text);
            System.out.println("========== END ==========");
            System.out.println("Total length: " + text.length() + " chars");
            
            // Look for key sections
            System.out.println("\n========== SECTION ANALYSIS ==========");
            analyzeSections(text);
        }
    }
    
    private static void analyzeSections(String text) {
        String[] sectionMarkers = {
            "PART-A", "PART-A:",
            "PART-B", "PART B",
            "PART-C", "PART C",
            "PART-D", "PART D",
            "PART-E", "PART E",
            "PART-F", "PART F",
            "PART-X", "PART X",
            "Form 26AS", "Form26AS",
            "Tax Deduction", "TDS ",
            "Tax Collected", "TCS ",
            "Tax Paid", "Tax Payments",
            "Verification", "Verification Summary"
        };
        
        for (String marker : sectionMarkers) {
            int idx = text.indexOf(marker);
            if (idx != -1) {
                System.out.println("Found: '" + marker + "' at position " + idx);
            }
        }
        
        // Show first occurrence of each part
        System.out.println("\n========== SEARCHING FOR 'PART' ==========");
        for (int i = 0; i < text.length() - 10; i++) {
            if (text.substring(i).startsWith("PART")) {
                int end = Math.min(i + 100, text.length());
                System.out.println("At " + i + ": " + text.substring(i, end).replace("\n", " "));
                i += 50;
            }
        }
    }
}
