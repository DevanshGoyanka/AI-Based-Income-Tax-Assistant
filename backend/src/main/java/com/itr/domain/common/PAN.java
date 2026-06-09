package com.itr.domain.common;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * PAN (Permanent Account Number) value object.
 * Format: AAAAA9999A (5 letters + 4 digits + 1 letter).
 * Immutable and self-validating.
 */
public final class PAN {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private final String value;

    public PAN(String value) {
        Objects.requireNonNull(value, "PAN must not be null");
        String uppercased = value.trim().toUpperCase();
        if (!PAN_PATTERN.matcher(uppercased).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: " + value);
        }
        this.value = uppercased;
    }

    public String getValue() {
        return value;
    }

    public String getMasked() {
        return value.substring(0, 5) + "****" + value.charAt(9);
    }

    /** Determine entity type from PAN structure (4th character = type). */
    public PANEntityType getEntityType() {
        return switch (value.charAt(3)) {
            case 'P' -> PANEntityType.INDIVIDUAL;
            case 'C' -> PANEntityType.COMPANY;
            case 'F' -> PANEntityType.FIRM;
            case 'H' -> PANEntityType.HUF;
            case 'A' -> PANEntityType.LLP;
            case 'T' -> PANEntityType.TRUST;
            case 'B' -> PANEntityType.BODY_OF_INDIVIDUALS;
            case 'L' -> PANEntityType.LOCAL_AUTHORITY;
            case 'J' -> PANEntityType.ARTIFICIAL_JUDICIAL_PERSON;
            case 'G' -> PANEntityType.GOVERNMENT;
            default -> PANEntityType.OTHER;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PAN pan)) return false;
        return value.equals(pan.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public enum PANEntityType {
        INDIVIDUAL,
        COMPANY,
        FIRM,
        HUF,
        LLP,
        TRUST,
        BODY_OF_INDIVIDUALS,
        LOCAL_AUTHORITY,
        ARTIFICIAL_JUDICIAL_PERSON,
        GOVERNMENT,
        OTHER
    }
}
