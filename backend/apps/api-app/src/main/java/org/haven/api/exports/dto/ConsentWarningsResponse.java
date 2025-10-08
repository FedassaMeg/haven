package org.haven.api.exports.dto;

import java.util.List;

public class ConsentWarningsResponse {
    private List<ConsentWarning> warnings;
    private boolean hasBlockingIssues;
    private boolean requiresAggregateMode;
    private String summary;

    public ConsentWarningsResponse() {}

    public ConsentWarningsResponse(List<ConsentWarning> warnings, boolean hasBlockingIssues,
                                   boolean requiresAggregateMode, String summary) {
        this.warnings = warnings;
        this.hasBlockingIssues = hasBlockingIssues;
        this.requiresAggregateMode = requiresAggregateMode;
        this.summary = summary;
    }

    public List<ConsentWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ConsentWarning> warnings) {
        this.warnings = warnings;
    }

    public boolean isHasBlockingIssues() {
        return hasBlockingIssues;
    }

    public void setHasBlockingIssues(boolean hasBlockingIssues) {
        this.hasBlockingIssues = hasBlockingIssues;
    }

    public boolean isRequiresAggregateMode() {
        return requiresAggregateMode;
    }

    public void setRequiresAggregateMode(boolean requiresAggregateMode) {
        this.requiresAggregateMode = requiresAggregateMode;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
