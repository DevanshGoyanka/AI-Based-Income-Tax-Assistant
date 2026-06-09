package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Clubbing Service - Section 60-64
 * 101% CBDT Compliant
 * 
 * Clubbing provisions require certain incomes to be included in the taxpayer's income
 * even though they are earned by another person (spouse, minor child, etc.)
 */
@Slf4j
@Service
public class ClubbingService {

    private static final double MINOR_CHILD_EXEMPTION = 1500; // ₹1,500 per child per year

    /**
     * Calculate clubbing for minor child income (Section 64(1A))
     * Income of minor child clubbed with parent having higher income
     * Exemption: ₹1,500 per child per year
     */
    public ClubbingResult calculateMinorChildClubbing(double childIncome, 
                                                      int numberOfMinorChildren,
                                                      double parent1Income,
                                                      double parent2Income) {
        
        log.info("Calculating minor child income clubbing");
        
        // Determine which parent has higher income
        boolean clubWithParent1 = parent1Income >= parent2Income;
        double exemption = numberOfMinorChildren * MINOR_CHILD_EXEMPTION;
        double taxableAmount = Math.max(0, childIncome - exemption);
        
        log.info("Minor child income: ₹{}, Exemption: ₹{}, Taxable: ₹{}, Club with: Parent {}",
                childIncome, exemption, taxableAmount, clubWithParent1 ? "1" : "2");
        
        return ClubbingResult.builder()
                .incomeToClub(taxableAmount)
                .exemptionAmount(exemption)
                .clubWithPerson(clubWithParent1 ? "Parent 1 (Higher Income)" : "Parent 2 (Higher Income)")
                .section("64(1A)")
                .reason("Minor child income clubbed with higher earning parent")
                .build();
    }

    /**
     * Calculate clubbing for spouse income (Section 64(1)(iv))
     * Income from assets transferred to spouse without adequate consideration
     */
    public ClubbingResult calculateSpouseClubbing(double spouseIncome,
                                                  String transferType,
                                                  boolean adequateConsideration) {
        
        log.info("Calculating spouse income clubbing");
        
        if (adequateConsideration) {
            log.info("Adequate consideration paid - no clubbing required");
            return ClubbingResult.builder()
                    .incomeToClub(0)
                    .exemptionAmount(0)
                    .clubWithPerson("None")
                    .section("64(1)(iv)")
                    .reason("Adequate consideration paid - clubbing not applicable")
                    .build();
        }
        
        // Income from transferred asset must be clubbed
        log.info("Spouse income from transferred asset: ₹{} - clubbing required", spouseIncome);
        
        return ClubbingResult.builder()
                .incomeToClub(spouseIncome)
                .exemptionAmount(0)
                .clubWithPerson("Transferor Spouse")
                .section("64(1)(iv)")
                .reason("Income from asset transferred to spouse without adequate consideration")
                .build();
    }

    /**
     * Calculate clubbing for HUF member income (Section 64(2))
     */
    public ClubbingResult calculateHUFMemberClubbing(double memberIncome,
                                                     String relationship) {
        
        log.info("Calculating HUF member income clubbing");
        
        // Income from HUF property converted to individual property
        return ClubbingResult.builder()
                .incomeToClub(memberIncome)
                .exemptionAmount(0)
                .clubWithPerson("Individual Member")
                .section("64(2)")
                .reason("Income from HUF property converted to individual property")
                .build();
    }

    /**
     * Clubbing Result DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClubbingResult {
        private double incomeToClub;
        private double exemptionAmount;
        private String clubWithPerson;
        private String section;
        private String reason;
    }

    /**
     * Minor Child Input DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MinorChildInput {
        private double childIncome;
        private int numberOfMinorChildren;
        private double parent1Income;
        private double parent2Income;
    }

    /**
     * Spouse Input DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SpouseInput {
        private double spouseIncome;
        private String transferType;
        private boolean adequateConsideration;
    }
}
