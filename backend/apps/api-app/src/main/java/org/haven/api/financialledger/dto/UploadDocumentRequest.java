package org.haven.api.financialledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadDocumentRequest(
    @NotBlank String documentId,
    @NotBlank String documentName,
    @NotBlank String documentType,
    @NotNull byte[] documentContent
) {}