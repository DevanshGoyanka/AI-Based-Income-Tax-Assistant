package com.itr.service;

import com.itr.domain.common.TaxComputationResult;
import com.itr.domain.common.TaxRegime;
import com.itr.dto.ITRFormData;
import com.itr.entity.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ComputationPDFService — generates computation summary PDF for ITR filing.
 * Uses iText library to create formatted PDF with income heads, deductions,
 * tax computation breakdown, and verification details.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComputationPDFService {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    /**
     * Generate computation PDF for a client.
     *
     * @param formData the ITR form data
     * @param result   the tax computation result
     * @return PDF bytes
     */
    public byte[] generateComputationPDF(ITRFormData formData, TaxComputationResult result) {
        log.info("Generating computation PDF for client {}", formData.getClientId());

        // In a full implementation, this would use iText to create a PDF
        // For now, return a placeholder
        throw new UnsupportedOperationException("PDF generation requires iText library setup");
    }

    /**
     * Generate a simple text-based computation summary (for development).
     *
     * @param formData the ITR form data
     * @param result   the tax computation result
     * @return plain text computation summary
     */
    public String generateComputationText(ITRFormData formData, TaxComputationResult result) {
        StringBuilder sb = new StringBuilder();
        CurrencyFormatter cf = new CurrencyFormatter();

        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("                    INCOME TAX COMPUTATION SHEET                \n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");

        // Header
        sb.append("Assessment Year: ").append(formData.getAssessmentYear()).append("\n");
        sb.append("ITR Form: ").append(formData.getItrType()).append("\n");
        sb.append("PAN: ").append(formData.getPan()).append("\n");
        sb.append("Regime: ").append(result.getTaxRegime().name()).append("\n");
        sb.append("\n───────────────────────────────────────────────────────────────\n\n");

        // Income Summary
        sb.append("INCOME SUMMARY\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "Salary Income", cf.format(formData.getSalaryIncome())));
        sb.append(String.format("%-30s %15s\n", "House Property", cf.format(formData.getHousePropertyIncome())));
        sb.append(String.format("%-30s %15s\n", "Capital Gains", cf.format(formData.getStcg111A() + formData.getLtcg112A())));
        sb.append(String.format("%-30s %15s\n", "Business Income", cf.format(formData.getBusinessIncome())));
        sb.append(String.format("%-30s %15s\n", "Other Sources", cf.format(formData.getOtherSourcesIncome())));
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "GROSS TOTAL INCOME", cf.format(result.getGrossTotalIncome())));
        sb.append(String.format("%-30s %15s\n", "Less: Deductions", cf.format(result.getTotalDeductions())));
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "TOTAL INCOME", cf.format(result.getTotalIncome())));
        sb.append("\n");

        // Tax Computation
        sb.append("TAX COMPUTATION\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "Tax on Total Income", cf.format(result.getTaxOnNormalIncome())));
        sb.append(String.format("%-30s %15s\n", "Less: Rebate 87A", cf.format(result.getRebate87A())));
        sb.append(String.format("%-30s %15s\n", "Tax After Rebate", cf.format(result.getTaxAfterRebate())));
        sb.append(String.format("%-30s %15s\n", "Add: Surcharge", cf.format(result.getSurcharge())));
        sb.append(String.format("%-30s %15s\n", "Add: H&E Cess (4%)", cf.format(result.getHealthAndEducationCess())));
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "TOTAL TAX LIABILITY", cf.format(result.getTotalTaxLiability())));
        sb.append("\n");

        // Tax Paid
        sb.append("TAX PAID\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "TDS", cf.format(result.getTdsAmount())));
        sb.append(String.format("%-30s %15s\n", "Advance Tax", cf.format(result.getAdvanceTaxPaid())));
        sb.append(String.format("%-30s %15s\n", "Self Assessment Tax", cf.format(result.getSelfAssessmentTaxPaid())));
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-30s %15s\n", "TOTAL TAX PAID", cf.format(result.getTotalTaxesPaid())));
        sb.append("\n");

        // Final Result
        sb.append("═══════════════════════════════════════════════════════════════\n");
        if (result.getRefundAmount() > 0) {
            sb.append(String.format("          REFUND DUE: %s\n", cf.format(result.getRefundAmount())));
        } else if (result.getBalanceTaxPayable() > 0) {
            sb.append(String.format("          TAX PAYABLE: %s\n", cf.format(result.getBalanceTaxPayable())));
        } else {
            sb.append("          NO TAX DUE - NIL RETURN\n");
        }
        sb.append("═══════════════════════════════════════════════════════════════\n");

        // Warnings
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            sb.append("\nWARNINGS:\n");
            result.getWarnings().forEach(w -> sb.append("  ⚠ ").append(w).append("\n"));
        }

        return sb.toString();
    }

    /**
     * Currency formatter for Indian Rupees.
     */
    private static class CurrencyFormatter {
        private final NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));

        public String format(long paise) {
            double rupees = paise / 100.0;
            return String.format("₹%,.0f", rupees);
        }

        public String formatPaise(long paise) {
            return String.format("₹%,d", paise);
        }
    }
}
