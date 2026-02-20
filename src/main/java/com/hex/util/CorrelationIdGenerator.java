package com.hex.util;

import java.util.UUID;

/**
 * Generates unique correlation IDs for request tracing.
 */
public final class CorrelationIdGenerator {

    private CorrelationIdGenerator() {
        // Utility class — prevent instantiation
    }

    /**
     * Generates a new unique correlation ID.
     *
     * @return a UUID-based correlation ID string
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
