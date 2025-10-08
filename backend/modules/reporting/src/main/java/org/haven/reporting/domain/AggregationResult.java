package org.haven.reporting.domain;

import java.util.Map;

/**
 * Result of an aggregation computation.
 * Contains metric values with suppression applied where necessary.
 */
public record AggregationResult(
        String metricName,
        Map<String, Object> values,
        boolean hasSuppressedCells
) {
    public static AggregationResult of(String metricName, Map<String, Object> values) {
        boolean hasSuppressed = values.values().stream()
                .anyMatch(v -> "*".equals(v));
        return new AggregationResult(metricName, values, hasSuppressed);
    }
}
