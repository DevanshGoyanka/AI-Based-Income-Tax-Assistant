package com.itr.service;

import com.itr.dto.Form16PartBData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Form 16 Part B Parser - Employer salary computation
 * Extracts salary breakdown and deductions from PDF
 */
@Slf4j
@Service
public class Form16PartBParser {

    public Form16PartBData parse(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            
            if (text == null || text.trim().length() < 100) {
                throw new IOException("PDF appears to be empty or scanned - text extraction failed");
            }
            
            return parseFromText(text);
        }
    }

    private Form16PartBData parseFromText(String text) {
        Form16PartBData data = new Form16PartBData();

        // Salary components 17(1)
        data.setBasicSalary(extractAmount(text, "Basic Salary"));
        data.setDearnessAllowance(extractAmount(text, "Dearness Allowance|DA"));
        data.setHouseRentAllowance(extractAmount(text, "House Rent Allowance|HRA"));
        data.setLeaveTravelAllowance(extractAmount(text, "Leave Travel Allowance|LTA"));
        data.setLeaveEncashment(extractAmount(text, "Leave Encashment"));
        data.setGratuity(extractAmount(text, "Gratuity"));
        data.setBonus(extractAmount(text, "Bonus"));
        data.setCommission(extractAmount(text, "Commission"));
        data.setArrearsOfSalary(extractAmount(text, "Arrears"));
        data.setOtherAllowances(extractAmount(text, "Other Allowances"));
        data.setTotalSalary17_1(extractAmount(text, "Total.*?17\\(1\\)|Gross Salary"));

        // Perquisites and profits
        data.setTotalPerquisites17_2(extractAmount(text, "Perquisites.*?17\\(2\\)"));
        data.setTotalProfitsInLieu17_3(extractAmount(text, "Profits.*?lieu.*?17\\(3\\)"));
        data.setGrossSalary(extractAmount(text, "Gross Salary"));

        // Exemptions u/s 10
        data.setHraExemption10_13A(extractAmount(text, "HRA.*?10\\(13A\\)|House Rent.*?Exempt"));
        data.setLtaExemption10_5(extractAmount(text, "LTA.*?10\\(5\\)|Leave Travel.*?Exempt"));
        data.setGratuityExemption10_10(extractAmount(text, "Gratuity.*?10\\(10\\)|Gratuity.*?Exempt"));
        data.setLeaveEncashmentExemption10_10AA(extractAmount(text, "Leave Encash.*?10\\(10AA\\)"));
        data.setTotalExemptionsUnder10(extractAmount(text, "Total.*?Exempt.*?10|Total.*?u/s 10"));

        // Net salary
        data.setNetSalaryAfterExemptions(extractAmount(text, "Net Salary|Balance.*?Salary"));

        // Deductions u/s 16
        data.setStandardDeduction(extractAmount(text, "Standard Deduction"));
        data.setProfessionalTax(extractAmount(text, "Professional Tax|PT"));
        data.setEntertainmentAllowance(extractAmount(text, "Entertainment Allowance"));
        data.setTaxableIncomeSalary(extractAmount(text, "Income.*?Salary|Taxable Salary"));

        // Chapter VI-A deductions
        data.setDeduction80C(extractAmount(text, "80C"));
        data.setDeduction80CCC(extractAmount(text, "80CCC"));
        data.setDeduction80CCD_1(extractAmount(text, "80CCD\\(1\\)"));
        data.setDeduction80CCD_1B(extractAmount(text, "80CCD\\(1B\\)|NPS.*?1B"));
        data.setDeduction80CCD_2(extractAmount(text, "80CCD\\(2\\)|Employer.*?NPS"));
        data.setDeduction80D(extractAmount(text, "80D|Medical Insurance"));
        data.setDeduction80E(extractAmount(text, "80E|Education Loan"));
        data.setDeduction80G(extractAmount(text, "80G|Donation"));
        data.setTotalDeductionsVIA(extractAmount(text, "Total.*?VI-A|Total.*?Deduction"));

        // Tax computation
        data.setTaxPayableOnSalary(extractAmount(text, "Tax Payable|Tax.*?Income"));
        data.setReliefUnder89(extractAmount(text, "Relief.*?89\\(1\\)|Section 89"));
        data.setNetTaxPayable(extractAmount(text, "Net Tax Payable|Net Tax"));
        data.setTdsDeductedTotal(extractAmount(text, "TDS Deducted|Total TDS"));

        log.info("Parsed Form 16 Part B: Gross={}, TDS={}", 
                 data.getGrossSalary(), data.getTdsDeductedTotal());
        
        return data;
    }

    private long extractAmount(String text, String labelPattern) {
        Pattern p = Pattern.compile(
            "(?i)(?:" + labelPattern + ")[^0-9\\n]{0,30}(Rs\\.?\\s?)?([0-9][0-9,]*)",
            Pattern.DOTALL);
        Matcher m = p.matcher(text);
        
        if (m.find()) {
            String num = m.group(m.groupCount()).replaceAll("[^0-9]", "");
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                log.debug("Failed to parse amount for pattern: {}", labelPattern);
            }
        }
        return 0L;
    }
}
