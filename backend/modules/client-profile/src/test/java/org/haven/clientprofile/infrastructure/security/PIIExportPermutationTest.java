package org.haven.clientprofile.infrastructure.security;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.pii.PIIAccessLevel;
// Note: These would normally be imported from the reporting module
// For this test, we'll focus on the PII redaction aspects
import org.haven.clientprofile.domain.Client.AdministrativeGender;
import org.haven.shared.vo.HumanName;
import org.haven.shared.vo.hmis.HmisPersonalId;
import org.haven.shared.security.DeterministicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive PII Export Permutation Test Matrix
 * Ensures SSN redaction compliance, ID stability, and proper redaction level alignment
 * across all export types and access contexts.
 */
@DisplayName("PII Export Permutation Test Matrix")
class PIIExportPermutationTest {

    private PIIRedactionService piiRedactionService;
    private DeterministicIdGenerator idGenerator;
    private Client testClient;
    private UUID testClientId;

    @BeforeEach
    void setUp() {
        piiRedactionService = new PIIRedactionService();
        idGenerator = new DeterministicIdGenerator();
        
        // Create test client with full PII data
        testClientId = UUID.randomUUID();
        testClient = createTestClientWithFullPii(testClientId);
    }

    @Nested
    @DisplayName("SSN Redaction Compliance")
    class SsnRedactionCompliance {

        @ParameterizedTest
        @EnumSource(PIIRedactionService.ExportType.class)
        @DisplayName("SSN must be redacted by default across all export types")
        void ssnMustBeRedactedByDefaultAcrossAllExportTypes(PIIRedactionService.ExportType exportType) {
            // Arrange
            PIIAccessContext restrictedContext = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("STAFF"), // userRoles - below required level for SSN access
                "TEST_EXPORT", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act
            Map<String, Object> redactedProjection = piiRedactionService.createExportProjection(
                testClient, 
                restrictedContext, 
                exportType
            );

            // Assert
            String exportedSsn = (String) redactedProjection.get("socialSecurityNumber");
            
            if (exportedSsn != null) {
                // If SSN is included, it must be redacted (not the original value)
                assertNotEquals("123-45-6789", exportedSsn, 
                    String.format("SSN should be redacted for export type %s with restricted access", exportType));
                
                // Should match expected redaction pattern
                assertTrue(exportedSsn.matches("\\*{3}-\\*{2}-\\*{4}") || exportedSsn.equals("[REDACTED]"),
                    String.format("SSN redaction format incorrect for export type %s: %s", exportType, exportedSsn));
            }
        }

        @ParameterizedTest
        @CsvSource({
            "HMIS_EXPORT, CONFIDENTIAL, true",
            "HMIS_EXPORT, HIGHLY_CONFIDENTIAL, true", 
            "VSP_SHARING, HIGHLY_CONFIDENTIAL, true",
            "RESEARCH_DATASET, HIGHLY_CONFIDENTIAL, false", // Research typically excludes SSN entirely
            "COURT_REPORTING, RESTRICTED, true"
        })
        @DisplayName("SSN inclusion depends on export type and access level")
        void ssnInclusionDependsOnExportTypeAndAccessLevel(
            PIIRedactionService.ExportType exportType, 
            PIIAccessLevel accessLevel, 
            boolean shouldIncludeSsn) {
            
            // Arrange
            String roleForAccessLevel = getRoleForAccessLevel(accessLevel);
            PIIAccessContext context = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of(roleForAccessLevel), // userRoles matching access level
                "TEST_EXPORT_PRIVILEGED", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act
            Map<String, Object> projection = piiRedactionService.createExportProjection(
                testClient, 
                context, 
                exportType
            );

            // Assert
            boolean actuallyIncludesSsn = projection.containsKey("socialSecurityNumber") 
                && projection.get("socialSecurityNumber") != null;
            
            assertEquals(shouldIncludeSsn, actuallyIncludesSsn,
                String.format("SSN inclusion mismatch for %s with %s access", exportType, accessLevel));
        }

        @Test
        @DisplayName("Whitelisted clients should bypass SSN redaction when explicitly configured")
        void whitelistedClientsShouldBypassSsnRedaction() {
            // Arrange - This test simulates the whitelisting logic that would be in HmisIntegrationService
            String testClientIdStr = testClientId.toString();
            PIIAccessContext hmisContext = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("CASE_MANAGER"), // userRoles for restricted access
                "HMIS_EXPORT_WHITELISTED", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act
            Map<String, Object> projection = piiRedactionService.createExportProjection(
                testClient, 
                hmisContext, 
                PIIRedactionService.ExportType.HMIS_EXPORT
            );

            // Assert
            String exportedSsn = (String) projection.get("socialSecurityNumber");
            
            // Note: The actual whitelisting logic is in HmisIntegrationService, 
            // but this tests the PIIRedactionService behavior when given appropriate context
            assertNotNull(exportedSsn, "Whitelisted client should have SSN included");
        }
    }

