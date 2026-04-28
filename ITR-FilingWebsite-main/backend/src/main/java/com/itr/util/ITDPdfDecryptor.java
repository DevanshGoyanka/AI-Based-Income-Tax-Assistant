package com.itr.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility for decrypting ITD PDFs (26AS, AIS, TIS)
 * Password format: {pan_lowercase}{ddmmyyyy}
 * Example: PAN = ACUPG3482G, DOB = 14-Jun-1974 → Password = "acupg3482g14061974"
 */
@Component
public class ITDPdfDecryptor {

    /**
     * Generate ITD password from PAN and DOB
     * @param pan PAN (will be converted to lowercase)
     * @param dob Date of Birth
     * @return Password string
     */
    public String generateITDPassword(String pan, LocalDate dob) {
        String panLower = pan.toLowerCase().trim();
        String dobStr = dob.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        return panLower + dobStr;
    }

    /**
     * Decrypt PDF and extract text
     * @param pdfBytes PDF file bytes
     * @param password Decryption password
     * @return Extracted text content
     * @throws IOException if decryption or extraction fails
     */
    public String decryptAndExtractText(byte[] pdfBytes, String password) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes);
             PDDocument doc = PDDocument.load(bais, password, 
                MemoryUsageSetting.setupTempFileOnly())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    /**
     * Decrypt PDF using PAN and DOB, then extract text
     * @param pdfBytes PDF file bytes
     * @param pan PAN
     * @param dob Date of Birth
     * @return Extracted text content
     * @throws IOException if decryption or extraction fails
     */
    public String decryptAndExtractText(byte[] pdfBytes, String pan, LocalDate dob) 
            throws IOException {
        String password = generateITDPassword(pan, dob);
        return decryptAndExtractText(pdfBytes, password);
    }
}
