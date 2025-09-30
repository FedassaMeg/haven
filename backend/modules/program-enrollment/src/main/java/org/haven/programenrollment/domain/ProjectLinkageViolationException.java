package org.haven.programenrollment.domain;

import java.util.UUID;

/**
 * Exception thrown when TH/RRH linkage constraints are violated
 */
public class ProjectLinkageViolationException extends RuntimeException {

    private final ViolationType violationType;
    private final UUID linkageId;
    private final UUID thProjectId;
    private final UUID rrhProjectId;

    public ProjectLinkageViolationException(String message,
                                          ViolationType violationType,
                                          UUID linkageId,
                                          UUID thProjectId,
                                          UUID rrhProjectId) {
        super(message);
        this.violationType = violationType;
        this.linkageId = linkageId;
        this.thProjectId = thProjectId;
        this.rrhProjectId = rrhProjectId;
    }

    public ProjectLinkageViolationException(String message,
                                          ViolationType violationType,
                                          UUID linkageId,
                                          UUID thProjectId,
                                          UUID rrhProjectId,
                                          Throwable cause) {
        super(message, cause);
        this.violationType = violationType;
        this.linkageId = linkageId;
        this.thProjectId = thProjectId;
        this.rrhProjectId = rrhProjectId;
    }

    public enum ViolationType {
        MOVE_IN_DATE_CONSTRAINT("RRH move-in date cannot precede TH exit date"),
        EXCESSIVE_TRANSITION_GAP("Transition gap between TH exit and RRH move-in exceeds threshold"),
        MISSING_PREDECESSOR("RRH enrollment is missing required predecessor TH enrollment"),
        OVERLAPPING_ENROLLMENTS("Client has overlapping TH and RRH enrollments"),
        INVALID_LINKAGE_STATE("Linkage is not in a valid state for the requested operation"),
        UNAUTHORIZED_MODIFICATION("User is not authorized to modify this linkage");

        private final String description;

        ViolationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    public UUID getLinkageId() {
        return linkageId;
    }

    public UUID getThProjectId() {
        return thProjectId;
    }

    public UUID getRrhProjectId() {
        return rrhProjectId;
    }

    /**
     * Get a user-friendly error message for display in UI
     */
    public String getUserFriendlyMessage() {
        return switch (violationType) {
            case MOVE_IN_DATE_CONSTRAINT ->
                "The RRH move-in date must be on or after the TH exit date for linked projects.";
            case EXCESSIVE_TRANSITION_GAP ->
                "The time between TH exit and RRH move-in is too long. Please verify the dates are correct.";
            case MISSING_PREDECESSOR ->
                "This RRH enrollment requires a linked TH enrollment that cannot be found.";
            case OVERLAPPING_ENROLLMENTS ->
                "The client has overlapping TH and RRH enrollments which is not allowed.";
            case INVALID_LINKAGE_STATE ->
                "The project linkage is not currently active or valid.";
            case UNAUTHORIZED_MODIFICATION ->
                "You do not have permission to modify this project linkage.";
        };
    }

    /**
     * Get suggested resolution steps
     */
    public String getResolutionSuggestion() {
        return switch (violationType) {
            case MOVE_IN_DATE_CONSTRAINT ->
                "Update the RRH move-in date to be on or after the TH exit date, or correct the TH exit date.";
            case EXCESSIVE_TRANSITION_GAP ->
                "Verify both dates are accurate. If correct, document the reason for the extended transition period.";
            case MISSING_PREDECESSOR ->
                "Create the missing TH enrollment or remove the predecessor reference from the RRH enrollment.";
            case OVERLAPPING_ENROLLMENTS ->
                "Adjust enrollment dates to ensure proper sequencing or exit one of the overlapping enrollments.";
            case INVALID_LINKAGE_STATE ->
                "Contact an administrator to review and potentially reactivate the project linkage.";
            case UNAUTHORIZED_MODIFICATION ->
                "Contact a program manager or administrator to make this change.";
        };
    }
}