    @Nested
    @DisplayName("Hashed ID Stability")
    class HashedIdStability {

        @Test
        @DisplayName("Deterministic PersonalIDs must be stable across multiple generations")
        void deterministicPersonalIdsMustBeStableAcrossMultipleGenerations() {
            // Arrange
            UUID sameClientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // Act - Generate PersonalID multiple times
            HmisPersonalId personalId1 = HmisPersonalId.fromClientId(sameClientId);
            HmisPersonalId personalId2 = HmisPersonalId.fromClientId(sameClientId);
            HmisPersonalId personalId3 = HmisPersonalId.fromClientId(sameClientId);

            // Assert
            assertEquals(personalId1.value(), personalId2.value(), 
                "PersonalIDs should be identical for same ClientId (first vs second)");
            assertEquals(personalId2.value(), personalId3.value(), 
                "PersonalIDs should be identical for same ClientId (second vs third)");
            assertEquals(personalId1.value(), personalId3.value(), 
                "PersonalIDs should be identical for same ClientId (first vs third)");
        }

        @Test
        @DisplayName("Different ClientIDs must produce different PersonalIDs")
        void differentClientIdsMustProduceDifferentPersonalIds() {
            // Arrange
            UUID clientId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID clientId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

            // Act
            HmisPersonalId personalId1 = HmisPersonalId.fromClientId(clientId1);
            HmisPersonalId personalId2 = HmisPersonalId.fromClientId(clientId2);

            // Assert
            assertNotEquals(personalId1.value(), personalId2.value(),
                "Different ClientIDs must produce different PersonalIDs");
        }

