package com.hex.model;

import java.util.Arrays;

/**
 * Represents the type of loan review being conducted.
 */
public enum ReviewType {
    LDC,
    SEC_POLICY,
    CONDUIT;

    /**
     * Parses a display string into a ReviewType enum value.
     * Accepts "LDC", "Sec Policy", "Conduit" (case-insensitive).
     *
     * @param value the display string to parse
     * @return the corresponding ReviewType
     * @throws IllegalArgumentException if the value does not match any ReviewType
     */
    public static ReviewType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ReviewType value must not be null");
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        return Arrays.stream(values())
                .filter(rt -> rt.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid ReviewType: '" + value + "'. Allowed values: LDC, Sec Policy, Conduit"));
    }
}
