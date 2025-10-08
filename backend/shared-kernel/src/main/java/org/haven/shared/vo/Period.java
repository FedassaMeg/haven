package org.haven.shared.vo;

import java.time.Instant;
import java.util.Objects;

/**
 * FHIR-inspired Period value object
 * Based on FHIR Period datatype
 */
public record Period(
    Instant start,
    Instant end
) {
    public Period {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Period start cannot be after end");
        }
    }

    public boolean isActive() {
        Instant now = Instant.now();
        return (start == null || !start.isAfter(now)) && 
               (end == null || !end.isBefore(now));
    }

    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "Instant cannot be null");
        return (start == null || !start.isAfter(instant)) && 
               (end == null || !end.isBefore(instant));
    }
    
    // Explicit getters for compatibility
    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
}