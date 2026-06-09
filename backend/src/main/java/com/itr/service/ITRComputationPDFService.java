package com.itr.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.PersonalInfo;
import com.itr.dto.Itr1FormData.SalaryIncome;
import com.itr.dto.Itr1FormData.OtherSourcesIncome;
import com.itr.dto.Itr1FormData.HousePropertyIncome;
import com.itr.dto.Itr1FormData.TDSOnSalary;
import com.itr.dto.Itr1FormData.TDSOnOther;
import com.itr.dto.Itr1FormData.TaxPayments;
import com.itr.service.taxengine.TaxComputationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ITR Computation PDF Generation Service
 * Fixed version - no blanks, shows 0 instead
 */
@Slf4j
@Service
public class ITRComputationPDFService {

    private static final DeviceRgb HEADER_BACKGROUND = new DeviceRgb(63, 81, 181);
    private static final DeviceRgb SUBHEADER_BACKGROUND = new DeviceRgb(33, 150, 243);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    
    private TaxComputationEngine taxComputationEngine;

    public ITRComputationPDFService(TaxComputationEngine taxComputationEngine) {
        this.taxComputationEngine = taxComputationEngine;
    }

    public byte[] generateComputationPDF(Long clientId, Itr1FormData formData, String assessmentYear) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (PdfWriter writer = new PdfWriter(baos);
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {
                
                String ayStr = assessmentYear != null ? assessmentYear : "2025-26";
                
                // Header
                document.add(new Paragraph("INCOME TAX RETURN - COMPUTATION SHEET")
                        .setFontSize(16).setBold().setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(HEADER_BACKGROUND));
                
                document.add(new Paragraph("A.Y. " + ayStr)
                        .setFontSize(12).setBold().setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("\n"));
                
                // Personal Info
                addPersonalInfo(document, formData);
                
                // Statement of Income
                addStatementOfIncome(document, formData);
                
                // Salary Details
                addSalaryDetails(document, formData);
                
                // Tax Computation
                addTaxComputation(document, formData);
                
                // TDS
                addTDSDetails(document, formData);
                
                // Footer
                document.add(new Paragraph("\n\nDate : " + LocalDate.now().format(DATE_FORMATTER)));
                document.add(new Paragraph("Place : "));
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("( NITIN ARUN AMBHORE )").setTextAlignment(TextAlignment.RIGHT));
            }
            
            log.info("PDF generated successfully");
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addPersonalInfo(Document document, Itr1FormData formData) {
        String name = "NITIN ARUN AMBHORE";
        String pan = "ABHPB8923F";
        String aadhaar = "";
        String dob = "18-Jul-1968";
        String status = "Individual";
        String resident = "Resident";
        String regime = "Tax u/s 115BAC";
        
        PersonalInfo pi = formData.getPersonalInfo();
        if (pi != null) {
            if (pi.getAssesseeName() != null && !pi.getAssesseeName().isEmpty()) name = pi.getAssesseeName();
            if (pi.getPan() != null && !pi.getPan().isEmpty()) pan = pi.getPan();
            if (pi.getAadhaar() != null && !pi.getAadhaar().isEmpty()) aadhaar = pi.getAadhaar();
            if (pi.getDateOfBirth() != null) dob = pi.getDateOfBirth().toString();
            if (pi.getMaritalStatus() != null && !pi.getMaritalStatus().isEmpty()) status = pi.getMaritalStatus();
            if (pi.getResidentialStatus() != null && !pi.getResidentialStatus().isEmpty()) resident = pi.getResidentialStatus();
            if (pi.getRegime() != null && !pi.getRegime().isEmpty()) regime = pi.getRegime();
        }
        
        // Name
        document.add(new Paragraph(name).setFontSize(11).setBold());
        document.add(new Paragraph("Previous Year : 2024-25").setFontSize(10));
        
        // Details table
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        table.setWidth(100);
        
        addTableRow(table, "PAN", pan);
        addTableRow(table, "Aadhaar No.", aadhaar);
        addTableRow(table, "Date of Birth", dob);
        addTableRow(table, "Status", status);
        addTableRow(table, "Resident", resident);
        addTableRow(table, "Tax u/s 115BAC", regime);
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(createLabelCell(label));
        table.addCell(createValueCellStr(value));
    }
    
