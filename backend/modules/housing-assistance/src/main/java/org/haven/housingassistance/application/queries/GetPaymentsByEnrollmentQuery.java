package org.haven.housingassistance.application.queries;

import org.haven.housingassistance.domain.HousingAssistance.AssistancePaymentSubtype;
import java.time.LocalDate;
import java.util.UUID;

public record GetPaymentsByEnrollmentQuery(
    UUID enrollmentId
) {}