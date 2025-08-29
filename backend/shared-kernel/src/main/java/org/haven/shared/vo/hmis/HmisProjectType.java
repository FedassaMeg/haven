package org.haven.shared.vo.hmis;

public enum HmisProjectType {
    EMERGENCY_SHELTER("ES", "Emergency Shelter", 1),
    TRANSITIONAL_HOUSING("TH", "Transitional Housing", 2),
    RAPID_REHOUSING("RRH", "Rapid Re-Housing", 13),
    STREET_OUTREACH("SO", "Street Outreach", 4),
    SERVICES_ONLY("SSO", "Services Only", 6),
    OTHER("OTH", "Other", 7),
    SAFE_HAVEN("SH", "Safe Haven", 8),
    PERMANENT_HOUSING_WITH_SERVICES("PH", "Permanent Housing (disability required for entry)", 3),
    PERMANENT_HOUSING_ONLY("PHO", "Housing Only", 9),
    PERMANENT_HOUSING_WITH_SERVICES_NO_DISABILITY("PHND", "Housing with Services (no disability required for entry)", 10),
    DAY_SHELTER("DS", "Day Shelter", 11),
    HOMELESSNESS_PREVENTION("HP", "Homelessness Prevention", 12),
    COORDINATED_ENTRY("CE", "Coordinated Entry", 14),
    JOINT_TH_RRH("JOINT_TH_RRH", "Joint TH & RRH", 15);

    private final String code;
    private final String description;
    private final int hmisTypeId;

    HmisProjectType(String code, String description, int hmisTypeId) {
        this.code = code;
        this.description = description;
        this.hmisTypeId = hmisTypeId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getHmisTypeId() {
        return hmisTypeId;
    }

    public boolean isTransitionalHousing() {
        return this == TRANSITIONAL_HOUSING;
    }

    public boolean isRapidRehousing() {
        return this == RAPID_REHOUSING;
    }

    public boolean isJointThRrh() {
        return this == JOINT_TH_RRH;
    }

    public boolean supportsJointFlow() {
        return this == TRANSITIONAL_HOUSING || this == RAPID_REHOUSING || this == JOINT_TH_RRH;
    }

    public static HmisProjectType fromHmisTypeId(int hmisTypeId) {
        for (HmisProjectType type : values()) {
            if (type.hmisTypeId == hmisTypeId) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown HMIS project type ID: " + hmisTypeId);
    }

    public static HmisProjectType fromCode(String code) {
        for (HmisProjectType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown HMIS project type code: " + code);
    }
}