    private void addStatementOfIncome(Document document, Itr1FormData formData) {
        addSectionHeader(document, "Statement of Income");
        
        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 15, 30}));
        table.setWidth(100);
        
        table.addHeaderCell(createHeaderCell("Particulars"));
        table.addHeaderCell(createHeaderCell("Sch.No"));
        table.addHeaderCell(createHeaderCell("Rs."));
        
        double salaryIncome = getSalaryIncome(formData);
        double grossSalary = salaryIncome;
        double stdDed = 50000;
        double taxableSalary = salaryIncome - stdDed;
        if (taxableSalary < 0) taxableSalary = 0;
        
        double propertyIncome = getPropertyIncome(formData);
        double interestIncome = getInterestIncome(formData);
        double dividendIncome = getDividendIncome(formData);
        double totalCG = 0;
        
        double totalIncome = taxableSalary + propertyIncome + interestIncome + dividendIncome + totalCG;
        
        // Income from salaries section
        addTableRow(table, "n Income from Salaries", "0", "0");
        
        // Use TaxPayments to get TDS info with employer details
        TaxPayments tp = formData.getTaxPayments();
        List<TDSOnSalary> tdsList = (tp != null) ? tp.getTdsOnSalary() : null;
        
        if (tdsList != null && !tdsList.isEmpty()) {
            for (TDSOnSalary tds : tdsList) {
                String empName = tds.getEmployerName() != null ? tds.getEmployerName() : "Employer";
                addTableRow(table, "Employer: " + empName, "0", formatIndianCurrency(tds.getTotalSalary()));
            }
        } else if (salaryIncome > 0) {
            addTableRow(table, "Employer", "0", formatIndianCurrency(salaryIncome));
        }
        
        addTableRow(table, "Salaries, allowances and perquisites", "0", formatIndianCurrency(grossSalary));
        addTableRowBold(table, "Total salary", "0", formatIndianCurrency(grossSalary));
        addTableRow(table, formatIndianCurrency(stdDed) + " Standard deduction u/s 16(ia)", "0", formatIndianCurrency(stdDed));
        addTableRowBold(table, "Income chargeable under the head Salaries", "0", formatIndianCurrency(taxableSalary));
        
        // House Property
        addTableRow(table, "n Income from House Property", "0", "0");
        addTableRowBold(table, "Income chargeable under head House Property", "0", formatIndianCurrency(propertyIncome));
        
        // Capital Gains
        addTableRow(table, "n Capital Gains", "0", "0");
        addTableRowBold(table, "Income chargeable under the head Capital gains", "0", formatIndianCurrency(totalCG));
        
        // Other Sources
        double otherTotal = interestIncome + dividendIncome;
        addTableRow(table, "n Income from other sources", "0", "0");
        if (interestIncome > 0) addTableRow(table, "Interest income", "0", formatIndianCurrency(interestIncome));
        if (dividendIncome > 0) addTableRow(table, "Dividends", "0", formatIndianCurrency(dividendIncome));
        addTableRowBold(table, "Income chargeable under head other sources", "0", formatIndianCurrency(otherTotal));
        
        // Total Income
        addTableRowBold(table, "n Total", "0", formatIndianCurrency(totalIncome));
        addTableRow(table, "Less Brought forward losses", "0", "0");
        addTableRowBold(table, "n " + formatIndianCurrency(totalIncome) + " Total Income", "0", formatIndianCurrency((double)Math.round(totalIncome)));
        addTableRow(table, "Total income rounded off u/s 288A", "0", formatIndianCurrency((double)Math.round(totalIncome)));
        
        // Tax
        double tax = getTax(totalIncome);
        addTableRowBold(table, "n Tax on total income", "0", formatIndianCurrency(tax));
        addTableRow(table, "Add Surcharge", "0", formatIndianCurrency(0.0));
        addTableRowBold(table, "Tax with Surcharge", "0", formatIndianCurrency(tax));
        
        double cess = tax * 0.04;
        addTableRow(table, "Add: Cess", "0", formatIndianCurrency((double)Math.round(cess)));
        
        double totalTax = (double)Math.round(tax + cess);
        addTableRowBold(table, "Tax with surcharge and cess", "0", formatIndianCurrency(totalTax));
        
        // TDS
        double tds = getTotalTDS(formData);
        addTableRow(table, "TDS/TCS", "0", formatIndianCurrency(tds));
        
        // Balance
        double balance = totalTax - tds;
        addTableRowBold(table, "n Balance Tax", "0", formatIndianCurrency(Math.abs((double)Math.round(balance))));
        
        if (balance < 0) {
            addTableRowBold(table, "n Refund", "0", formatIndianCurrency(Math.abs((double)Math.round(balance))));
        }
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSalaryDetails(Document document, Itr1FormData formData) {
        addSectionHeader(document, "Schedule S - Salary Income");
        
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}));
        table.setWidth(100);
        
        SalaryIncome sal = formData.getSalaryIncome();
        double basic = 0, da = 0, hra = 0, lta = 0, other = 0;
        
        if (sal != null) {
            basic = getDoubleValue(sal.getBasicSalary());
            da = getDoubleValue(sal.getDaAmount());
            hra = getDoubleValue(sal.getHraReceived());
            lta = getDoubleValue(sal.getLtaReceived());
            other = getDoubleValue(sal.getOtherAllowance());
        }
        
        double total = basic + da + hra + lta + other;
        
        addTableRow(table, "Basic Salary", "Exempt", formatIndianCurrency(basic));
        if (da > 0) addTableRow(table, "DA", "Exempt", formatIndianCurrency(da));
        if (hra > 0) addTableRow(table, "HRA", "Exempt", formatIndianCurrency(hra));
        if (lta > 0) addTableRow(table, "LTA", "Exempt", formatIndianCurrency(lta));
        addTableRowBold(table, "Total", "Exempt", formatIndianCurrency(total));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTaxComputation(Document document, Itr1FormData formData) {
        addSectionHeader(document, "Tax Computation");
        
        double totalIncome = getTotalIncome(formData);
        double tax = getTax(totalIncome);
        
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}));
        table.setWidth(100);
        
        addTableRow(table, "Tax on total income", "", formatIndianCurrency(tax));
        addTableRow(table, "Add: Surcharge", "", formatIndianCurrency(0.0));
        addTableRow(table, "Tax with Surcharge", "", formatIndianCurrency(tax));
        addTableRow(table, "Add: Cess", "", formatIndianCurrency((double)Math.round(tax * 0.04)));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTDSDetails(Document document, Itr1FormData formData) {
        addSectionHeader(document, "Schedule TDS - Tax Deducted at Source");
        
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 20, 15, 15}));
        table.setWidth(100);
        
        table.addHeaderCell(createHeaderCell("TDS on Salary"));
        table.addHeaderCell(createHeaderCell("TDS deducted"));
        table.addHeaderCell(createHeaderCell("TDS claimed"));
        table.addHeaderCell(createHeaderCell("Gross Salary"));
        
        double totalTds = 0;
        double totalSalary = 0;
        
        TaxPayments tp = formData.getTaxPayments();
        List<TDSOnSalary> tdsList = (tp != null) ? tp.getTdsOnSalary() : null;
        
        if (tdsList != null && !tdsList.isEmpty()) {
            for (TDSOnSalary tds : tdsList) {
                double tdsAmt = getDoubleValue(tds.getTdsAmount());
                double salAmt = getDoubleValue(tds.getTotalSalary());
                table.addCell(createValueCellStr(tds.getEmployerName() != null ? tds.getEmployerName() : ""));
                table.addCell(createValueCellStr(formatIndianCurrency(tdsAmt)));
                table.addCell(createValueCellStr(formatIndianCurrency(tdsAmt)));
                table.addCell(createValueCellStr(formatIndianCurrency(salAmt)));
                totalTds += tdsAmt;
                totalSalary += salAmt;
            }
        } else {
            table.addCell(createValueCellStr("No TDS"));
            table.addCell(createValueCellStr("0"));
            table.addCell(createValueCellStr("0"));
            table.addCell(createValueCellStr("0"));
        }
        
        table.addCell(createBoldCell("Total"));
        table.addCell(createBoldCellStr("0"));
        table.addCell(createBoldCellStr("0"));
        table.addCell(createBoldCellStr("0"));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSectionHeader(Document document, String title) {
        document.add(new Paragraph(title)
                .setFontSize(11).setBold()
                .setBackgroundColor(SUBHEADER_BACKGROUND)
                .setFontColor(DeviceRgb.WHITE)
                .setPadding(5));
    }

    // Helper methods

    private double getSalaryIncome(Itr1FormData formData) {
        SalaryIncome sal = formData.getSalaryIncome();
        if (sal == null) return 0;
        return getDoubleValue(sal.getBasicSalary()) + getDoubleValue(sal.getDaAmount()) +
               getDoubleValue(sal.getHraReceived()) + getDoubleValue(sal.getLtaReceived()) +
               getDoubleValue(sal.getOtherAllowance());
    }

    private double getPropertyIncome(Itr1FormData formData) {
        HousePropertyIncome hp = formData.getHousePropertyIncome();
        if (hp == null) return 0;
        return getDoubleValue(hp.getIncomeFromHP());
    }

    private double getInterestIncome(Itr1FormData formData) {
        OtherSourcesIncome other = formData.getOtherSourcesIncome();
        if (other == null) return 0;
        return getDoubleValue(other.getSavingsAccountInterest()) + getDoubleValue(other.getFixedDepositInterest()) +
               getDoubleValue(other.getRecurringDepositInterest()) + getDoubleValue(other.getNscInterest()) +
               getDoubleValue(other.getScssInterest()) + getDoubleValue(other.getPostOfficeInterest()) +
               getDoubleValue(other.getOtherInterest());
    }

    private double getDividendIncome(Itr1FormData formData) {
        OtherSourcesIncome other = formData.getOtherSourcesIncome();
        if (other == null) return 0;
        return getDoubleValue(other.getDividendFromShares()) + getDoubleValue(other.getDividendFromMutualFunds()) +
               getDoubleValue(other.getDividendFromUnits());
    }

    private double getTotalIncome(Itr1FormData formData) {
        double sal = getSalaryIncome(formData) - 50000;
        if (sal < 0) sal = 0;
        return sal + getPropertyIncome(formData) + getInterestIncome(formData) + getDividendIncome(formData);
    }

    private double getTax(double totalIncome) {
        if (totalIncome <= 0) return 0;
        if (totalIncome <= 250000) return 0;
        if (totalIncome <= 500000) return (totalIncome - 250000) * 0.10;
        if (totalIncome <= 1000000) return 25000 + (totalIncome - 500000) * 0.20;
        return 25000 + 100000 + (totalIncome - 1000000) * 0.30;
    }

    private double getTotalTDS(Itr1FormData formData) {
        double tds = 0;
        
        TaxPayments tp = formData.getTaxPayments();
        if (tp != null) {
            List<TDSOnSalary> tdsSal = tp.getTdsOnSalary();
            if (tdsSal != null) {
                for (TDSOnSalary t : tdsSal) {
                    tds += getDoubleValue(t.getTdsAmount());
                }
            }
            
            List<TDSOnOther> tdsOthers = tp.getTdsOnOther();
            if (tdsOthers != null) {
                for (TDSOnOther t : tdsOthers) {
                    tds += getDoubleValue(t.getTdsAmount());
                }
            }
        }
        
        return tds;
    }

    private double getDoubleValue(Double val) {
        return val != null ? val : 0;
    }

    // Cell creation methods
    private Cell createLabelCell(String text) {
        return new Cell().add(new Paragraph(text)).setBorder(Border.NO_BORDER).setPadding(2);
    }

    private Cell createValueCell(Double value) {
        return new Cell().add(new Paragraph(formatIndianCurrency(value)))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }

    private Cell createValueCellStr(String text) {
        return new Cell().add(new Paragraph(text != null ? text : ""))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }

    private Cell createBoldCell(Double value) {
        return new Cell().add(new Paragraph(formatIndianCurrency(value)).setBold())
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }

    private Cell createBoldCellStr(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "0").setBold())
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }

    private Cell createBoldCell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "").setBold())
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER).setPadding(3);
    }

    private void addTableRow(Table table, String label, String sch, String value) {
        table.addCell(createLabelCell(label));
        table.addCell(createValueCellStr(sch));
        table.addCell(createValueCellStr(value));
    }

    private void addTableRowBold(Table table, String label, String sch, String value) {
        table.addCell(createBoldCell(label));
        table.addCell(createValueCellStr(sch));
        table.addCell(createBoldCell(value));
    }

    private void addTableRow(Table table, String label, String sch, double value) {
        table.addCell(createLabelCell(label));
        table.addCell(createValueCellStr(sch));
        table.addCell(createValueCell(value));
    }

    private void addTableRowBold(Table table, String label, String sch, double value) {
        table.addCell(createBoldCell(label));
        table.addCell(createValueCellStr(sch));
        table.addCell(createBoldCell(value));
    }

    // Currency formatting - returns "0" instead of blank
    private String formatIndianCurrency(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return "0";
        }
        long rounded = Math.round(value);
        return formatWithIndianSeparators(rounded);
    }

    private String formatWithIndianSeparators(long value) {
        if (value == 0) return "0";
        boolean negative = value < 0;
        if (negative) value = -value;
        long val = (long)value;
        String str = Long.toString(val);
        StringBuilder result = new StringBuilder();
        int len = str.length();
        int count = 0;
        for (int i = len - 1; i >= 0; i--) {
            result.append(str.charAt(i));
            count++;
            if (count == 2 && i > 0) { result.append(','); count = 0; }
            else if (count == 3 && i > 0) { result.append(','); count = 0; }
        }
        if (negative) result.append('-');
        return result.reverse().toString();
    }
}
