package com.itr.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Loss Ledger Entity - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
 * Tracks carry-forward losses with 8-year expiry (4-year for speculative)
 */
@Entity
@Table(name = "loss_ledger", indexes = {
    @Index(name = "idx_ll_pan_ay", columnList = "taxpayer_pan, incurred_ay"),
    @Index(name = "idx_ll_pan_active", columnList = "taxpayer_pan, is_expired, remaining_amount")
})
@Data
@NoArgsConstructor
public class LossLedger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_pan", nullable = false, length = 10)
    private String taxpayerPAN;

    @Column(name = "incurred_ay", nullable = false, length = 7)
    private String incurredAY;

    @Column(name = "loss_type", nullable = false, length = 30)
    private String lossType;

    @Column(name = "original_amount", nullable = false)
    private int originalAmount;

    @Column(name = "utilized_amount", nullable = false)
    private int utilizedAmount = 0;

    @Column(name = "remaining_amount", nullable = false)
    private int remainingAmount;

    @Column(name = "expiry_ay", nullable = false, length = 7)
    private String expiryAY;

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static LossLedger of(String pan, String ay, String type, int amount, String expiryAY) {
        LossLedger ll = new LossLedger();
        ll.taxpayerPAN = pan;
        ll.incurredAY = ay;
        ll.lossType = type;
        ll.originalAmount = amount;
        ll.remainingAmount = amount;
        ll.expiryAY = expiryAY;
        return ll;
    }

    public void utilize(int amount) {
        this.utilizedAmount += amount;
        this.remainingAmount -= amount;
        if (this.remainingAmount <= 0) {
            this.remainingAmount = 0;
            this.isExpired = true;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
