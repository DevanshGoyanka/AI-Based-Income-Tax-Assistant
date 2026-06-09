package com.itr.repository;

import com.itr.entity.LossLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Loss Ledger Repository - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
 */
@Repository
public interface LossLedgerRepository extends JpaRepository<LossLedger, Long> {

    @Query("SELECT l FROM LossLedger l " +
           "WHERE l.taxpayerPAN = :pan " +
           "  AND l.isExpired = false " +
           "  AND l.remainingAmount > 0 " +
           "  AND l.expiryAY >= :currentAY " +
           "ORDER BY l.incurredAY ASC")
    List<LossLedger> findActive(@Param("pan") String pan, @Param("currentAY") String currentAY);

    List<LossLedger> findByTaxpayerPANAndIncurredAY(String pan, String incurredAY);

    @Query("SELECT l FROM LossLedger l " +
           "WHERE l.taxpayerPAN = :pan " +
           "  AND l.lossType = :lossType " +
           "  AND l.isExpired = false " +
           "  AND l.remainingAmount > 0 " +
           "ORDER BY l.incurredAY ASC")
    List<LossLedger> findActiveByType(@Param("pan") String pan, @Param("lossType") String lossType);
}
