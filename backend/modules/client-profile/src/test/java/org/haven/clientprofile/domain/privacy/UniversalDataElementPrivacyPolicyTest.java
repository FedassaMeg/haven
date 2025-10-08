package org.haven.clientprofile.domain.privacy;

import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.haven.clientprofile.domain.privacy.RacePrivacyControl.RaceRedactionStrategy;
import org.haven.clientprofile.domain.privacy.UniversalDataElementPrivacyPolicy.DataAccessPurpose;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for Universal Data Element Privacy Policy
 */
class UniversalDataElementPrivacyPolicyTest {
    
    private UniversalDataElementPrivacyPolicy policy;
    private PIIAccessContext mockContext;
    private final UUID testClientId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        policy = new UniversalDataElementPrivacyPolicy();
        mockContext = Mockito.mock(PIIAccessContext.class);
    }
    
    @Test
    @DisplayName("Direct service with full access should allow full disclosure")
    void testDirectServiceFullAccess() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.CONFIDENTIAL)))
            .thenReturn(true);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.FULL_DISCLOSURE);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.FULL);
    }
    
    @Test
    @DisplayName("Direct service with restricted access should generalize data")
    void testDirectServiceRestrictedAccess() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.CONFIDENTIAL)))
            .thenReturn(false);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.GENERALIZED);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.CATEGORY_ONLY);
    }
    
    @Test
    @DisplayName("Case management for assigned worker should allow full disclosure")
    void testCaseManagementAssignedWorker() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        when(mockContext.isAssignedCaseWorker(testClientId)).thenReturn(true);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.CASE_MANAGEMENT, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.CASE_MANAGEMENT, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.FULL_DISCLOSURE);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.FULL);
    }
    
    @Test
    @DisplayName("Case management for non-assigned worker should mask data")
    void testCaseManagementNonAssignedWorker() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        when(mockContext.isAssignedCaseWorker(testClientId)).thenReturn(false);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.CASE_MANAGEMENT, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.CASE_MANAGEMENT, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.MASKED);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.CATEGORY_ONLY);
    }
    
    @Test
    @DisplayName("Research purpose should use aliasing")
    void testResearchPurpose() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.RESEARCH, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.RESEARCH, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.ALIASED);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.REDACTED);
    }
    
    @Test
    @DisplayName("VSP sharing should use maximum privacy")
    void testVSPSharing() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.VSP_SHARING, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.VSP_SHARING, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.ALIASED);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.REDACTED);
    }
    
    @Test
    @DisplayName("Court ordered with legal authorization should allow full disclosure")
    void testCourtOrderedWithAuthorization() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(true);
        when(mockContext.hasLegalAuthorization()).thenReturn(true);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.COURT_ORDERED, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.COURT_ORDERED, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.FULL_DISCLOSURE);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.FULL);
    }
    
    @Test
    @DisplayName("No access should hide all demographic data")
    void testNoAccess() {
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.RESTRICTED)))
            .thenReturn(false);
        
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(
            mockContext, DataAccessPurpose.DIRECT_SERVICE, testClientId);
        
        assertThat(raceStrategy).isEqualTo(RaceRedactionStrategy.HIDDEN);
        assertThat(ethnicityPrecision).isEqualTo(EthnicityPrecision.HIDDEN);
    }
    
    @Test
    @DisplayName("Anonymous access should not include demographics")
    void testAnonymousAccess() {
        when(mockContext.isAnonymous()).thenReturn(true);
        
        boolean shouldInclude = policy.shouldIncludeDemographics(mockContext, DataAccessPurpose.REPORTING);
        
        assertThat(shouldInclude).isFalse();
    }
    
    @Test
    @DisplayName("Audit purpose should never include demographics")
    void testAuditPurpose() {
        when(mockContext.isAnonymous()).thenReturn(false);
        when(mockContext.hasAccess(eq(PIICategory.QUASI_IDENTIFIER), eq(PIIAccessLevel.INTERNAL)))
            .thenReturn(true);
        
        boolean shouldInclude = policy.shouldIncludeDemographics(mockContext, DataAccessPurpose.AUDIT);
        
        assertThat(shouldInclude).isFalse();
    }
    
    @Test
    @DisplayName("Aliasing should be used for research and VSP sharing")
    void testAliasingPreference() {
        boolean researchAliasing = policy.shouldUseAliasing(mockContext, DataAccessPurpose.RESEARCH);
        boolean vspAliasing = policy.shouldUseAliasing(mockContext, DataAccessPurpose.VSP_SHARING);
        boolean directServiceAliasing = policy.shouldUseAliasing(mockContext, DataAccessPurpose.DIRECT_SERVICE);
        
        assertThat(researchAliasing).isTrue();
        assertThat(vspAliasing).isTrue();
        assertThat(directServiceAliasing).isFalse();
    }
    
    @Test
    @DisplayName("Privacy notice should describe applied controls")
    void testPrivacyNotice() {
        String notice = policy.getPrivacyNotice(
            RaceRedactionStrategy.GENERALIZED,
            EthnicityPrecision.CATEGORY_ONLY
        );
        
        assertThat(notice).contains("Privacy Controls Applied");
        assertThat(notice).contains("generalized to categories");
        assertThat(notice).contains("shown as category only");
    }
}