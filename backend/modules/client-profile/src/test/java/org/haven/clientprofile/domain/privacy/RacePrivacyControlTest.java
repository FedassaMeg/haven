package org.haven.clientprofile.domain.privacy;

import org.haven.shared.vo.hmis.HmisRace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Race privacy controls
 */
class RacePrivacyControlTest {
    
    private final UUID testClientId = UUID.randomUUID();
    
    @Test
    @DisplayName("Full disclosure should return all races unchanged")
    void testFullDisclosure() {
        Set<HmisRace> actualRaces = Set.of(
            HmisRace.ASIAN,
            HmisRace.BLACK_AFRICAN_AMERICAN
        );
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.FULL_DISCLOSURE)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        assertThat(redacted).isEqualTo(actualRaces);
        assertThat(control.isFullDisclosure()).isTrue();
    }
    
    @Test
    @DisplayName("Generalized should handle multiple races appropriately")
    void testGeneralizedMultipleRaces() {
        Set<HmisRace> actualRaces = Set.of(
            HmisRace.ASIAN,
            HmisRace.WHITE,
            HmisRace.BLACK_AFRICAN_AMERICAN
        );
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.GENERALIZED)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        // Multiple races should be generalized
        assertThat(redacted).hasSize(1);
        assertThat(redacted).contains(HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER);
    }
    
    @Test
    @DisplayName("Category only should indicate whether race is known")
    void testCategoryOnly() {
        Set<HmisRace> actualRaces = Set.of(HmisRace.ASIAN);
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.CATEGORY_ONLY)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        // Should indicate data exists but not reveal specifics
        assertThat(redacted).hasSize(1);
        assertThat(redacted).contains(HmisRace.CLIENT_PREFERS_NOT_TO_ANSWER);
    }
    
    @Test
    @DisplayName("Masked should return partial information for multiple races")
    void testMaskedMultipleRaces() {
        Set<HmisRace> actualRaces = Set.of(
            HmisRace.ASIAN,
            HmisRace.WHITE
        );
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.MASKED)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        // Should return only one race when multiple exist
        assertThat(redacted).hasSize(1);
        assertThat(actualRaces).contains(redacted.iterator().next());
    }
    
    @Test
    @DisplayName("Aliased should return consistent but different race")
    void testAliased() {
        Set<HmisRace> actualRaces = Set.of(HmisRace.ASIAN);
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.ALIASED)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted1 = control.getRedactedRaces();
        Set<HmisRace> redacted2 = control.getRedactedRaces();
        
        // Should be consistent across calls
        assertThat(redacted1).isEqualTo(redacted2);
        // Should be a known race
        assertThat(redacted1.iterator().next().isKnownRace()).isTrue();
    }
    
    @Test
    @DisplayName("Hidden should return DATA_NOT_COLLECTED")
    void testHidden() {
        Set<HmisRace> actualRaces = Set.of(HmisRace.ASIAN, HmisRace.WHITE);
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.HIDDEN)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        assertThat(redacted).hasSize(1);
        assertThat(redacted).contains(HmisRace.DATA_NOT_COLLECTED);
    }
    
    @Test
    @DisplayName("Empty races should return DATA_NOT_COLLECTED")
    void testEmptyRaces() {
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(Set.of())
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.FULL_DISCLOSURE)
            .withClientId(testClientId)
            .build();
        
        Set<HmisRace> redacted = control.getRedactedRaces();
        
        assertThat(redacted).hasSize(1);
        assertThat(redacted).contains(HmisRace.DATA_NOT_COLLECTED);
    }
    
    @Test
    @DisplayName("Reporting projection should include appropriate metadata")
    void testReportingProjection() {
        Set<HmisRace> actualRaces = Set.of(HmisRace.ASIAN, HmisRace.WHITE);
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(actualRaces)
            .withStrategy(RacePrivacyControl.RaceRedactionStrategy.GENERALIZED)
            .withClientId(testClientId)
            .build();
        
        var projection = control.getReportingProjection();
        
        assertThat(projection).containsKey("hasRaceData");
        assertThat(projection.get("hasRaceData")).isEqualTo(true);
        assertThat(projection).containsKey("isMultiracial");
        assertThat(projection.get("isMultiracial")).isEqualTo(true);
        assertThat(projection).containsKey("redactionLevel");
        assertThat(projection.get("redactionLevel")).isEqualTo("GENERALIZED");
    }
}