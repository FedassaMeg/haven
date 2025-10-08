package org.haven.shared.security;

import org.haven.shared.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfidentialityPolicyService")
class ConfidentialityPolicyServiceTest {

    @Mock
    private AuditService auditService;

    private ConfidentialityPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new ConfidentialityPolicyService(auditService);
    }

    @Nested
    @DisplayName("Note Access Control")
    class NoteAccessControl {

        private UUID noteId = UUID.randomUUID();
        private UUID authorId = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("Should deny access to sealed note by non-sealer")
        void shouldDenyAccessToSealedNoteByNonSealer() {
            UUID sealedBy = UUID.randomUUID(); // Different from userId
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    true, sealedBy, null, context
            );

            assertTrue(decision.isDenied());
            assertEquals("SEALED_NOTE_RESTRICTION", decision.getPolicyRule());
            assertTrue(decision.getReason().contains("sealed"));
            verify(auditService).logAccess(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should allow access to sealed note by sealer")
        void shouldAllowAccessToSealedNoteBySealer() {
            UUID sealedBy = userId; // Same as userId
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    true, sealedBy, null, context
            );

            assertTrue(decision.isAllowed());
        }

        @Test
        @DisplayName("Should allow access when user in authorized viewers list")
        void shouldAllowAccessWhenUserInAuthorizedViewers() {
            List<UUID> authorizedViewers = Arrays.asList(userId, UUID.randomUUID());
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CUSTOM",
                    false, null, authorizedViewers, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("CUSTOM_AUTHORIZED_VIEWERS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should deny access when user not in authorized viewers list")
        void shouldDenyAccessWhenUserNotInAuthorizedViewers() {
            List<UUID> authorizedViewers = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CUSTOM",
                    false, null, authorizedViewers, context
            );

            assertTrue(decision.isDenied());
            assertEquals("CUSTOM_AUTHORIZED_VIEWERS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow DV counselor to access privileged counseling notes")
        void shouldAllowDVCounselorToAccessPrivilegedCounseling() {
            AccessContext context = createContext(userId, UserRole.DV_COUNSELOR);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "PRIVILEGED_COUNSELING", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("PRIVILEGED_COUNSELING_ACCESS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow licensed clinician to access privileged counseling notes")
        void shouldAllowLicensedClinicianToAccessPrivilegedCounseling() {
            AccessContext context = createContext(userId, UserRole.LICENSED_CLINICIAN);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "PRIVILEGED_COUNSELING", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("PRIVILEGED_COUNSELING_ACCESS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow note author to access privileged counseling notes")
        void shouldAllowAuthorToAccessPrivilegedCounseling() {
            UUID author = userId; // User is the author
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, author, "PRIVILEGED_COUNSELING", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("PRIVILEGED_COUNSELING_ACCESS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should deny case manager access to privileged counseling notes")
        void shouldDenyCaseManagerAccessToPrivilegedCounseling() {
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "PRIVILEGED_COUNSELING", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isDenied());
            assertEquals("PRIVILEGED_COUNSELING_ACCESS", decision.getPolicyRule());
        }
    }

    @Nested
    @DisplayName("Visibility Scope Rules")
    class VisibilityScopeRules {

        private UUID noteId = UUID.randomUUID();
        private UUID authorId = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("Should allow anyone to access PUBLIC scope")
        void shouldAllowPublicAccess() {
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "PUBLIC",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("SCOPE_PUBLIC", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow case manager to access CASE_TEAM scope")
        void shouldAllowCaseManagerToCaseTeam() {
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("SCOPE_CASE_TEAM", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow supervisor to access CASE_TEAM scope")
        void shouldAllowSupervisorToCaseTeam() {
            AccessContext context = createContext(userId, UserRole.SUPERVISOR);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
        }

        @Test
        @DisplayName("Should deny clinician access to CASE_TEAM scope")
        void shouldDenyClinicianToCaseTeam() {
            AccessContext context = createContext(userId, UserRole.CLINICIAN);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    false, null, null, context
            );

            assertTrue(decision.isDenied());
            assertEquals("SCOPE_CASE_TEAM", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow clinical roles to access CLINICAL_ONLY scope")
        void shouldAllowClinicalRolesToClinicalOnly() {
            for (UserRole role : Arrays.asList(UserRole.CLINICIAN, UserRole.THERAPIST,
                    UserRole.COUNSELOR, UserRole.DV_COUNSELOR)) {
                AccessContext context = createContext(userId, role);

                PolicyDecision decision = policyService.canAccessNote(
                        noteId, authorId, "COUNSELING", "CLINICAL_ONLY",
                        false, null, null, context
                );

                assertTrue(decision.isAllowed());
                assertEquals("SCOPE_CLINICAL_ONLY", decision.getPolicyRule());
            }
        }

        @Test
        @DisplayName("Should allow legal roles to access LEGAL_TEAM scope")
        void shouldAllowLegalRolesToLegalTeam() {
            for (UserRole role : Arrays.asList(UserRole.LEGAL_ADVOCATE, UserRole.ATTORNEY)) {
                AccessContext context = createContext(userId, role);

                PolicyDecision decision = policyService.canAccessNote(
                        noteId, authorId, "LEGAL_ADVOCACY", "LEGAL_TEAM",
                        false, null, null, context
                );

                assertTrue(decision.isAllowed());
                assertEquals("SCOPE_LEGAL_TEAM", decision.getPolicyRule());
            }
        }

        @Test
        @DisplayName("Should allow medical roles to access MEDICAL_TEAM scope")
        void shouldAllowMedicalRolesToMedicalTeam() {
            for (UserRole role : Arrays.asList(UserRole.NURSE, UserRole.DOCTOR,
                    UserRole.MEDICAL_ADVOCATE)) {
                AccessContext context = createContext(userId, role);

                PolicyDecision decision = policyService.canAccessNote(
                        noteId, authorId, "MEDICAL", "MEDICAL_TEAM",
                        false, null, null, context
                );

                assertTrue(decision.isAllowed());
                assertEquals("SCOPE_MEDICAL_TEAM", decision.getPolicyRule());
            }
        }

        @Test
        @DisplayName("Should allow only author to access AUTHOR_ONLY scope")
        void shouldAllowOnlyAuthorToAuthorOnly() {
            UUID author = userId; // User is the author
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, author, "STANDARD", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("SCOPE_AUTHOR_ONLY", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should deny non-author access to AUTHOR_ONLY scope")
        void shouldDenyNonAuthorToAuthorOnly() {
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "AUTHOR_ONLY",
                    false, null, null, context
            );

            assertTrue(decision.isDenied());
            assertEquals("SCOPE_AUTHOR_ONLY", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow attorney to access ATTORNEY_CLIENT scope")
        void shouldAllowAttorneyToAttorneyClient() {
            AccessContext context = createContext(userId, UserRole.ATTORNEY);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, authorId, "ATTORNEY_CLIENT", "ATTORNEY_CLIENT",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("SCOPE_ATTORNEY_CLIENT", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should allow client (author) to access ATTORNEY_CLIENT scope")
        void shouldAllowClientToAttorneyClient() {
            UUID author = userId; // User is the client/author
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canAccessNote(
                    noteId, author, "ATTORNEY_CLIENT", "ATTORNEY_CLIENT",
                    false, null, null, context
            );

            assertTrue(decision.isAllowed());
            assertEquals("SCOPE_ATTORNEY_CLIENT", decision.getPolicyRule());
        }
    }

    @Nested
    @DisplayName("VSP Access Control")
    class VSPAccessControl {

        private UUID clientId = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("Should allow non-VSP user access to all data")
        void shouldAllowNonVSPUserAccess() {
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            PolicyDecision decision = policyService.canVSPAccessClient(
                    clientId, true, "HMIS", context
            );

            assertTrue(decision.isAllowed());
            assertEquals("NON_VSP_ACCESS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should deny VSP access to HMIS data for DV victims (VAWA)")
        void shouldDenyVSPAccessToHMISForDVVictims() {
            AccessContext context = createContext(userId, UserRole.VSP);

            PolicyDecision decision = policyService.canVSPAccessClient(
                    clientId, true, "HMIS", context
            );

            assertTrue(decision.isDenied());
            assertEquals("VSP_VAWA_RESTRICTION", decision.getPolicyRule());
            assertTrue(decision.getReason().contains("VAWA"));
        }

        @Test
        @DisplayName("Should allow VSP access to ComparableDB data")
        void shouldAllowVSPAccessToComparableDB() {
            AccessContext context = createContext(userId, UserRole.VSP);

            PolicyDecision decision = policyService.canVSPAccessClient(
                    clientId, false, "COMPARABLE_DB", context
            );

            assertTrue(decision.isAllowed());
            assertEquals("VSP_COMPARABLE_DB_ACCESS", decision.getPolicyRule());
        }

        @Test
        @DisplayName("Should deny VSP access to unknown data systems")
        void shouldDenyVSPAccessToUnknownDataSystem() {
            AccessContext context = createContext(userId, UserRole.VSP);

            PolicyDecision decision = policyService.canVSPAccessClient(
                    clientId, false, "UNKNOWN_SYSTEM", context
            );

            assertTrue(decision.isDenied());
            assertEquals("VSP_DATA_SYSTEM_RESTRICTION", decision.getPolicyRule());
        }
    }

    @Nested
    @DisplayName("Audit Trail")
    class AuditTrail {

        @Test
        @DisplayName("Should log all policy decisions to audit service")
        void shouldLogAllPolicyDecisions() {
            UUID userId = UUID.randomUUID();
            UUID noteId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            AccessContext context = createContext(userId, UserRole.CASE_MANAGER);

            policyService.canAccessNote(
                    noteId, authorId, "STANDARD", "CASE_TEAM",
                    false, null, null, context
            );

            verify(auditService, times(1)).logAccess(
                    eq(userId),
                    eq("Test User"),
                    eq("RestrictedNote"),
                    eq(noteId),
                    anyString(),
                    anyString(),
                    any()
            );
        }
    }

    // Helper method to create AccessContext
    private AccessContext createContext(UUID userId, UserRole... roles) {
        return AccessContext.fromRoles(
                userId,
                "Test User",
                Arrays.asList(roles),
                "Unit test access",
                "127.0.0.1",
                "test-session-" + UUID.randomUUID(),
                "Test User Agent"
        );
    }
}
