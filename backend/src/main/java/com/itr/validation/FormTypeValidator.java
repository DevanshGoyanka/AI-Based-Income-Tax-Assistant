package com.itr.validation;

import com.itr.domain.othersources.OnlineGameEntry;
import com.itr.domain.othersources.TraditionalWinningEntry;
import com.itr.domain.vda.VDAEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * FormTypeValidator — validates income eligibility per ITR form type.
 *
 * ITR-1 (Sahaj): No lottery/gambling/horse race, no online games, no VDA.
 * ITR-4 (Sugam): No lottery/gambling/horse race, no online games, no VDA, no capital gains.
 * ITR-2: All income types valid for Schedule OS.
 */
@Slf4j
public class FormTypeValidator {

    public static class IneligibleIncomeException extends RuntimeException {
        public IneligibleIncomeException(String message) {
            super(message);
        }
    }

    /**
     * Validate that the form data is eligible for the given ITR type.
     * Throws IneligibleIncomeException if not.
     */
    public static void validate(
            String itrType,
            List<TraditionalWinningEntry> winningsEntries,
            List<OnlineGameEntry> onlineGameEntries,
            List<VDAEntry> vdaEntries) {

        if (itrType == null) return; // If not specified, skip validation

        switch (itrType) {
            case "ITR-1":
            case "ITR-4":
                if (winningsEntries != null && !winningsEntries.isEmpty()) {
                    throw new IneligibleIncomeException(
                        itrType + " cannot include lottery/gambling/horse race income. Use ITR-2.");
                }
                if (onlineGameEntries != null && !onlineGameEntries.isEmpty()) {
                    throw new IneligibleIncomeException(
                        itrType + " cannot include online game winnings (s.115BBJ). Use ITR-2.");
                }
                if (vdaEntries != null && !vdaEntries.isEmpty()) {
                    throw new IneligibleIncomeException(
                        itrType + " cannot include VDA income (s.115BBH). Use ITR-2.");
                }
                break;
            case "ITR-2":
                // All income types valid for Schedule OS in ITR-2
                break;
            default:
                throw new IllegalArgumentException("Unsupported ITR type: " + itrType);
        }
    }
}
