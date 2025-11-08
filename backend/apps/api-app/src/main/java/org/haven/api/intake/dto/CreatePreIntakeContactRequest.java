package org.haven.api.intake.dto;

import org.haven.intake.domain.ReferralSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePreIntakeContactRequest(
    @NotBlank(message = "Client alias is required")
    @Size(min = 2, max = 200, message = "Client alias must be between 2 and 200 characters")
    String clientAlias,

    @NotNull(message = "Contact date is required")
    @PastOrPresent(message = "Contact date cannot be in the future")
    LocalDate contactDate,

    @NotNull(message = "Referral source is required")
    ReferralSource referralSource,

    @NotBlank(message = "Intake worker name is required")
    @Size(max = 200, message = "Intake worker name must not exceed 200 characters")
    String intakeWorkerName
) {}
