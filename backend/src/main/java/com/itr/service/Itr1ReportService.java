package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.*;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class Itr1ReportService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##,##0");
    private static final DeviceRgb HDR_BG = new DeviceRgb(220, 230, 242);
    private static final DeviceRgb SEC_BG = new DeviceRgb(240, 245, 255);

    public byte[] generatePdfReport(Itr1FormData formData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(36, 36, 36, 36);

            addHeader(doc, formData);
            addStatementOfIncome(doc, formData);
            addSchedules(doc, formData);
            addSignatureBlock(doc, formData);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document doc, Itr1FormData f) {
        var info = f.getPersonalInfo();
        if (info == null) return;

        String name    = safe(info.getAssesseeName());
        String pan     = safe(info.getPan());
        String aadhaar = safe(info.getAadhaar());
        String dob     = info.getDateOfBirth() != null
            ? info.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "";
        String fy      = safe(info.getFinancialYear(), "2024-2025");
        String ay      = safe(info.getAssessmentYear(), "2025-26");
        String status  = "Individual \nResident";

        doc.add(para("A.Y. " + ay, 10, false).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(4));
        
        Table t = new Table(UnitValue.createPercentArray(new float[]{15, 35, 20, 30}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        t.addCell(nb(para("Name :", 10, false)));
        t.addCell(nb(para(name, 10, false)));
        t.addCell(nb(para("Previous Year : " + fy, 10, true)));
        t.addCell(nb(para("")));
        
        t.addCell(nb(para("PAN :", 10, false)));
        t.addCell(nb(para(pan, 10, false)));
        t.addCell(nb(para("Aadhaar No. : " + formatAadhaar(aadhaar), 10, true)));
        t.addCell(nb(para("")));
        
        t.addCell(nb(para("Date of Birth :", 10, false)));
        t.addCell(nb(para(dob, 10, false)));
        t.addCell(nb(para("Status : " + status, 10, true)));
        t.addCell(nb(para("")));
        
        doc.add(t);
    }
    
    private String formatAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) return aadhaar;
        return aadhaar.substring(0, 4) + " " + aadhaar.substring(4, 8) + " " + aadhaar.substring(8, 12);
    }

    private void addStatementOfIncome(Document doc, Itr1FormData f) {
        doc.add(para("Statement of Income", 12, true).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(para("Tax u/s 115BAC (New Regime)", 10, false).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 8, 21, 21})).setWidth(UnitValue.createPercentValue(100));
        
        t.addCell(hc("Particulars"));
        t.addCell(hc("Sch."));
        t.addCell(hc("Taxable"));
        t.addCell(hc("Exempt"));

        // Get data - EXACT same paths as dashboard
        var sal = f.getSalaryIncome();
        var comp = f.getTaxComputation();
        
        // Calculate salary values - use same logic as calculator/dashboard
        double salaryIncome = 0;
        double grossSalary = 0;
        double stdDed = 75000;
        
        log.info("PDF Debug - SalaryIncome object: sal={}", sal != null ? "exists" : "null");
        
        if (sal != null) {
            // Log raw values
            double basic = getDoubleValue(sal.getBasicSalary());
            double da = getDoubleValue(sal.getDaAmount());
            double hra = getDoubleValue(sal.getHraReceived());
            double lta = getDoubleValue(sal.getLtaReceived());
            double otherAlw = getDoubleValue(sal.getOtherAllowance());
            double computedNet = getDoubleValue(sal.getIncomeFromSalary());
            double computedGross = getDoubleValue(sal.getGrossSalary());
            stdDed = getDoubleValue(sal.getStandardDeduction());
            if (stdDed == 0) stdDed = 75000;
            
            log.info("PDF Debug - Raw: basic={}, da={}, hra={}, lta={}, other={}", basic, da, hra, lta, otherAlw);
            log.info("PDF Debug - Computed: computedNet={}, computedGross={}, stdDed={}", computedNet, computedGross, stdDed);
            
            // Use computed values if available, otherwise calculate from raw components
            if (computedNet > 0) {
                salaryIncome = computedNet;
                grossSalary = computedGross;
            } else if (computedGross > 0) {
                grossSalary = computedGross;
                salaryIncome = Math.max(0, grossSalary - stdDed);
            } else if (basic > 0) {
                // Calculate from raw components
                grossSalary = basic + da + hra + lta + otherAlw;
                salaryIncome = Math.max(0, grossSalary - stdDed);
            }
        }
        
        // If still 0, try TaxComputation
        if (salaryIncome == 0 && comp != null) {
            salaryIncome = getDoubleValue(comp.getIncomeFromSalary());
            grossSalary = salaryIncome + stdDed;
        }
        
        log.info("PDF Debug - FINAL: salaryIncome={}, grossSalary={}", salaryIncome, grossSalary);
        
        // Other sources - same approach
        var os = f.getOtherSourcesIncome();
        double otherSourcesIncome = 0;
        if (comp != null) {
            otherSourcesIncome = getDoubleValue(comp.getIncomeFromOtherSources());
        }
        if (otherSourcesIncome == 0 && os != null) {
            otherSourcesIncome = getDoubleValue(os.getTotalOtherSourcesIncome());
        }
        
        double totalIncome = (comp != null) ? getDoubleValue(comp.getTotalIncome()) : 0;
        if (totalIncome == 0) {
            totalIncome = salaryIncome + otherSourcesIncome;
        }
        log.info("PDF Debug - totalIncome (before TaxComputation) = {}", totalIncome);
        
        double totalTax = (comp != null) ? getDoubleValue(comp.getTotalTaxLiability()) : 0;
        double cess = (comp != null) ? getDoubleValue(comp.getCess()) : 0;
        double totalTDS = (comp != null) ? getDoubleValue(comp.getTotalTDS()) : 0;
        
        log.info("PDF Debug - comp totalIncome={}, totalTaxLiability={}, cess={}", 
            (comp != null ? comp.getTotalIncome() : "null"), totalTax, cess);
        
        // Try to get TDS from TaxPayments too
        var pay = f.getTaxPayments();
        if (totalTDS == 0 && pay != null) {
            if (pay.getTdsOnSalary() != null) {
                for (TDSOnSalary tds : pay.getTdsOnSalary()) totalTDS += getDoubleValue(tds.getTdsAmount());
            }
            if (pay.getTdsOnOther() != null) {
                for (TDSOnOther tds : pay.getTdsOnOther()) totalTDS += getDoubleValue(tds.getTdsAmount());
            }
        }

        // Fix gross salary calculation - use proper precedence
        double calcGrossSalary = grossSalary;
        if (calcGrossSalary == 0 && salaryIncome > 0) {
            calcGrossSalary = salaryIncome + stdDed;
        }
        
        // Get employer info
        String employerName = "";
        if (sal != null && sal.getEmployerName() != null) {
            employerName = sal.getEmployerName();
        }
        List<TDSOnSalary> tdsSalList = (pay != null) ? pay.getTdsOnSalary() : null;

        // SALARY SECTION
        t.addCell(new Cell(1,4).add(para("n  Income from Salaries", 10, true)).setPadding(4));
        
        // Show employers
        if (tdsSalList != null && !tdsSalList.isEmpty()) {
            int empNum = 1;
            for (TDSOnSalary tdsSalEntry : tdsSalList) {
                String emp = safe(tdsSalEntry.getEmployerName(), "Employer-" + empNum);
                t.addCell(lc("Employer-" + empNum + ": " + emp));
                t.addCell(cc("1"));
                t.addCell(rc(0));  // employer row - 0 in taxable
                t.addCell(ec());
                empNum++;
            }
        } else if (!employerName.isEmpty()) {
            t.addCell(lc("Employer: " + employerName));
            t.addCell(cc("1"));
            t.addCell(rc(0));
            t.addCell(ec());
        }
        
        t.addCell(lc("Salaries, allowances and perquisites"));
        t.addCell(cc("2"));
        t.addCell(rc(calcGrossSalary));  // Gross salary in taxable column
        t.addCell(ec());  // exempt column - empty
        
        t.addCell(lc("Standard deduction u/s 16(ia)"));
        t.addCell(ec());
        t.addCell(ec());  // empty taxable
        t.addCell(rc(stdDed));  // Deduction in exempt column - reduces gross to get net
        
        t.addCell(blc("Income chargeable under the head \"Salaries\""));
        t.addCell(ec());
        t.addCell(rc(salaryIncome));  // Net taxable salary - in taxable column!
        t.addCell(ec());

        // OTHER SOURCES SECTION
        double interest = getInterestIncome(os);
        double dividend = getDividendIncome(os);
        if (otherSourcesIncome > 0 || interest > 0 || dividend > 0) {
            t.addCell(new Cell(1,4).add(para("n  Income from Other Sources", 10, true)).setPadding(4));
            
            if (dividend > 0) {
                t.addCell(lc("Dividends"));
                t.addCell(cc("3"));
                t.addCell(rc(dividend));
                t.addCell(ec());
            }
            
            if (interest > 0) {
                t.addCell(lc("Interest income"));
                t.addCell(cc("4"));
                t.addCell(rc(interest));
                t.addCell(ec());
            }
            
            t.addCell(blc("Income chargeable under head \"Other Sources\""));
            t.addCell(ec());
            t.addCell(rc(otherSourcesIncome));  // FIX: Added value to taxable column
            t.addCell(ec());
        }

        // TOTAL INCOME
        t.addCell(new Cell(1,4).add(para("n  Total Income", 10, true)).setPadding(4));
        
        t.addCell(lc("Total income rounded off u/s 288A"));
        t.addCell(ec());
        t.addCell(rc(Math.round(totalIncome)));  // FIX: Value to taxable
        t.addCell(ec());
        
        // TAX CALCULATION
        double taxWithCess = totalTax;
        
        t.addCell(lc("Tax on total income"));
        t.addCell(ec());
        t.addCell(rc(Math.round(totalTax - cess)));  // FIX: Value to taxable
        t.addCell(ec());
        
        t.addCell(lc("Add: Cess (4%)"));
        t.addCell(ec());
        t.addCell(rc(Math.round(cess)));  // FIX: Value to taxable  
        t.addCell(ec());
        
        t.addCell(blc("Tax with cess"));
        t.addCell(ec());
        t.addCell(rc(Math.round(taxWithCess)));  // FIX: Value to taxable
        t.addCell(ec());
        
        // TDS
        if (totalTDS > 0) {
            t.addCell(lc("TDS / TCS"));
            t.addCell(cc("5"));
            t.addCell(ec());
            t.addCell(rc(totalTDS));
        }
        
        // Advance Tax - get from TaxPayments
        double advanceTax = 0;
        double selfTax = 0;
        if (pay != null) {
            advanceTax = getDoubleValue(pay.getTotalAdvanceTax());
            selfTax = getDoubleValue(pay.getTotalSelfAssessmentTax());
        }
        
        if (advanceTax > 0) {
            t.addCell(lc("Advance Tax Paid"));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(advanceTax));
        }
        
        if (selfTax > 0) {
            t.addCell(lc("Self-assessment tax paid"));
            t.addCell(cc("6"));
            t.addCell(ec());
            t.addCell(rc(selfTax));
        }
        
        // BALANCE - taxWithCess - totalTDS - advanceTax - selfTax
        double balance = taxWithCess - totalTDS - advanceTax - selfTax;
        if (balance != 0) {
            String label = balance > 0 ? "n  Balance Tax Payable" : "n  Refund";
            t.addCell(new Cell(1,3).add(para(label, 10, true)).setPadding(4));
            t.addCell(brc(Math.abs(balance)));
        }

        doc.add(t);
        doc.add(new Paragraph(" ").setFontSize(8));
    }

    private double getDoubleValue(Double val) { return val != null ? val : 0; }
    
    private double getInterestIncome(OtherSourcesIncome os) {
        if (os == null) return 0;
        return getDoubleValue(os.getSavingsAccountInterest()) + getDoubleValue(os.getFixedDepositInterest())
             + getDoubleValue(os.getRecurringDepositInterest()) + getDoubleValue(os.getNscInterest())
             + getDoubleValue(os.getScssInterest()) + getDoubleValue(os.getPostOfficeInterest())
             + getDoubleValue(os.getOtherInterest());
    }
    
    private double getDividendIncome(OtherSourcesIncome os) {
        if (os == null) return 0;
        return getDoubleValue(os.getDividendFromShares()) + getDoubleValue(os.getDividendFromMutualFunds())
             + getDoubleValue(os.getDividendFromUnits());
    }

    private void addSchedules(Document doc, Itr1FormData f) {
        var sal = f.getSalaryIncome();
        var os = f.getOtherSourcesIncome();
        var pay = f.getTaxPayments();
        var comp = f.getTaxComputation();

        doc.add(new com.itextpdf.layout.element.AreaBreak());
        
        var info = f.getPersonalInfo();
        if (info != null) {
            doc.add(para(safe(info.getAssesseeName()) + " Asst year: " + safe(info.getAssessmentYear(), "2025-26"), 10, false).setMarginBottom(8));
        }

        // Use EXACT same logic as dashboard/calculator to get salary values
        double salaryIncome = 0;
        double grossSalary = 0;
        double stdDed = 75000;
        
        // Priority 1: SalaryIncome.incomeFromSalary (same as ITR1CalculatorService)
        if (sal != null) {
            salaryIncome = getDoubleValue(sal.getIncomeFromSalary());
            grossSalary = getDoubleValue(sal.getGrossSalary());
            stdDed = getDoubleValue(sal.getStandardDeduction());
            if (stdDed == 0) stdDed = 75000;
            if (salaryIncome == 0 && grossSalary > 0) {
                salaryIncome = Math.max(0, grossSalary - stdDed);
            }
        }
        // Priority 2: TaxComputation
        if (salaryIncome == 0 && comp != null) {
            salaryIncome = getDoubleValue(comp.getIncomeFromSalary());
        }
        if (grossSalary == 0) grossSalary = salaryIncome + stdDed;
        double taxableSalary = salaryIncome;

        // SCHEDULE 1 - Employer Details from TDS list
        List<TDSOnSalary> tdsSalList = (pay != null) ? pay.getTdsOnSalary() : null;
        
        if (tdsSalList != null && !tdsSalList.isEmpty()) {
            int scheduleNum = 1;
            for (TDSOnSalary tds : tdsSalList) {
                doc.add(para("Schedule " + scheduleNum, 10, true).setMarginBottom(2));
                doc.add(para("Employer Details", 10, true).setMarginBottom(4));
                Table et = new Table(UnitValue.createPercentArray(new float[]{30, 70})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
                et.addCell(lc("Name"));
                et.addCell(lc(safe(tds.getEmployerName())));
                et.addCell(lc("TAN"));
                et.addCell(lc(safe(tds.getTan())));
                et.addCell(lc("Nature of Employment"));
                et.addCell(lc("Others"));
                doc.add(et);
                scheduleNum++;
            }
        } else if (sal != null && sal.getEmployerName() != null) {
            doc.add(para("Schedule 1", 10, true).setMarginBottom(2));
            doc.add(para("Employer Details", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{30, 70})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t.addCell(lc("Name"));
            t.addCell(lc(safe(sal.getEmployerName())));
            t.addCell(lc("TAN"));
            t.addCell(lc(safe(sal.getEmployerTAN())));
            t.addCell(lc("Nature of Employment"));
            t.addCell(lc("Others"));
            doc.add(t);
        }

        // SCHEDULE 2 - Salary
        doc.add(para("Schedule 2", 10, true).setMarginBottom(2));
        doc.add(para("Salary Income", 10, true).setMarginBottom(4));
        Table st = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);
        st.addCell(hc("Particulars"));
        st.addCell(hc("Exempt"));
        st.addCell(hc("Taxable"));
        
        st.addCell(lc("Salary u/s 17(1)"));
        st.addCell(ec());
        st.addCell(rc(grossSalary));
        
        st.addCell(blc("Total"));
        st.addCell(ec());
        st.addCell(brc(grossSalary));
        doc.add(st);
        
        Table st2 = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        st2.addCell(hc("Summary of Salary"));
        st2.addCell(hc("Gross"));
        st2.addCell(hc("Taxable"));
        st2.addCell(lc("Salary income"));
        st2.addCell(rc(grossSalary));
        st2.addCell(rc(taxableSalary));
        doc.add(st2);

        // SCHEDULE 3 - Dividends
        double totDiv = getDividendIncome(os);
        if (totDiv > 0) {
            doc.add(para("Schedule 3", 10, true).setMarginBottom(2));
            doc.add(para("Dividends taxable at Normal rate", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t.addCell(lc("Domestic Company Dividend"));
            t.addCell(rc(totDiv));
            t.addCell(blc("Total Dividends"));
            t.addCell(brc(totDiv));
            doc.add(t);
        }

        // SCHEDULE 4 - Interest
        double totInt = getInterestIncome(os);
        if (totInt > 0) {
            doc.add(para("Schedule 4", 10, true).setMarginBottom(2));
            doc.add(para("Interest Income", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            double sbInt = getDoubleValue(os != null ? os.getSavingsAccountInterest() : 0);
            double fdInt = getDoubleValue(os != null ? os.getFixedDepositInterest() : 0);
            if (sbInt > 0) { t.addCell(lc("Saving Bank Interest")); t.addCell(rc(sbInt)); }
            if (fdInt > 0) { t.addCell(lc("Fixed Deposit Interest")); t.addCell(rc(fdInt)); }
            doc.add(t);
        }

        // SCHEDULE 5 - TDS
        if (tdsSalList != null && !tdsSalList.isEmpty()) {
            doc.add(para("Schedule 5", 10, true).setMarginBottom(2));
            doc.add(para("TDS Details", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            double totTds = 0;
            for (TDSOnSalary tds : tdsSalList) {
                String tan = safe(tds.getTan());
                double amt = getDoubleValue(tds.getTdsAmount());
                String empName = safe(tds.getEmployerName());
                t.addCell(lc("TDS on Salary - " + empName + " (TAN: " + tan + ")"));
                t.addCell(rc(amt));
                totTds += amt;
            }
            t.addCell(blc("Total"));
            t.addCell(brc(totTds));
            doc.add(t);
        }
    }

    private void addSignatureBlock(Document doc, Itr1FormData f) {
        doc.add(new Paragraph(" ").setFontSize(12));
        var info = f.getPersonalInfo();
        String sigName = info != null ? "(" + safe(info.getAssesseeName()) + ")" : "";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50})).setWidth(UnitValue.createPercentValue(100));
        t.addCell(nb(para("Date : " + date, 9, false)));
        t.addCell(nb(para(sigName, 9, false).setTextAlignment(TextAlignment.RIGHT)));
        doc.add(t);
    }

    private Paragraph para(String text, int size, boolean bold) {
        Paragraph p = new Paragraph(text != null ? text : "").setFontSize(size);
        if (bold) p.setBold();
        return p;
    }
    private Paragraph para(String text) { return new Paragraph(text != null ? text : "").setFontSize(9); }
    private Cell nb(Paragraph p) { return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(2); }
    private Cell hc(String text) {
        return new Cell().add(para(text, 9, true)).setBackgroundColor(HDR_BG).setPadding(4).setTextAlignment(TextAlignment.CENTER);
    }
    private Cell lc(String text) { return new Cell().add(para(text, 9, false)).setPadding(3); }
    private Cell blc(String text) { return new Cell().add(para(text, 9, true)).setPadding(3); }
    private Cell cc(String text) {
        return new Cell().add(para(text, 9, false)).setPadding(3).setTextAlignment(TextAlignment.CENTER);
    }
    private Cell ec() { return new Cell().add(para("0", 9, false)).setPadding(3).setTextAlignment(TextAlignment.RIGHT); }
    private Cell rc(double amount) {
        return new Cell().add(para(formatCurrency(amount), 9, false)).setPadding(3).setTextAlignment(TextAlignment.RIGHT);
    }
    private Cell brc(double amount) {
        return new Cell().add(para(formatCurrency(amount), 9, true)).setPadding(3).setTextAlignment(TextAlignment.RIGHT);
    }
    private String safe(String s) { return s != null ? s : ""; }
    private String safe(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
    private String formatCurrency(double amount) {
        if (amount == 0) return "0";
        return CURRENCY_FORMAT.format(Math.round(amount));
    }
}
