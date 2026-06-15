package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * GiftEntry — Section 56(2)(x) gift entry.
 *
 * Gift Types:
 *   CASH — Cash / Cheque / DD (face value)
 *   IMMOVABLE_WITHOUT_CONS — Immovable property without consideration (stamp duty value)
 *   IMMOVABLE_INADEQUATE_CONS — Immovable property with inadequate consideration (SDV − Consideration)
 *   MOVABLE_WITHOUT_CONS — Movable property without consideration (FMV)
 *   MOVABLE_INADEQUATE_CONS — Movable property with inadequate consideration (FMV − Consideration)
 */
public class GiftEntry {
    private String giftType;               // CASH, IMMOVABLE_WITHOUT_CONS, IMMOVABLE_INADEQUATE_CONS, MOVABLE_WITHOUT_CONS, MOVABLE_INADEQUATE_CONS
    private BigDecimal amount;             // FMV / stamp duty value
    private BigDecimal consideration;      // Paid consideration (0 if without consideration)
    private boolean receivedOnMarriage;    // If true → exempt regardless of value
    private boolean fromRelative;          // If true → exempt
    private String donorRelationship;      // For audit trail

    public GiftEntry() {}

    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getConsideration() { return consideration; }
    public void setConsideration(BigDecimal consideration) { this.consideration = consideration; }
    public boolean isReceivedOnMarriage() { return receivedOnMarriage; }
    public void setReceivedOnMarriage(boolean receivedOnMarriage) { this.receivedOnMarriage = receivedOnMarriage; }
    public boolean isFromRelative() { return fromRelative; }
    public void setFromRelative(boolean fromRelative) { this.fromRelative = fromRelative; }
    public String getDonorRelationship() { return donorRelationship; }
    public void setDonorRelationship(String donorRelationship) { this.donorRelationship = donorRelationship; }
}
