package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.CaseId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCaseNoteCmd(
    @NotNull(message = "Case ID is required")
    CaseId caseId,
    
    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    String content,
    
    @NotBlank(message = "Author ID is required")
    String authorId
) {}