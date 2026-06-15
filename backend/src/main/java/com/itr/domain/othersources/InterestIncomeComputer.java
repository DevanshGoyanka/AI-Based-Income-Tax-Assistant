package com.itr.domain.othersources;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * InterestIncomeComputer — computes interest income with 7 corrected ITD tags.
 *
 * 1. IntrstFrmSavingBank — Savings account interest (banks/cooperative/post office savings) — 80TTA/80TTB
 * 2. IntrstFrmTermDeposit — Bank term deposits / recurring deposits / post office time deposits
 * 3. IntrstFrmNSC — NSC accrued interest
 * 4. IntrstFrmSCSS — Senior Citizens Savings Scheme interest
 * 5. IntrstFrmPostOffice — Post Office MIS / TD / other deposits (non-savings)
 * 6. IntrstFrmIncmTaxRefund — Interest on Income Tax refund (Section 244A)
 * 7. OthrIntrstInc — Others: NBFC / HFC / Company FDs / Debentures (NEW - AY 2026-27)
 * 8. IntrstOnEnhComp — Interest on enhanced compensation (s.145A(b))
 */
@Slf4j
public class InterestIncomeComputer {

    private BigDecimal savingBankInterest      = BigDecimal.ZERO;
    private BigDecimal termDepositInterest     = BigDecimal.ZERO;
    private BigDecimal nscInterest             = BigDecimal.ZERO;
    private BigDecimal scssInterest            = BigDecimal.ZERO;
    private BigDecimal postOfficeInterest      = BigDecimal.ZERO;
    private BigDecimal itRefundInterest        = BigDecimal.ZERO;
    private BigDecimal otherInterest           = BigDecimal.ZERO;  // NBFC/HFC/Company FD
    private BigDecimal enhancedCompInterest    = BigDecimal.ZERO;  // s.145A(b)
    private BigDecimal totalInterest           = BigDecimal.ZERO;

    /**
     * Aggregate interest entries.
     */
    public void aggregate(List<InterestEntry> entries) {
        for (InterestEntry entry : entries) {
            switch (entry.getItdTag()) {
                case "IntrstFrmSavingBank":
                    savingBankInterest = savingBankInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstFrmTermDeposit":
                    termDepositInterest = termDepositInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstFrmNSC":
                    nscInterest = nscInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstFrmSCSS":
                    scssInterest = scssInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstFrmPostOffice":
                    postOfficeInterest = postOfficeInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstFrmIncmTaxRefund":
                    itRefundInterest = itRefundInterest.add(entry.getGrossAmount());
                    break;
                case "OthrIntrstInc":
                    otherInterest = otherInterest.add(entry.getGrossAmount());
                    break;
                case "IntrstOnEnhComp":
                    enhancedCompInterest = enhancedCompInterest.add(entry.getGrossAmount());
                    break;
                default:
                    // Unknown tag — treat as other interest
                    otherInterest = otherInterest.add(entry.getGrossAmount());
            }
        }

        totalInterest = savingBankInterest.add(termDepositInterest).add(nscInterest)
                        .add(scssInterest).add(postOfficeInterest).add(itRefundInterest)
                        .add(otherInterest).add(enhancedCompInterest);

        log.info("Interest breakdown: SB={}, FD={}, NSC={}, SCSS={}, PO={}, ITRef={}, Other={}, EnhComp={}, Total={}",
            savingBankInterest, termDepositInterest, nscInterest, scssInterest,
            postOfficeInterest, itRefundInterest, otherInterest, enhancedCompInterest, totalInterest);
    }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public BigDecimal getSavingBankInterest() { return savingBankInterest; }
    public BigDecimal getTermDepositInterest() { return termDepositInterest; }
    public BigDecimal getNscInterest() { return nscInterest; }
    public BigDecimal getScssInterest() { return scssInterest; }
    public BigDecimal getPostOfficeInterest() { return postOfficeInterest; }
    public BigDecimal getItRefundInterest() { return itRefundInterest; }
    public BigDecimal getOtherInterest() { return otherInterest; }
    public BigDecimal getEnhancedCompInterest() { return enhancedCompInterest; }
}
