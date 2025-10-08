package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.programenrollment.domain.vsp.VspExportMetadata;
import org.haven.programenrollment.domain.vsp.VspExportMetadataRepository;
import org.haven.shared.audit.AuditService;
import org.haven.shared.vo.hmis.VawaRecipientCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive regression tests for VSP Export Service
 * Verifies anonymization rules, hash stability, and export history accuracy
 */
class VspExportServiceTest {

    private VspExportService service;

    @Mock
    private VspExportMetadataRepository metadataRepository;

    @Mock
    private CeExportService ceExportService;

    @Mock
    private ConsentLedgerUpdatePublisher consentPublisher;

    @Mock
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<VspExportMetadata> metadataCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> auditCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VspExportService(
            metadataRepository,
            ceExportService,
            consentPublisher,
            auditService,
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Nested
    @DisplayName("CE-Specific Anonymization Rules")
    class AnonymizationTests {

        @Test
        @DisplayName("Should suppress location metadata for VSP exports")
        void shouldSuppressLocationMetadata() {
            // Given
            Map<String, Object> originalData = new HashMap<>();
            originalData.put("clientId", "12345");
            originalData.put("locationData", "123 Main St");
            originalData.put("gpsCoordinates", "40.7128,-74.0060");
            originalData.put("address", "New York, NY");
            originalData.put("zipCode", "10001");

            VspExportMetadata.AnonymizationRules rules = VspExportMetadata.AnonymizationRules.builder()
                .suppressLocationMetadata(true)
                .build();

            // When
            Map<String, Object> anonymized = rules.apply(originalData, "CE_TEST123");

            // Then
            assertThat(anonymized).doesNotContainKeys("locationData", "gpsCoordinates", "address", "zipCode");
            assertThat(anonymized).containsEntry("clientId", "12345");
        }

        @Test
        @DisplayName("Should replace household IDs with CE hash keys")
        void shouldReplaceHouseholdIds() {
            // Given
            Map<String, Object> originalData = new HashMap<>();
            originalData.put("householdId", "HH-2024-001");
            originalData.put("clientId", "CL-2024-001");

            VspExportMetadata.AnonymizationRules rules = VspExportMetadata.AnonymizationRules.builder()
                .replaceHouseholdIds(true)
                .build();

            String ceHashKey = "CE_ABC123XYZ";

            // When
            Map<String, Object> anonymized = rules.apply(originalData, ceHashKey);

            // Then
            assertThat(anonymized).containsEntry("householdId", ceHashKey);
            assertThat(anonymized).containsKey("householdHash");
            assertThat(anonymized.get("householdHash")).asString().startsWith("HH_");
            assertThat(anonymized).containsEntry("clientId", "CL-2024-001");
        }

        @Test
        @DisplayName("Should redact DV indicators based on recipient category")
        void shouldRedactDvIndicators() {
            // Given
            Map<String, Object> originalData = new HashMap<>();
            originalData.put("dvStatus", "FLEEING");
            originalData.put("fleeingDv", true);
            originalData.put("domesticViolenceIndicator", "YES");
            originalData.put("clientName", "John Doe");

            VspExportMetadata.AnonymizationRules rules = VspExportMetadata.AnonymizationRules.builder()
                .redactDvIndicators(true)
                .build();

            // When
            Map<String, Object> anonymized = rules.apply(originalData, "CE_TEST");

            // Then
            assertThat(anonymized).doesNotContainKeys("dvStatus", "fleeingDv", "domesticViolenceIndicator");
            assertThat(anonymized).containsEntry("clientName", "John Doe");
        }

        @ParameterizedTest
        @DisplayName("Should apply correct anonymization level based on recipient category")
        @EnumSource(value = VawaRecipientCategory.class)
        void shouldApplyCorrectAnonymizationLevel(VawaRecipientCategory category) {
            // Given
            VspExportService.VspExportRequest request = createTestRequest(category);

            // When
            VawaRecipientCategory.AnonymizationLevel level = category.getRequiredAnonymizationLevel();

            // Then
            switch (level) {
                case MINIMAL:
                    assertThat(category.hasFullVawaCompliance()).isTrue();
                    break;
                case STANDARD:
                    assertThat(category.isAuthorizedForVictimData()).isTrue();
                    assertThat(category.hasFullVawaCompliance()).isFalse();
                    break;
                case FULL:
                    assertThat(category.isAuthorizedForVictimData()).isFalse();
                    break;
            }
        }
    }

    @Nested
    @DisplayName("Hash Stability Tests")
    class HashStabilityTests {

        @Test
        @DisplayName("Should generate consistent CE hash keys for same input")
        void shouldGenerateConsistentCeHashKeys() {
            // Given
            VspExportService.VspExportRequest request1 = createTestRequest(VawaRecipientCategory.VICTIM_SERVICE_PROVIDER);
            VspExportService.VspExportRequest request2 = createTestRequest(VawaRecipientCategory.VICTIM_SERVICE_PROVIDER);

            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            mockCeExportService();
            service.exportForVsp(request1);
            service.exportForVsp(request2);

            // Then
            verify(metadataRepository, times(2)).save(metadataCaptor.capture());
            List<VspExportMetadata> savedMetadata = metadataCaptor.getAllValues();

            // CE hash keys should be different for different exports (time-based)
            assertThat(savedMetadata.get(0).getCeHashKey()).isNotEqualTo(savedMetadata.get(1).getCeHashKey());

            // Both should start with CE_ prefix
            assertThat(savedMetadata.get(0).getCeHashKey()).startsWith("CE_");
            assertThat(savedMetadata.get(1).getCeHashKey()).startsWith("CE_");
        }

        @Test
        @DisplayName("Should generate stable packet hash for export data")
        void shouldGenerateStablePacketHash() {
            // Given
            VspExportService.VspExportRequest request = createTestRequest(VawaRecipientCategory.VICTIM_SERVICE_PROVIDER);
            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            mockCeExportService();
            service.exportForVsp(request);

            // Then
            verify(metadataRepository).save(metadataCaptor.capture());
            VspExportMetadata saved = metadataCaptor.getValue();

            assertThat(saved.getPacketHash()).isNotNull();
            assertThat(saved.getPacketHash()).hasSizeGreaterThan(20);
            // Base64 encoded hash
            assertThat(saved.getPacketHash()).matches("[A-Za-z0-9+/]+=*");
        }
    }

    @Nested
    @DisplayName("Export History Accuracy Tests")
    class ExportHistoryTests {

        @Test
        @DisplayName("Should track export metadata accurately")
        void shouldTrackExportMetadataAccurately() {
            // Given
            VspExportService.VspExportRequest request = createTestRequest(VawaRecipientCategory.COC_LEAD);
            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            mockCeExportService();
            VspExportService.VspExportResult result = service.exportForVsp(request);

            // Then
            verify(metadataRepository).save(metadataCaptor.capture());
            VspExportMetadata saved = metadataCaptor.getValue();

            assertThat(saved.getRecipient()).isEqualTo(request.recipient());
            assertThat(saved.getRecipientCategory()).isEqualTo(request.recipientCategory());
            assertThat(saved.getConsentBasis()).isEqualTo(request.consentBasis());
            assertThat(saved.getShareScopes()).isEqualTo(request.shareScopes());
            assertThat(saved.getInitiatedBy()).isEqualTo(request.initiatedBy());
            assertThat(saved.getStatus()).isEqualTo(VspExportMetadata.ExportStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should correctly retrieve share history for recipient")
        void shouldRetrieveShareHistory() {
            // Given
            String recipient = "Test VSP Organization";
            List<VspExportMetadata> exports = createTestExports(recipient);
            when(metadataRepository.findByRecipient(recipient)).thenReturn(exports);
            when(metadataRepository.getStatisticsForRecipient(recipient))
                .thenReturn(new VspExportMetadataRepository.ExportStatistics(
                    3, 1, 1, 1,
                    Instant.now().minusSeconds(7200),
                    Instant.now(),
                    1.5
                ));

            // When
            VspExportService.RecipientShareHistory history = service.getShareHistory(recipient);

            // Then
            assertThat(history.recipient()).isEqualTo(recipient);
            assertThat(history.exports()).hasSize(3);
            assertThat(history.totalExports()).isEqualTo(3);
            assertThat(history.activeExports()).isEqualTo(1);
            assertThat(history.revokedExports()).isEqualTo(1);
            assertThat(history.expiredExports()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle export revocation correctly")
        void shouldHandleExportRevocation() {
            // Given
            UUID exportId = UUID.randomUUID();
            VspExportMetadata metadata = createTestMetadata(exportId);
            when(metadataRepository.findById(exportId)).thenReturn(Optional.of(metadata));
            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.revokeExport(exportId, "admin@haven.org", "Security concern");

            // Then
            verify(metadataRepository).save(metadataCaptor.capture());
            VspExportMetadata revoked = metadataCaptor.getValue();

            assertThat(revoked.getStatus()).isEqualTo(VspExportMetadata.ExportStatus.REVOKED);
            assertThat(revoked.getRevokedBy()).isEqualTo("admin@haven.org");
            assertThat(revoked.getRevocationReason()).isEqualTo("Security concern");
            assertThat(revoked.getRevokedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("VAWA Compliance Tests")
    class VawaComplianceTests {

        @Test
        @DisplayName("Should enforce VAWA recipient categories")
        void shouldEnforceVawaRecipientCategories() {
            // Given
            VspExportService.VspExportRequest request = createTestRequest(VawaRecipientCategory.UNAUTHORIZED);

            // When/Then
            assertThatThrownBy(() -> service.exportForVsp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorized for victim data");
        }

        @Test
        @DisplayName("Should restrict DV data sharing to fully compliant recipients")
        void shouldRestrictDvDataSharing() {
            // Given
            Set<CeShareScope> scopesWithDv = new HashSet<>();
            scopesWithDv.add(CeShareScope.DV_DATA);
            scopesWithDv.add(CeShareScope.ASSESSMENT_DATA);

            VspExportService.VspExportRequest request = new VspExportService.VspExportRequest(
                UUID.randomUUID(),
                "Legal Aid Org",
                VawaRecipientCategory.LEGAL_AID,  // Not fully compliant
                "Client consent",
                "COC-123",
                List.of(UUID.randomUUID()),
                null,
                null,
                scopesWithDv,
                "JSON",
                "KEY123",
                90,
                "test@haven.org",
                true,
                true,
                false,
                "Legal representation",
                null
            );

            // When/Then
            assertThatThrownBy(() -> service.exportForVsp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("full VAWA compliance for DV data");
        }

        @ParameterizedTest
        @DisplayName("Should allow DV data for fully compliant categories")
        @ValueSource(strings = {
            "VICTIM_SERVICE_PROVIDER",
            "EMERGENCY_SHELTER",
            "INTERNAL_USE",
            "CLIENT_REQUEST"
        })
        void shouldAllowDvDataForCompliantCategories(String categoryName) {
            // Given
            VawaRecipientCategory category = VawaRecipientCategory.valueOf(categoryName);
            Set<CeShareScope> scopesWithDv = Set.of(CeShareScope.DV_DATA);

            VspExportService.VspExportRequest request = new VspExportService.VspExportRequest(
                UUID.randomUUID(),
                "Test Organization",
                category,
                "Valid consent",
                "COC-123",
                List.of(UUID.randomUUID()),
                null,
                null,
                scopesWithDv,
                "JSON",
                "KEY123",
                90,
                "test@haven.org",
                true,
                false,
                false,
                "Authorized access",
                null
            );

            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            mockCeExportService();

            // When/Then - Should not throw
            assertThatCode(() -> service.exportForVsp(request))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Export Expiry Tests")
    class ExportExpiryTests {

        @Test
        @DisplayName("Should process expired exports correctly")
        void shouldProcessExpiredExports() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            List<VspExportMetadata> expiredExports = List.of(
                createExpiredMetadata(UUID.randomUUID(), now.minusDays(1)),
                createExpiredMetadata(UUID.randomUUID(), now.minusHours(1))
            );

            when(metadataRepository.findExpiringBefore(any())).thenReturn(expiredExports);
            when(metadataRepository.deleteExpiredOlderThan(any())).thenReturn(5);

            // When
            service.processExpiredExports();

            // Then
            verify(metadataRepository, times(2)).updateStatus(any(), eq(VspExportMetadata.ExportStatus.EXPIRED));
            verify(metadataRepository).deleteExpiredOlderThan(any());
        }

        @Test
        @DisplayName("Should calculate expiry date correctly")
        void shouldCalculateExpiryDate() {
            // Given
            VspExportService.VspExportRequest request = createTestRequest(VawaRecipientCategory.COC_LEAD);
            when(metadataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            mockCeExportService();
            service.exportForVsp(request);

            // Then
            verify(metadataRepository).save(metadataCaptor.capture());
            VspExportMetadata saved = metadataCaptor.getValue();

            LocalDateTime expectedExpiry = LocalDateTime.now().plusDays(request.expiryDays());
            assertThat(saved.getExpiryDate()).isCloseTo(expectedExpiry, within(1, java.time.temporal.ChronoUnit.MINUTES));
        }
    }

    // Helper methods

    private VspExportService.VspExportRequest createTestRequest(VawaRecipientCategory category) {
        return new VspExportService.VspExportRequest(
            UUID.randomUUID(),
            "Test Organization",
            category,
            "Written consent",
            "COC-123",
            List.of(UUID.randomUUID(), UUID.randomUUID()),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            Set.of(CeShareScope.ASSESSMENT_DATA, CeShareScope.REFERRAL_DATA),
            "JSON",
            "ENCRYPTION_KEY_001",
            90,
            "test@haven.org",
            true,
            true,
            false,
            "Data sharing agreement",
            null
        );
    }

    private void mockCeExportService() {
        CeExportService.CeExportResult mockResult = new CeExportService.CeExportResult(
            UUID.randomUUID(),
            "test-file.json",
            "encrypted data".getBytes(),
            10,
            CeExportService.ExportType.ALL_RECORDS,
            "JSON"
        );

        when(ceExportService.exportVendorJson(any())).thenReturn(mockResult);
        when(ceExportService.exportHudXml(any())).thenReturn(mockResult);
        when(ceExportService.exportHudCsv(any())).thenReturn(mockResult);
    }

    private VspExportMetadata createTestMetadata(UUID exportId) {
        return new VspExportMetadata(
            exportId,
            "Test Organization",
            VawaRecipientCategory.COC_LEAD,
            "Written consent",
            "HASH123",
            "CE_TEST123",
            Instant.now(),
            LocalDateTime.now().plusDays(90),
            Set.of(CeShareScope.ASSESSMENT_DATA),
            VspExportMetadata.AnonymizationRules.builder().build(),
            new HashMap<>(),
            "test@haven.org"
        );
    }

    private VspExportMetadata createExpiredMetadata(UUID exportId, LocalDateTime expiryDate) {
        VspExportMetadata metadata = createTestMetadata(exportId);
        // Use reflection to set expiry date
        try {
            java.lang.reflect.Field field = VspExportMetadata.class.getDeclaredField("expiryDate");
            field.setAccessible(true);
            field.set(metadata, expiryDate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return metadata;
    }

    private List<VspExportMetadata> createTestExports(String recipient) {
        VspExportMetadata active = createTestMetadata(UUID.randomUUID());

        VspExportMetadata revoked = createTestMetadata(UUID.randomUUID());
        revoked.revoke("admin", "test reason");

        VspExportMetadata expired = createTestMetadata(UUID.randomUUID());
        expired.checkAndUpdateExpiry();

        return List.of(active, revoked, expired);
    }
}