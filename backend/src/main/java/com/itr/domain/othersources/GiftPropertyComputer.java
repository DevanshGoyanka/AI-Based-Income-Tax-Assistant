package com.itr.domain.othersources;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * GiftPropertyComputer — Section 56(2)(x) gift income computation.
 *
 * Key rules:
 * 1. Rs 50,000 aggregate threshold — entire amount (not excess) is taxable if aggregate > Rs 50,000.
 * 2. Gifts on occasion of marriage are exempt regardless of value.
 * 3. Gifts from relatives (as defined in Section 56(2)(x) Explanation) are exempt.
 * 4. Five gift types: Cash, Immovable (without/inadequate consideration), Movable (without/inadequate consideration).
 */
@Slf4j
public class GiftPropertyComputer {

    public GiftComputationResult compute(List<GiftEntry> entries) {
        BigDecimal totalGifts = BigDecimal.ZERO;
        BigDecimal taxableGifts = BigDecimal.ZERO;
        int exemptMarriageCount = 0;
        int exemptRelativeCount = 0;

        for (GiftEntry entry : entries) {
            // Check marriage exemption first
            if (entry.isReceivedOnMarriage()) {
                exemptMarriageCount++;
                continue;
            }

            // Check relative exemption
            if (entry.isFromRelative()) {
                exemptRelativeCount++;
                continue;
            }

            // Compute taxable value
            BigDecimal taxableValue = entry.getAmount();
            if (entry.getConsideration() != null && entry.getConsideration().compareTo(BigDecimal.ZERO) > 0) {
                // Inadequate consideration — only the excess over consideration is taxable
                taxableValue = entry.getAmount().subtract(entry.getConsideration());
                if (taxableValue.compareTo(BigDecimal.ZERO) < 0) {
                    taxableValue = BigDecimal.ZERO;
                }
            }

            totalGifts = totalGifts.add(taxableValue);
        }

        // Rs 50,000 aggregate threshold — entire amount is taxable if aggregate > Rs 50,000
        if (totalGifts.compareTo(new BigDecimal("50000")) > 0) {
            taxableGifts = totalGifts;
        }

        log.info("Gift Income: TotalTaxable={}, ExemptMarriage={}, ExemptRelative={}, Aggregate={}, Taxable={}",
            totalGifts, exemptMarriageCount, exemptRelativeCount, totalGifts, taxableGifts);

        return new GiftComputationResult(totalGifts, taxableGifts, exemptMarriageCount, exemptRelativeCount);
    }
}
