package org.haven.intake.application.commands;

import org.haven.intake.domain.PreIntakeContactId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Map;

public record UpdateWorkflowDataCmd(
    @NotNull(message = "Temp client ID is required")
    PreIntakeContactId tempClientId,

    @NotNull(message = "Step number is required")
    @Min(value = 1, message = "Step must be at least 1")
    @Max(value = 10, message = "Step must not exceed 10")
    int step,

    @NotNull(message = "Step data is required")
    Map<String, Object> stepData
) {}
