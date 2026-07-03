package com.itr.domain.common;

import java.util.Objects;

/**
 * IndianAmount — value object representing a monetary amount in paise.
 * <p>
 * All tax calculations MUST use paise internally (long), not BigDecimal or double.
 * Formatting to Indian numbering system (₹X,XX,XXX) is done by the formatter.
 * Immutable.
 */
public final class IndianAmount implements Comparable<IndianAmount> {

    public static final IndianAmount ZERO = new IndianAmount(0);
    
    private final long paise;

    private IndianAmount(long paise) {
        this.paise = paise;
    }

    public static IndianAmount of(long paise) {
        return new IndianAmount(paise);
    }

    public static IndianAmount fromRupees(long rupees) {
        return new IndianAmount(rupees * 100);
    }
    
    public static IndianAmount fromRupees(double rupees) {
        return new IndianAmount(Math.round(rupees * 100));
    }

    public long getPaise() {
        return paise;
    }

    public long toRupees() {
        return paise / 100;
    }
    
    public double toRupeesDecimal() {
        return paise / 100.0;
    }

    public IndianAmount add(IndianAmount other) {
        return new IndianAmount(this.paise + other.paise);
    }

    public IndianAmount subtract(IndianAmount other) {
        return new IndianAmount(this.paise - other.paise);
    }

    public IndianAmount multiply(long factor) {
        return new IndianAmount(this.paise * factor);
    }

    /** Multiply by a rate expressed in basis points (e.g. 500 = 5%). */
    public IndianAmount multiplyByBps(long bps) {
        // paise * bps / 10000 to get resulting paise, with rounding
        return new IndianAmount(Math.round((double) this.paise * bps / 10000.0));
    }

    /** Apply ceiling: amount cannot exceed max. */
    public IndianAmount cap(IndianAmount max) {
        return this.paise > max.paise ? max : this;
    }

    /** Apply floor: amount cannot go below min (e.g. zero for losses). */
    public IndianAmount floor(IndianAmount min) {
        return this.paise < min.paise ? min : this;
    }

    public boolean isZero() {
        return paise == 0;
    }

    public boolean isNegative() {
        return paise < 0;
    }

    public IndianAmount negate() {
        return new IndianAmount(-paise);
    }

    public IndianAmount abs() {
        return new IndianAmount(Math.abs(paise));
    }

    @Override
    public int compareTo(IndianAmount other) {
        return Long.compare(this.paise, other.paise);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndianAmount that)) return false;
        return paise == that.paise;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paise);
    }

    @Override
    public String toString() {
        return "₹" + String.format("%,.2f", toRupees());
    }
}
