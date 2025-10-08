package org.haven.clientprofile.domain.privacy;

import org.haven.shared.vo.hmis.HmisEthnicity;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Ethnicity privacy controls
 */
class EthnicityPrivacyControlTest {
    
    private final UUID testClientId = UUID.randomUUID();
    
    @Test
    @DisplayName("Full precision should return ethnicity unchanged")
    void testFullPrecision() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity redacted = control.getRedactedEthnicity();
        
        assertThat(redacted).isEqualTo(actualEthnicity);
        assertThat(control.isFullDisclosure()).isTrue();
    }
    
    @Test
    @DisplayName("Category only should hide specific ethnicity")
    void testCategoryOnly() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.CATEGORY_ONLY)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity redacted = control.getRedactedEthnicity();
        
        // Should indicate data exists but not reveal specifics
        assertThat(redacted).isEqualTo(HmisEthnicity.CLIENT_PREFERS_NOT_TO_ANSWER);
    }
    
    @Test
    @DisplayName("Redacted should return CLIENT_PREFERS_NOT_TO_ANSWER")
    void testRedacted() {
        HmisEthnicity actualEthnicity = HmisEthnicity.NON_HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.REDACTED)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity redacted = control.getRedactedEthnicity();
        
        assertThat(redacted).isEqualTo(HmisEthnicity.CLIENT_PREFERS_NOT_TO_ANSWER);
    }
    
    @Test
    @DisplayName("Hidden should return DATA_NOT_COLLECTED")
    void testHidden() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.HIDDEN)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity redacted = control.getRedactedEthnicity();
        
        assertThat(redacted).isEqualTo(HmisEthnicity.DATA_NOT_COLLECTED);
    }
    
    @Test
    @DisplayName("Unknown ethnicity should remain unchanged with category only")
    void testUnknownEthnicityCategoryOnly() {
        HmisEthnicity actualEthnicity = HmisEthnicity.CLIENT_DOESNT_KNOW;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.CATEGORY_ONLY)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity redacted = control.getRedactedEthnicity();
        
        // Unknown values should remain as-is
        assertThat(redacted).isEqualTo(actualEthnicity);
    }
    
    @Test
    @DisplayName("Statistical category should provide appropriate grouping")
    void testStatisticalCategory() {
        EthnicityPrivacyControl hispanicControl = new EthnicityPrivacyControl.Builder()
            .withEthnicity(HmisEthnicity.HISPANIC_LATINO)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        EthnicityPrivacyControl nonHispanicControl = new EthnicityPrivacyControl.Builder()
            .withEthnicity(HmisEthnicity.NON_HISPANIC_LATINO)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        assertThat(hispanicControl.getStatisticalCategory()).isEqualTo("Hispanic/Latino");
        assertThat(nonHispanicControl.getStatisticalCategory()).isEqualTo("Non-Hispanic/Latino");
    }
    
    @Test
    @DisplayName("Aliased ethnicity should be consistent")
    void testAliasedEthnicity() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        HmisEthnicity aliased1 = control.getAliasedEthnicity();
        HmisEthnicity aliased2 = control.getAliasedEthnicity();
        
        // Should be consistent across calls
        assertThat(aliased1).isEqualTo(aliased2);
        // Should be a known ethnicity
        assertThat(aliased1.isKnownEthnicity()).isTrue();
    }
    
    @Test
    @DisplayName("Reporting projection should include appropriate metadata")
    void testReportingProjection() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        var projection = control.getReportingProjection();
        
        assertThat(projection).containsKey("hasEthnicityData");
        assertThat(projection.get("hasEthnicityData")).isEqualTo(true);
        assertThat(projection).containsKey("precisionLevel");
        assertThat(projection.get("precisionLevel")).isEqualTo("FULL");
        assertThat(projection).containsKey("ethnicity");
        assertThat(projection.get("ethnicity")).isEqualTo("HISPANIC_LATINO");
    }
    
    @Test
    @DisplayName("Redacted description should vary by precision level")
    void testRedactedDescription() {
        HmisEthnicity actualEthnicity = HmisEthnicity.HISPANIC_LATINO;
        
        EthnicityPrivacyControl fullControl = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.FULL)
            .withClientId(testClientId)
            .build();
        
        EthnicityPrivacyControl redactedControl = new EthnicityPrivacyControl.Builder()
            .withEthnicity(actualEthnicity)
            .withPrecision(EthnicityPrecision.REDACTED)
            .withClientId(testClientId)
            .build();
        
        assertThat(fullControl.getRedactedDescription()).isEqualTo("Hispanic/Latino");
        assertThat(redactedControl.getRedactedDescription()).isEqualTo("[REDACTED]");
    }
}