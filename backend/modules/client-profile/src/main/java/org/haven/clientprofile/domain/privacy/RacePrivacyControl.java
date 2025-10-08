package org.haven.clientprofile.domain.privacy;

import org.haven.shared.vo.hmis.HmisRace;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Privacy control for Race data implementing aliasing and masking strategies.
 * Supports HMIS compliance while protecting sensitive demographic information.
 */
public class RacePrivacyControl {
    
    private final Set<HmisRace> actualRaces;
    private final RaceRedactionStrategy redactionStrategy;
    private final UUID clientId;
    
    public RacePrivacyControl(Set<HmisRace> actualRaces, RaceRedactionStrategy strategy, UUID clientId) {
        this.actualRaces = actualRaces != null ? new HashSet<>(actualRaces) : new HashSet<>();
        this.redactionStrategy = strategy != null ? strategy : RaceRedactionStrategy.FULL_DISCLOSURE;
        this.clientId = clientId;
    }
    
    /**
     * Returns the race data based on the redaction strategy
     */
    public Set<HmisRace> getRedactedRaces() {
        return switch (redactionStrategy) {
            case FULL_DISCLOSURE -> new HashSet<>(actualRaces);
            case GENERALIZED -> generalizeRaces();
            case CATEGORY_ONLY -> getCategoryOnly();
            case MASKED -> getMaskedRaces();
            case ALIASED -> getAliasedRaces();
            case HIDDEN -> Set.of(HmisRace.DATA_NOT_COLLECTED);
        };
    }
    
    /**
     * Generalizes races to broader categories
     */
    private Set<HmisRace> generalizeRaces() {
        if (actualRaces.isEmpty()) {
            return Set.of(HmisRace.DATA_NOT_COLLECTED);
        }
        
        // Check for "unknown" responses
        if (actualRaces.stream().anyMatch(r -> !r.isKnownRace())) {
            return Set.of(HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER);
        }
        
        // For multiple races, return a generalized indicator
        if (actualRaces.size() > 1) {
            // Return a generic "multiple races" indicator
            // Since HMIS doesn't have this, we use CLIENT_PREFERS_NOT_TO_ANSWER as a privacy measure
            return Set.of(HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER);
        }
        
        return actualRaces;
    }
    
    /**
     * Returns only whether race information is known or not
     */
    private Set<HmisRace> getCategoryOnly() {
        boolean hasKnownRace = actualRaces.stream().anyMatch(HmisRace::isKnownRace);
        if (hasKnownRace) {
            // Indicate data exists but don't reveal specifics
            return Set.of(HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER);
        }
        return Set.of(HmisRace.DATA_NOT_COLLECTED);
    }
    
    /**
     * Returns masked race data (partial information)
     */
    private Set<HmisRace> getMaskedRaces() {
        if (actualRaces.isEmpty()) {
            return Set.of(HmisRace.DATA_NOT_COLLECTED);
        }
        
        // For masking, we could return the count but not specifics
        // Or return one race if multiple are selected
        if (actualRaces.size() > 1) {
            // Return first race only as a partial disclosure
            return Set.of(actualRaces.iterator().next());
        }
        
        return actualRaces;
    }
    
    /**
     * Returns aliased race data (consistent but different from actual)
     */
    private Set<HmisRace> getAliasedRaces() {
        if (actualRaces.isEmpty()) {
            return Set.of(HmisRace.DATA_NOT_COLLECTED);
        }
        
        // Use client ID to generate consistent alias
        // This ensures the same client always gets the same alias
        Random random = new Random(clientId.hashCode());
        List<HmisRace> knownRaces = Arrays.stream(HmisRace.values())
            .filter(HmisRace::isKnownRace)
            .collect(Collectors.toList());
        
        if (knownRaces.isEmpty()) {
            return Set.of(HmisRace.DATA_NOT_COLLECTED);
        }
        
        // Return a consistent but different race
        int aliasIndex = Math.abs(random.nextInt()) % knownRaces.size();
        return Set.of(knownRaces.get(aliasIndex));
    }
    
    /**
     * Gets a human-readable description of the redacted races
     */
    public String getRedactedDescription() {
        Set<HmisRace> redacted = getRedactedRaces();
        
        if (redacted.isEmpty()) {
            return "No race data";
        }
        
        if (redacted.size() == 1) {
            HmisRace race = redacted.iterator().next();
            if (redactionStrategy == RaceRedactionStrategy.CATEGORY_ONLY && 
                race == HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER) {
                return "Race information on file (details protected)";
            }
            return race.getDescription();
        }
        
        return redacted.stream()
            .map(HmisRace::getDescription)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Checks if the current strategy allows full disclosure
     */
    public boolean isFullDisclosure() {
        return redactionStrategy == RaceRedactionStrategy.FULL_DISCLOSURE;
    }
    
    /**
     * Creates a report-safe version for aggregate reporting
     */
    public Map<String, Object> getReportingProjection() {
        Map<String, Object> projection = new HashMap<>();
        
        projection.put("hasRaceData", !actualRaces.isEmpty());
        projection.put("isMultiracial", actualRaces.size() > 1);
        projection.put("redactionLevel", redactionStrategy.name());
        
        if (redactionStrategy == RaceRedactionStrategy.FULL_DISCLOSURE ||
            redactionStrategy == RaceRedactionStrategy.GENERALIZED) {
            projection.put("races", getRedactedRaces().stream()
                .map(HmisRace::name)
                .collect(Collectors.toList()));
        }
        
        return projection;
    }
    
    /**
     * Race redaction strategies
     */
    public enum RaceRedactionStrategy {
        FULL_DISCLOSURE,    // No redaction - full race information
        GENERALIZED,        // Broader categories (e.g., "Multiple races")
        CATEGORY_ONLY,      // Only whether race is known/unknown
        MASKED,             // Partial information (e.g., only primary race)
        ALIASED,            // Consistent but false information
        HIDDEN              // Complete redaction
    }
    
    /**
     * Builder for creating RacePrivacyControl instances
     */
    public static class Builder {
        private Set<HmisRace> races = new HashSet<>();
        private RaceRedactionStrategy strategy = RaceRedactionStrategy.FULL_DISCLOSURE;
        private UUID clientId;
        
        public Builder withRaces(Set<HmisRace> races) {
            this.races = races;
            return this;
        }
        
        public Builder withStrategy(RaceRedactionStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public Builder withClientId(UUID clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public RacePrivacyControl build() {
            return new RacePrivacyControl(races, strategy, clientId);
        }
    }
}