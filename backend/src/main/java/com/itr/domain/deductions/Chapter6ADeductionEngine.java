package com.itr.domain.deductions;

import com.itr.domain.common.TaxRegime;
import static com.itr.domain.common.AssessmentYear.*;

/**
 * Chapter6ADeductionEngine — master engine for all Chapter VI-A deductions.
 * <p>
 * CRITICAL: For new regime under Section 115BAC, ONLY the following deductions
 * from Chapter VI-A are allowed:
 * - 80CCD(2) — Employer NPS contribution
 * - 80CCH(2) — Agniveer Corpus Fund
 * - 80JJAA — Additional employee cost
 * All other deductions return ZERO in new regime.
 */
public final class Chapter6ADeductionEngine {

    private Chapter6ADeductionEngine() {}

    /**
     * Compute total deductions for a given regime.
     *
     * @param input All deduction inputs bundled
     * @param regime OLD or NEW
     * @return Total deductions in paise (zero for most items in new regime)
     */
    public static DeductionResult compute(DeductionInput input, TaxRegime regime) {
        if (regime == TaxRegime.NEW) {
            return computeNewRegime(input);
        }
        return computeOldRegime(input);
    }

    private static DeductionResult computeOldRegime(DeductionInput input) {
        // 80C combined: LIC, PPF, ELSS, NSC, FD, tuition, home loan principal, ULIP, etc.
        long section80C = Math.min(input.section80C, LIMIT_80C_COMBINED);

        // 80CCD(1B): Additional NPS over and above 80C
        long section80CCD1B = Math.min(input.section80CCD1B, LIMIT_80CCD_1B);

        // 80D: Medical insurance
        long section80D = compute80D(input);

        // 80DD: Disabled dependent
        long section80DD = input.section80DD;
        long limit80DD = input.isSevereDisability ? LIMIT_80DD_SEVERE : LIMIT_80DD_NORMAL;

        // 80DDB: Specified diseases
        long section80DDB = input.age >= 60 ? Math.min(input.section80DDB, LIMIT_80DDB_ABOVE60)
                                             : Math.min(input.section80DDB, LIMIT_80DDB_BELOW60);

        // 80E: Education loan (no cap, but limited to interest paid)
        long section80E = Math.max(input.section80E, 0);

        // 80EEA: Affordable housing (₹1.5L if conditions met)
        long section80EEA = Math.min(input.section80EEA, LIMIT_80EEA);

        // 80EEB: EV loan (₹1.5L)
        long section80EEB = Math.min(input.section80EEB, LIMIT_80EEB);

        // 80G: Donations (various categories)
        long section80G = compute80G(input.gti, input);

        // 80GG: Rent (for those not receiving HRA)
        long section80GG = compute80GG(input);

        // 80TTA: Savings interest (below 60)
        long section80TTA = input.age < 60 ? Math.min(input.section80TTA, LIMIT_80TTA) : 0;

        // 80TTB: All interest (senior citizens)
        long section80TTB = input.age >= 60 ? Math.min(input.section80TTB, LIMIT_80TTB) : 0;

        // 80U: Self disability
        long section80U = input.section80U;
        long limit80U = input.isSelfSevereDisability ? LIMIT_80U_SEVERE : LIMIT_80U_NORMAL;

        long total = section80C + section80CCD1B
            + Math.min(section80DD, limit80DD)
            + section80DDB
            + section80E
            + section80EEA + section80EEB
            + section80G + section80GG
            + section80TTA + section80TTB
            + Math.min(section80U, limit80U);

        return new DeductionResult(total);
    }

    private static DeductionResult computeNewRegime(DeductionInput input) {
        // New regime allows only: 80CCD(2) — Employer NPS, 80CCH(2) — Agniveer, 80JJAA
        // Note: 80CCD(2) is computed elsewhere (percent of basic+DA)
        // 80JJAA additional employee cost
        long total = input.section80CCD2 + input.section80CCH2 + input.section80JJAA;
        return new DeductionResult(total);
    }

    private static long compute80D(DeductionInput input) {
        long selfLimit = input.age >= 60 ? LIMIT_80D_SELF_ABOVE60 : LIMIT_80D_SELF_BELOW60;
        long parentsLimit = input.parentsAge >= 60 ? LIMIT_80D_PARENTS_ABOVE60 : LIMIT_80D_PARENTS_BELOW60;
        long self = Math.min(input.selfMedInsurance, selfLimit);
        long parents = Math.min(input.parentsMedInsurance, parentsLimit);
        // Preventive health check-up within overall 80D limits
        long preventive = Math.min(input.preventiveHCU, LIMIT_80D_PREVENTIVE_HCU);
        long adjustedSelf = Math.min(self, selfLimit - preventive > 0 ? selfLimit : selfLimit);
        return Math.min(self + parents + preventive, selfLimit + parentsLimit);
    }

    private static long compute80G(long gti, DeductionInput input) {
        // 80G calculation: depends on category (100%/50% with/without 10% GTI limit)
        // Simplified: sum of all 80G donations, capped at relevant limits
        // Each donation category has its own percentage and limit
        long total80G = 0;
        for (var donation : input.donations) {
            total80G += donation.eligibleAmount();
        }
        return Math.min(total80G, Math.max(0, gti * 10 / 100));
    }

    private static long compute80GG(DeductionInput input) {
        if (input.hraReceived || input.totalRentPaid <= 0) return 0;
        // Min of: ₹5K/month, 25% of adjusted total income, actual rent - 10% of ATI
        long monthly = 5_00000L * 12; // ₹60K per year
        long pctOfATI = input.ati * 25 / 100;
        long rentMinus10 = input.totalRentPaid - (input.ati * 10 / 100);
        return Math.min(monthly, Math.min(pctOfATI, Math.max(rentMinus10, 0)));
    }

    public record DonationEntry(long amount, long eligiblePercentage, boolean has10PercentLimit) {
        public long eligibleAmount() { return amount * eligiblePercentage / 100; }
    }

    public record DeductionInput(
        long section80C,
        long section80CCD1B,
        long section80CCD2,
        long section80CCH2,
        long selfMedInsurance,
        long parentsMedInsurance,
        long preventiveHCU,
        long section80DD,
        long section80DDB,
        long section80E,
        long section80EEA,
        long section80EEB,
        long section80JJAA,
        long section80TTA,
        long section80TTB,
        long section80U,
        long totalRentPaid,
        long gti,
        long ati,
        int age,
        int parentsAge,
        boolean isSevereDisability,
        boolean isSelfSevereDisability,
        boolean hraReceived,
        java.util.List<DonationEntry> donations
    ) {}

    public record DeductionResult(long totalDeductions) {}
}
