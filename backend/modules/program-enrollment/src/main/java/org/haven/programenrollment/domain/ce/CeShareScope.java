package org.haven.programenrollment.domain.ce;

/**
 * Coordinated Entry share scopes align with consent-ledger enforcement boundaries.
 * Each scope must be explicitly granted before CE payloads can move downstream.
 */
public enum CeShareScope {
    COC_COORDINATED_ENTRY,
    HMIS_PARTICIPATION,
    BY_NAME_LIST,
    VAWA_RESTRICTED_PARTNERS,
    DV_DATA,
    SYSTEM_PERFORMANCE,
    ADMIN_AUDIT;

    /**
     * @return true when scope is locked behind VAWA protections.
     */
    public boolean requiresVawaClearance() {
        return this == VAWA_RESTRICTED_PARTNERS || this == DV_DATA;
    }
}
