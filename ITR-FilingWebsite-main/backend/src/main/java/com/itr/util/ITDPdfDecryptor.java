package com.itr.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility to decrypt password-protected AIS/TIS PDFs from ITD portal
 * Password format: {pan_lowercase}{ddmmyyyy}
 * Example: aaaaa1234a21011991 (PAN: AAAAA1234A, DOB: 21/01/1991)
 */
@Component
@Slf4j
public class ITDPdfDecryptor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    /**
     * Generate ITD PDF password
     * @param pan PAN (will be converted to lowercase)
     * @param dob Date of birth or incorporation (ddMMyyyy format)
     * @return Password string
     */
    public String generatePassword(String pan, String dob) {
        if (pan == null || dob == null) {
            throw new IllegalArgumentException("PAN and DOB cannot be null");
        }
        return pan.toLowerCase() + dob;
    }

    /**
     * Generate ITD PDF password from LocalDate
     */
    public String generatePassword(String pan, LocalDate dob) {
        return generatePassword(pan, dob.format(DATE_FORMATTER));
    }

    /**
     * Decrypt and extract text from password-protected ITD PDF
     * @param pdfFile PDF file
     * @param pan PAN
     * @param dob Date of birth/incorporation
     * @return Extracted text content
     */
    public String decryptAndExtractText(File pdfFile, String pan, String dob) throws IOException {
        String password = generatePassword(pan, dob);
        
        try (PDDocument document = PDDocument.load(pdfFile, password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Successfully decrypted and extracted text from PDF: {}", pdfFile.getName());
            return text;
        } catch (IOException e) {
            log.error("Failed to decrypt PDF. Check if PAN and DOB are correct. Password format: {pan_lowercase}{ddmmyyyy}", e);
            throw new IOException("Failed to decrypt ITD PDF. Verify PAN and DOB are correct.", e);
        }
    }

    /**
     * Decrypt and extract text using LocalDate
     */
    public String decryptAndExtractText(File pdfFile, String pan, LocalDate dob) throws IOException {
        return decryptAndExtractText(pdfFile, pan, dob.format(DATE_FORMATTER));
    }

    /**
     * Validate if PDF can be decrypted with given credentials
     */
    public boolean validatePassword(File pdfFile, String pan, String dob) {
        String password = generatePassword(pan, dob);
        try (PDDocument document = PDDocument.load(pdfFile, password)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
