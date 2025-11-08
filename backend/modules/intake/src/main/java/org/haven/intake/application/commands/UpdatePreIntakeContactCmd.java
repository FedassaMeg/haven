package org.haven.intake.application.commands;

import org.haven.intake.domain.PreIntakeContactId;
import org.haven.intake.domain.ReferralSource;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdatePreIntakeContactCmd(
    @NotNull(message = "Temp client ID is required")
    PreIntakeContactId tempClientId,

    String clientAlias,

    LocalDate contactDate,

    ReferralSource referralSource
) {}
