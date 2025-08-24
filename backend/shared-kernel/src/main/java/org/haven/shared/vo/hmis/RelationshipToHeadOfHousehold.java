package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.15 Relationship to Head of Household
 * Describes the relationship of each household member to the head of household.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum RelationshipToHeadOfHousehold {
    SELF_HEAD_OF_HOUSEHOLD("Self (head of household)"),
    HEAD_OF_HOUSEHOLDS_CHILD("Head of household's child"),
    HEAD_OF_HOUSEHOLDS_SPOUSE_PARTNER("Head of household's spouse or partner"),
    HEAD_OF_HOUSEHOLDS_OTHER_RELATION("Head of household's other relation member (other relation to head of household)"),
    OTHER_NON_RELATION("Other: non-relation member"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    RelationshipToHeadOfHousehold(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHeadOfHousehold() {
        return this == SELF_HEAD_OF_HOUSEHOLD;
    }

    public boolean isFamilyMember() {
        return this == HEAD_OF_HOUSEHOLDS_CHILD ||
               this == HEAD_OF_HOUSEHOLDS_SPOUSE_PARTNER ||
               this == HEAD_OF_HOUSEHOLDS_OTHER_RELATION;
    }

    public boolean isKnownRelationship() {
        return this != CLIENT_DOESNT_KNOW &&
               this != CLIENT_PREFERS_NOT_TO_ANSWER &&
               this != DATA_NOT_COLLECTED;
    }
}