        @Test
        @DisplayName("Hashed PersonalIDs must not reveal original ClientID")
        void hashedPersonalIdsMustNotRevealOriginalClientId() {
            // Arrange
            UUID originalClientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // Act
            HmisPersonalId hashedPersonalId = HmisPersonalId.fromClientId(originalClientId);

            // Assert
            String hashedValue = hashedPersonalId.value();
            
            // Hashed ID should not contain any part of the original UUID
            assertFalse(hashedValue.contains("550e8400"), "Hashed ID should not contain original UUID parts");
            assertFalse(hashedValue.contains("e29b"), "Hashed ID should not contain original UUID parts");
            assertFalse(hashedValue.contains("41d4"), "Hashed ID should not contain original UUID parts");
            assertFalse(hashedValue.contains("a716"), "Hashed ID should not contain original UUID parts");
            assertFalse(hashedValue.contains("446655440000"), "Hashed ID should not contain original UUID parts");
            
            // Should not be the original UUID string
            assertNotEquals(originalClientId.toString(), hashedValue, 
                "Hashed PersonalID should not be the original ClientID");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "custom-salt-production",
            "custom-salt-staging", 
            "custom-salt-development"
        })
        @DisplayName("Different salts must produce different PersonalIDs for same ClientID")
        void differentSaltsMustProduceDifferentPersonalIdsForSameClientId(String salt) {
            // Arrange
            UUID clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // Act
            HmisPersonalId defaultSaltId = HmisPersonalId.fromClientId(clientId);
            HmisPersonalId customSaltId = HmisPersonalId.fromClientId(clientId, salt);

            // Assert
            assertNotEquals(defaultSaltId.value(), customSaltId.value(),
                String.format("PersonalID with custom salt '%s' should differ from default salt", salt));
        }
    }

    @Nested
    @DisplayName("Redaction Level Alignment")  
    class RedactionLevelAlignment {

        @ParameterizedTest
        @EnumSource(PIIRedactionService.ExportType.class)
        @DisplayName("Each export type must have consistent redaction behavior")
        void eachExportTypeMustHaveConsistentRedactionBehavior(PIIRedactionService.ExportType exportType) {
            // Arrange
            PIIAccessContext standardContext = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("CASE_MANAGER"), // userRoles for restricted access
                "CONSISTENCY_TEST", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act - Run export projection twice  
            Map<String, Object> projection1 = piiRedactionService.createExportProjection(
                testClient, standardContext, exportType);
            Map<String, Object> projection2 = piiRedactionService.createExportProjection(
                testClient, standardContext, exportType);

            // Assert - Results should be identical
            assertEquals(projection1.keySet(), projection2.keySet(),
                String.format("Field inclusion should be consistent for %s", exportType));
            
            for (String key : projection1.keySet()) {
                assertEquals(projection1.get(key), projection2.get(key),
                    String.format("Field '%s' redaction should be consistent for %s", key, exportType));
            }
        }

        @ParameterizedTest
        @CsvSource({
            "HMIS_EXPORT, 3", // Should include most fields with some redaction
            "VSP_SHARING, 1", // Most restrictive - minimal fields
            "RESEARCH_DATASET, 2", // Some fields with heavy redaction  
            "COURT_REPORTING, 4" // May include more fields for legal purposes
        })
        @DisplayName("Export types must have appropriate field inclusion levels")
        void exportTypesMustHaveAppropriateFieldInclusionLevels(
            PIIRedactionService.ExportType exportType, 
            int minimumExpectedFields) {
            
            // Arrange
            PIIAccessContext context = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("CASE_MANAGER"), // userRoles for restricted access
                "FIELD_INCLUSION_TEST", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act
            Map<String, Object> projection = piiRedactionService.createExportProjection(
                testClient, context, exportType);

            // Assert
            assertTrue(projection.size() >= minimumExpectedFields,
                String.format("%s should include at least %d fields, but only included %d: %s", 
                    exportType, minimumExpectedFields, projection.size(), projection.keySet()));
        }

        @Test
        @DisplayName("Redaction behavior must be consistent across field types")
        void redactionBehaviorMustBeConsistentAcrossFieldTypes() {
            // Arrange
            PIIAccessContext context = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("STAFF"), // userRoles for internal access
                "CONSISTENCY_TEST", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act
            Map<String, Object> projection = piiRedactionService.createExportProjection(
                testClient, context, PIIRedactionService.ExportType.HMIS_EXPORT);

            // Assert - Verify that string fields are consistently redacted
            for (Map.Entry<String, Object> entry : projection.entrySet()) {
                if (entry.getValue() instanceof String && entry.getKey().toLowerCase().contains("ssn")) {
                    String redactedValue = (String) entry.getValue();
                    assertTrue(redactedValue.contains("*") || redactedValue.equals("[REDACTED]"),
                        String.format("SSN field '%s' should be redacted but was: %s", entry.getKey(), redactedValue));
                }
            }
        }
    }

    @Nested
    @DisplayName("Integration Test Scenarios")
    class IntegrationTestScenarios {

        @Test
        @DisplayName("PII redaction and ID hashing work together")
        void piiRedactionAndIdHashingWorkTogether() {
            // Arrange
            PIIAccessContext hmisExportContext = new PIIAccessContext(
                UUID.randomUUID(), // userId
                List.of("CASE_MANAGER"), // userRoles for restricted access
                "HMIS_EXPORT_INTEGRATION", // businessJustification
                UUID.randomUUID(), // caseId
                "test-session-id", // sessionId
                "127.0.0.1" // ipAddress
            );

            // Act 1: Apply PII redaction
            Map<String, Object> redactedData = piiRedactionService.createExportProjection(
                testClient, hmisExportContext, PIIRedactionService.ExportType.HMIS_EXPORT);

            // Act 2: Generate hashed PersonalID  
            HmisPersonalId hashedPersonalId = HmisPersonalId.fromClientId(testClientId);

            // Assert - Verify both PII hardening mechanisms work
            assertNotNull(redactedData, "Redacted data should be generated");
            assertNotNull(hashedPersonalId, "Hashed PersonalID should be generated");
            
            // Verify hashed PersonalID doesn't reveal original
            assertNotEquals(testClientId.toString(), hashedPersonalId.value(),
                "Hashed PersonalID should not be the original ClientID");
            
            // Verify SSN is redacted in projection
            String ssnFromProjection = (String) redactedData.get("socialSecurityNumber");
            if (ssnFromProjection != null) {
                assertNotEquals("123-45-6789", ssnFromProjection,
                    "SSN should be redacted in export projection");
            }
        }
    }

    // Helper methods

    private Client createTestClientWithFullPii(UUID clientId) {
        HumanName testName = new HumanName(
            HumanName.NameUse.OFFICIAL,
            "TestLastName",
            List.of("TestFirstName"),
            null,
            null,
            "TestFirstName TestLastName"
        );
        
        Client client = Client.create(testName, AdministrativeGender.MALE, LocalDate.of(1990, 1, 1));
        
        // Set test SSN and other PII
        client.updateSocialSecurityNumber("123-45-6789");
        
        return client;
    }
    
    private String getRoleForAccessLevel(PIIAccessLevel accessLevel) {
        return switch (accessLevel) {
            case HIGHLY_CONFIDENTIAL -> "ADMINISTRATOR";
            case CONFIDENTIAL -> "SUPERVISOR";
            case RESTRICTED -> "CASE_MANAGER";
            case INTERNAL -> "STAFF";
            case PUBLIC -> "PUBLIC_USER";
        };
    }

}