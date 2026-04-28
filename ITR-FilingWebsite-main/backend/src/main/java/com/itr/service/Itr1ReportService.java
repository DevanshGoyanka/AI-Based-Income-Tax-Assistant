package com.itr.service;

import com.itr.dto.Itr1FormData;
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
        int age        = info.getAge();
        String status  = "Individual \nResident";

        // Right-aligned A.Y.
        doc.add(para("A.Y. " + ay, 10, false).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(4));
        
        // Name and details table
        Table t = new Table(UnitValue.createPercentArray(new float[]{25, 40, 35}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        t.addCell(nb(para("Name :", 10, false)));
        t.addCell(nb(para(name, 10, false)));
        t.addCell(nb(para("Previous Year : " + fy, 10, false)));
        
        t.addCell(nb(para("PAN :", 10, false)));
        t.addCell(nb(para(pan, 10, false)));
        t.addCell(nb(para("Aadhaar No. : " + formatAadhaar(aadhaar), 10, false)));
        
        t.addCell(nb(para("Date of Birth :", 10, false)));
        t.addCell(nb(para(dob, 10, false)));
        t.addCell(nb(para("Status : " + status, 10, false)));
        
        doc.add(t);
    }
    
    private String formatAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) return aadhaar;
        return aadhaar.substring(0, 4) + " " + aadhaar.substring(4, 8) + " " + aadhaar.substring(8, 12);
    }

    private void addStatementOfIncome(Document doc, Itr1FormData f) {
        doc.add(para("Statement of Income", 12, true)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(para("Tax u/s 115BAC (New Regime)", 10, false)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 8, 18, 18}))
            .setWidth(UnitValue.createPercentValue(100));
        
        // Header row
        t.addCell(hc("Particulars"));
        t.addCell(hc("Sch."));
        t.addCell(hc("Rs."));
        t.addCell(hc("Rs."));

        var sal  = f.getSalaryIncome();
        var os   = f.getOtherSourcesIncome();
        var comp = f.getTaxComputation();
        var pay  = f.getTaxPayments();

        // Salary Income
        if (sal != null && sal.getIncomeFromSalary() != 0) {
            t.addCell(new Cell(1,4).add(para("n  Income from Salaries", 10, true)).setPadding(4));
            
            if (sal.getEmployerName() != null) {
                t.addCell(lc("Employer: " + sal.getEmployerName()));
                t.addCell(cc("1"));
                t.addCell(ec());
                t.addCell(ec());
            }
            
            t.addCell(lc("Salaries, allowances and perquisites"));
            t.addCell(cc("2"));
            t.addCell(rc(sal.getGrossSalary()));
            t.addCell(ec());
            
            double stdDed = sal.getStandardDeduction() > 0 ? sal.getStandardDeduction() : 75000;
            t.addCell(lc("Standard deduction u/s 16(ia)"));
            t.addCell(ec());
            t.addCell(rc(stdDed));
            t.addCell(ec());
            
            t.addCell(lc("Income chargeable under the head \"Salaries\""));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(sal.getIncomeFromSalary()));
        }

        // Other Sources
        if (os != null && os.getTotalOtherSourcesIncome() > 0) {
            t.addCell(new Cell(1,4).add(para("n  Income from Other Sources", 10, true)).setPadding(4));
            
            if (os.getDividendFromShares() > 0) {
                t.addCell(lc("Dividends"));
                t.addCell(cc("3"));
                t.addCell(rc(os.getDividendFromShares()));
                t.addCell(ec());
            }
            
            double totalInterest = os.getSavingsAccountInterest() + os.getFixedDepositInterest();
            if (totalInterest > 0) {
                t.addCell(lc("Interest income"));
                t.addCell(cc("4"));
                t.addCell(rc(totalInterest));
                t.addCell(ec());
            }
            
            t.addCell(lc("Income chargeable under the head \"Other Sources\""));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(os.getTotalOtherSourcesIncome()));
        }

        // Total Income
        if (comp != null) {
            t.addCell(new Cell(1,4).add(para("n  Total Income", 10, true)).setPadding(4));
            
            t.addCell(lc("Total income rounded off u/s 288A"));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(comp.getTotalIncome()));
            
            double totalTax = comp.getTaxOnNormalIncome() + comp.getTotalCGTax();
            t.addCell(lc("Tax on total income"));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(totalTax));
            
            t.addCell(lc("Add: Cess (4%)"));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(comp.getCess()));
            
            double taxWithCess = totalTax + comp.getCess();
            t.addCell(lc("Tax with cess"));
            t.addCell(ec());
            t.addCell(ec());
            t.addCell(rc(taxWithCess));
            
            // Calculate total payments
            double totalPayments = 0;
            
            // TDS/TCS
            if (pay != null) {
                double totalTDS = pay.getTotalTDSOnSalary() + pay.getTotalTDSOnOther();
                if (totalTDS > 0) {
                    t.addCell(lc("TDS / TCS"));
                    t.addCell(cc("5"));
                    t.addCell(ec());
                    t.addCell(rc(totalTDS));
                    totalPayments += totalTDS;
                }
                
                // Advance tax
                if (pay.getTotalAdvanceTax() > 0) {
                    t.addCell(lc("Advance tax paid"));
                    t.addCell(ec());
                    t.addCell(ec());
                    t.addCell(rc(pay.getTotalAdvanceTax()));
                    totalPayments += pay.getTotalAdvanceTax();
                }
                
                // Self Assessment tax
                if (pay.getTotalSelfAssessmentTax() > 0) {
                    t.addCell(lc("Self Assessment tax paid"));
                    t.addCell(ec());
                    t.addCell(ec());
                    t.addCell(rc(pay.getTotalSelfAssessmentTax()));
                    totalPayments += pay.getTotalSelfAssessmentTax();
                }
            }
            
            // Balance calculation: Tax Liability - Total Payments
            double balance = taxWithCess - totalPayments;
            String balanceLabel = balance > 0 ? "n  Balance Tax Payable" : "n  Refund";
            t.addCell(new Cell(1,3).add(para(balanceLabel, 10, true)).setPadding(4));
            t.addCell(brc(Math.abs(balance)));
        }

        doc.add(t);
        doc.add(new Paragraph(" ").setFontSize(8));
    }
    
    private Cell cc(String text) {
        return new Cell().add(para(text, 9, false)).setPadding(3).setTextAlignment(TextAlignment.CENTER);
    }
    
    private Cell ec() {
        return new Cell().add(para("", 9, false)).setPadding(3);
    }

    private void addSchedules(Document doc, Itr1FormData f) {
        var sal = f.getSalaryIncome();
        var os  = f.getOtherSourcesIncome();
        var pay = f.getTaxPayments();

        // Page break for schedules
        doc.add(new com.itextpdf.layout.element.AreaBreak());
        
        // Header with name and AY
        var info = f.getPersonalInfo();
        if (info != null) {
            doc.add(para(safe(info.getAssesseeName()) + " Asst year: " + safe(info.getAssessmentYear(), "2025-26"), 10, false)
                .setMarginBottom(8));
        }

        // Schedule 1 - Employer Details
        if (sal != null && sal.getEmployerName() != null) {
            doc.add(para("Schedule 1", 10, true).setMarginBottom(2));
            doc.add(para("Employer Details", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t.addCell(lc("Name"));
            t.addCell(lc(sal.getEmployerName()));
            t.addCell(lc("TAN"));
            t.addCell(lc(safe(sal.getEmployerTAN())));
            t.addCell(lc("Nature of Employment"));
            t.addCell(lc("Others"));
            doc.add(t);
        }

        // Schedule 2 - Salary Income
        if (sal != null) {
            doc.add(para("Schedule 2", 10, true).setMarginBottom(2));
            doc.add(para("Salary Income", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);
            t.addCell(hc("Particulars"));
            t.addCell(hc("Exempt"));
            t.addCell(hc("Taxable"));
            
            double salaryAmt = sal.getGrossSalary();
            t.addCell(lc("Salary u/s 17(1)"));
            t.addCell(ec());
            t.addCell(rc(salaryAmt));
            
            t.addCell(blc("Total"));
            t.addCell(ec());
            t.addCell(brc(salaryAmt));
            doc.add(t);
            
            // Summary
            Table t2 = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t2.addCell(hc("Summary of Salary"));
            t2.addCell(hc("Gross"));
            t2.addCell(hc("Taxable"));
            t2.addCell(lc("Salary income"));
            t2.addCell(rc(salaryAmt));
            t2.addCell(rc(sal.getIncomeFromSalary()));
            doc.add(t2);
        }

        // Schedule 3 - Dividends
        if (os != null && os.getDividendFromShares() > 0) {
            doc.add(para("Schedule 3", 10, true).setMarginBottom(2));
            doc.add(para("Dividends taxable at Normal rate", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t.addCell(lc("Domestic Company Dividend"));
            t.addCell(rc(os.getDividendFromShares()));
            t.addCell(blc("Total Dividends"));
            t.addCell(brc(os.getDividendFromShares()));
            doc.add(t);
        }

        // Schedule 4 - Interest Income
        double totalInterest = os != null ? os.getSavingsAccountInterest() + os.getFixedDepositInterest() : 0;
        if (totalInterest > 0) {
            doc.add(para("Schedule 4", 10, true).setMarginBottom(2));
            doc.add(para("Interest Income", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            if (os.getSavingsAccountInterest() > 0) {
                t.addCell(lc("Saving Bank Interest"));
                t.addCell(rc(os.getSavingsAccountInterest()));
            }
            if (os.getFixedDepositInterest() > 0) {
                t.addCell(lc("Fixed Deposit Interest"));
                t.addCell(rc(os.getFixedDepositInterest()));
            }
            doc.add(t);
        }

        // Schedule 5 - TDS Details
        if (pay != null && pay.getTotalTDSOnSalary() > 0) {
            doc.add(para("Schedule 5", 10, true).setMarginBottom(2));
            doc.add(para("TDS Details", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            
            String tanInfo = "";
            if (pay.getTdsOnSalary() != null && !pay.getTdsOnSalary().isEmpty()) {
                tanInfo = " (TAN: " + safe(pay.getTdsOnSalary().get(0).getTan()) + ")";
            }
            t.addCell(lc("TDS on Salary" + tanInfo));
            t.addCell(rc(pay.getTotalTDSOnSalary()));
            doc.add(t);
        }

        // Schedule 6 - Advance Tax
        if (pay != null && pay.getTotalSelfAssessmentTax() > 0) {
            doc.add(para("Schedule 6", 10, true).setMarginBottom(2));
            doc.add(para("Self Assessment / Advance Tax Paid", 10, true).setMarginBottom(4));
            Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
            t.addCell(lc("Advance Tax"));
            t.addCell(rc(pay.getTotalSelfAssessmentTax()));
            doc.add(t);
        }
    }

    private void addSignatureBlock(Document doc, Itr1FormData f) {
        doc.add(new Paragraph(" ").setFontSize(12));
        var info = f.getPersonalInfo();
        String sigName = info != null ? "(" + safe(info.getAssesseeName()) + ")" : "";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100));
        t.addCell(nb(para("Date : " + date, 9, false)));
        t.addCell(nb(para(sigName, 9, false).setTextAlignment(TextAlignment.RIGHT)));
        doc.add(t);
    }

    private void sectionRow(Table t, String title) {
        t.addCell(new Cell(1,5).add(para(title, 10, true)).setBackgroundColor(HDR_BG).setPadding(4));
    }
    private void schedRow(Table t, String desc, String schNo, double subAmt) {
        t.addCell(lc(desc));
        t.addCell(new Cell().add(para(schNo, 9, false)).setPadding(3).setTextAlignment(TextAlignment.CENTER));
        t.addCell(subAmt > 0 ? rc(subAmt) : new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
    }
    private void descRow(Table t, String desc, double subAmt) {
        t.addCell(lc(desc));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(subAmt > 0 ? rc(subAmt) : new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
    }
    private void totalRow(Table t, String desc, double total, boolean bold) {
        t.addCell(bold ? blc(desc) : lc(desc));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("", 9, false)).setPadding(3));
        t.addCell(new Cell().add(para("n", 9, false)).setPadding(3).setTextAlignment(TextAlignment.CENTER));
        t.addCell(bold ? brc(total) : rc(total));
    }
    private void grandRow(Table t, String desc, double total) {
        t.addCell(new Cell(1,4).add(para(desc, 10, true)).setPadding(4));
        t.addCell(brc(total));
    }

    private Paragraph para(String text, int size, boolean bold) {
        Paragraph p = new Paragraph(text != null ? text : "").setFontSize(size);
        if (bold) p.setBold();
        return p;
    }
    private Cell nb(Paragraph p) { return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(2); }
    private Cell hc(String text) {
        return new Cell().add(para(text, 9, true)).setBackgroundColor(HDR_BG).setPadding(4).setTextAlignment(TextAlignment.CENTER);
    }
    private Cell lc(String text) { return new Cell().add(para(text, 9, false)).setPadding(3); }
    private Cell blc(String text) { return new Cell().add(para(text, 9, true)).setPadding(3); }
    private Cell rc(double amount) {
        return new Cell().add(para(formatCurrency(amount), 9, false)).setPadding(3).setTextAlignment(TextAlignment.RIGHT);
    }
    private Cell brc(double amount) {
        return new Cell().add(para(formatCurrency(amount), 9, true)).setPadding(3).setTextAlignment(TextAlignment.RIGHT);
    }
    private Paragraph schHdr(String text) {
        return para(text, 10, true).setMarginTop(8).setMarginBottom(3);
    }
    private Table simpleTable() {
        return new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);
    }
    private void addKV(Table t, String k, String v) {
        t.addCell(lc(k)); t.addCell(lc(v));
    }
    private void addKVn(Table t, String k, double v) {
        t.addCell(lc(k)); t.addCell(rc(v));
    }
    private String safe(String s) { return s != null ? s : ""; }
    private String safe(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
    private String formatCurrency(double amount) {
        if (amount == 0) return "-";
        return CURRENCY_FORMAT.format(Math.round(amount));
    }
}
