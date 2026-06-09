package com.itr.domain.capitalgains;

import java.time.LocalDate;
import static com.itr.domain.common.AssessmentYear.*;

/**
 * VDAComputer — computes tax on Virtual Digital Assets under Section 115BBH.
 * <p>
 * Flat 30% tax on gains from VDA (cryptocurrency, NFTs, etc.).
 * No deduction for expenses (except cost of acquisition).
 * No set-off of losses across VDAs or against other heads.
 * TDS under Section 194S at 1% (or higher in specified cases).
 */
public final class VDAComputer {

    private VDAComputer() {}

    public static VDAResult compute(long saleValue, long costOfAcquisition) {
        long gain = Math.max(saleValue - costOfAcquisition, 0);
        long tax = Math.round((double) gain * VDA_RATE_BPS / 10000.0);
        return new VDAResult(gain, tax);
    }

    public record VDAResult(long taxableGain, long taxPayable) {}
}
