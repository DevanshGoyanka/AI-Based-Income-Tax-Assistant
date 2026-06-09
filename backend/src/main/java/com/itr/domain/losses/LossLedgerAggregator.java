package com.itr.domain.losses;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LossLedgerAggregator — computes remaining carry-forward after current year set-off.
 * Aggregates all loss entries in FIFO order per loss head.
 */
public final class LossLedgerAggregator {

    private LossLedgerAggregator() {}

    /**
     * Aggregate loss entries by head and compute remaining carry-forward.
     *
     * @param entries All loss entries for a client
     * @param currentAY Current assessment year
     * @return Aggregated remaining losses by head
     */
    public static LossSummaryResult aggregate(List<LossEntry> entries, String currentAY) {
        long hpCF = sumRemaining(entries, LossHead.HP_LOSS, currentAY);
        long stcgCF = sumRemaining(entries, LossHead.STCG_LOSS, currentAY);
        long ltcgCF = sumRemaining(entries, LossHead.LTCG_LOSS, currentAY);
        long bizCF = sumRemaining(entries, LossHead.BUSINESS_LOSS, currentAY);
        long specBizCF = sumRemaining(entries, LossHead.SPECULATIVE_BUSINESS_LOSS, currentAY);
        long depCF = sumRemaining(entries, LossHead.UNABSORBED_DEPRECIATION, currentAY);

        return new LossSummaryResult(hpCF, stcgCF, ltcgCF, bizCF, specBizCF, depCF);
    }

    private static long sumRemaining(List<LossEntry> entries, LossHead head, String currentAY) {
        return entries.stream()
            .filter(e -> e.head() == head)
            .filter(e -> e.canBeSetOffIn(currentAY))
            .mapToLong(LossEntry::remainingAmount)
            .sum();
    }

    public record LossSummaryResult(
        long hpLossCF,
        long stcgLossCF,
        long ltcgLossCF,
        long businessLossCF,
        long speculativeBusinessLossCF,
        long unabsorbedDepreciation
    ) {}
}
