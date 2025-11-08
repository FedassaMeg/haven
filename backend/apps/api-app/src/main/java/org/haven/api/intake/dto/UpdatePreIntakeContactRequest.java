package org.haven.api.intake.dto;

import org.haven.intake.domain.ReferralSource;
import java.time.LocalDate;
import java.util.Map;

public record UpdatePreIntakeContactRequest(
    String clientAlias,
    LocalDate contactDate,
    ReferralSource referralSource,
    Integer step,
    Map<String, Object> stepData
) {}
