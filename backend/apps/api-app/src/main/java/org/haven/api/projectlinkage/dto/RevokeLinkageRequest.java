package org.haven.api.projectlinkage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class RevokeLinkageRequest {

    @NotNull(message = "Revocation date is required")
    private LocalDate revocationDate;

    @NotBlank(message = "Revocation reason is required")
    private String revocationReason;

    public RevokeLinkageRequest() {}

    public RevokeLinkageRequest(LocalDate revocationDate, String revocationReason) {
        this.revocationDate = revocationDate;
        this.revocationReason = revocationReason;
    }

    public LocalDate getRevocationDate() { return revocationDate; }
    public void setRevocationDate(LocalDate revocationDate) { this.revocationDate = revocationDate; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }
}