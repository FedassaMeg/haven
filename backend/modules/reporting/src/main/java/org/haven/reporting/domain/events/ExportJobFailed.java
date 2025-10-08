package org.haven.reporting.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event: Export job failed
 */
public class ExportJobFailed extends DomainEvent {
    private final String errorMessage;
    private final String errorCode;
    private final List<String> validationErrors;
    private final Instant failedAt;

    public ExportJobFailed(
            UUID exportJobId,
            String errorMessage,
            String errorCode,
            List<String> validationErrors,
            Instant failedAt) {
        super(exportJobId, failedAt);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.validationErrors = validationErrors;
        this.failedAt = failedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public Instant getFailedAt() {
        return failedAt;
    }
}
