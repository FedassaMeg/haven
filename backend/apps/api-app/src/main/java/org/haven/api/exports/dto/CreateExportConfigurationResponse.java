package org.haven.api.exports.dto;

import java.util.List;
import java.util.UUID;

public class CreateExportConfigurationResponse {
    private UUID exportJobId;
    private String state;
    private List<String> messages;

    public CreateExportConfigurationResponse() {}

    public CreateExportConfigurationResponse(UUID exportJobId, String state, List<String> messages) {
        this.exportJobId = exportJobId;
        this.state = state;
        this.messages = messages;
    }

    public UUID getExportJobId() {
        return exportJobId;
    }

    public void setExportJobId(UUID exportJobId) {
        this.exportJobId = exportJobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
