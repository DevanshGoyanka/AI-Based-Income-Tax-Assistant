package com.itr.domain.othersources;

import com.itr.domain.common.TaxRegime;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * WinningsComputer — handles both Section 115BB (traditional/offline winnings)
 * and Section 115BBJ (online game winnings).
 *
 * Section 115BB: Lottery, crossword, horse race, card games (offline), betting/gambling (offline)
 * Section 115BBJ: All online game winnings (fantasy sports, online rummy, online poker, etc.)
 *
 * Both taxed at 30% + 4% cess. No deductions.
 * ITR-1 and ITR-4 cannot report either type.
 */
@Slf4j
public class WinningsComputer {

    /**
     * Section 115BB — Traditional winnings.
     */
    public WinningsResult computeSec115BB(List<TraditionalWinningEntry> entries) {
        BigDecimal gross = entries.stream()
            .map(TraditionalWinningEntry::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = gross.multiply(new BigDecimal("0.30"));
        BigDecimal cess = tax.multiply(new BigDecimal("0.04"));
        log.info("Sec 115BB: Gross={}, Tax@30%={}, Cess@4%={}, Total={}", gross, tax, cess, tax.add(cess));
        return new WinningsResult(gross, tax, cess, tax.add(cess));
    }

    /**
     * Section 115BBJ — Online game winnings.
     * Net Winnings = Total Withdrawal − (Total Deposit + Opening Balance)
     * TDS: Section 194BA (30% at withdrawal, no threshold from AY 2024-25)
     */
    public WinningsResult computeSec115BBJ(List<OnlineGameEntry> entries) {
        BigDecimal netWinnings = BigDecimal.ZERO;
        BigDecimal totalTdsU194BA = BigDecimal.ZERO;

        for (OnlineGameEntry entry : entries) {
            BigDecimal net = entry.getTotalWithdrawal()
                                  .subtract(entry.getTotalDeposit())
                                  .subtract(entry.getOpeningBalance());
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                netWinnings = netWinnings.add(net);
            }
            totalTdsU194BA = totalTdsU194BA.add(entry.getTdsU194BA());
        }

        BigDecimal tax = netWinnings.multiply(new BigDecimal("0.30"));
        BigDecimal cess = tax.multiply(new BigDecimal("0.04"));
        BigDecimal totalTax = tax.add(cess);

        log.info("Sec 115BBJ: NetWinnings={}, Tax@30%={}, Cess@4%={}, Total={}, TDS={}",
            netWinnings, tax, cess, totalTax, totalTdsU194BA);

        return new WinningsResult(netWinnings, tax, cess, totalTax, totalTdsU194BA);
    }
}
