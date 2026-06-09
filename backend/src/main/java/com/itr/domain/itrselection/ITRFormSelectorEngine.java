package com.itr.domain.itrselection;

import com.itr.domain.common.*;
import java.util.*;

/**
 * ITRFormSelectorEngine — evaluates all eligibility rules in priority order.
 * Returns the applicable ITR form plus the reasons why other forms were ruled out.
 */
public final class ITRFormSelectorEngine {

    private ITRFormSelectorEngine() {}

    /**
     * Select the appropriate ITR form based on the assessee's profile.
     *
     * @param totalIncome   Total income in paise
     * @param hasBusiness   Whether assessee has business/profession income
     * @param hasCapitalGains Whether assessee has capital gains
     * @param hasHPIncome   Whether assessee has house property income
     * @param entityType    PAN entity type
     * @param hasForeignIncome Whether assessee has foreign income/assets
     * @param hasVDA        Whether assessee has VDA income
     * @param totalProperties Number of house properties owned
     * @return SelectionResult with form type and reasons
     */
    public static SelectionResult select(
            long totalIncome,
            boolean hasBusiness,
            boolean hasCapitalGains,
            boolean hasHPIncome,
            PAN.PANEntityType entityType,
            boolean hasForeignIncome,
            boolean hasVDA,
            int totalProperties) {

        List<String> reasons = new ArrayList<>();

        // ITR-1 (Sahaj): salaried, ≤1 HP, no business/CG, income ≤ ₹50L
        if (!hasBusiness && !hasCapitalGains && !hasForeignIncome && !hasVDA
            && totalProperties <= 1 && totalIncome <= 50_00_00000L
            && entityType == PAN.PANEntityType.INDIVIDUAL) {
            reasons.add("ITR-1: Income ≤ ₹50L, no business, no CG, ≤1 house property.");
            return new SelectionResult(ITRFormType.ITR_1, reasons);
        }

        // ITR-2: No business, but has CG or >1 HP or foreign income or VDA
        if (!hasBusiness && entityType != PAN.PANEntityType.FIRM
            && entityType != PAN.PANEntityType.COMPANY
            && entityType != PAN.PANEntityType.TRUST) {
            reasons.add("ITR-2: Has capital gains or foreign income or VDA.");
            return new SelectionResult(ITRFormType.ITR_2, reasons);
        }

        // ITR-3: Has business/profession income
        if (hasBusiness && entityType == PAN.PANEntityType.INDIVIDUAL
            || entityType == PAN.PANEntityType.HUF) {
            reasons.add("ITR-3: Has business or profession income.");
            return new SelectionResult(ITRFormType.ITR_3, reasons);
        }

        // ITR-4: Presumptive business
        if (entityType == PAN.PANEntityType.INDIVIDUAL
            || entityType == PAN.PANEntityType.HUF) {
            reasons.add("ITR-4: Eligible for presumptive taxation.");
            return new SelectionResult(ITRFormType.ITR_4, reasons);
        }

        // ITR-5: Firms, LLPs, AOPs, BOIs
        if (entityType == PAN.PANEntityType.FIRM
            || entityType == PAN.PANEntityType.LLP
            || entityType == PAN.PANEntityType.BODY_OF_INDIVIDUALS) {
            reasons.add("ITR-5: Firm/LLP/AOP/BOI entity.");
            return new SelectionResult(ITRFormType.ITR_5, reasons);
        }

        // ITR-6: Companies
        if (entityType == PAN.PANEntityType.COMPANY) {
            reasons.add("ITR-6: Company entity.");
            return new SelectionResult(ITRFormType.ITR_6, reasons);
        }

        // ITR-7: Trusts, political institutions
        if (entityType == PAN.PANEntityType.TRUST) {
            reasons.add("ITR-7: Trust/institution entity.");
            return new SelectionResult(ITRFormType.ITR_7, reasons);
        }

        reasons.add("Default: ITR-2 (individual/HUF with misc income).");
        return new SelectionResult(ITRFormType.ITR_2, reasons);
    }

    public record SelectionResult(ITRFormType formType, List<String> reasons) {}
}
