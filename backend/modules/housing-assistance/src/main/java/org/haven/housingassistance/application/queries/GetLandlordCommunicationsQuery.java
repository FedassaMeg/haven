package org.haven.housingassistance.application.queries;

import java.util.UUID;

public record GetLandlordCommunicationsQuery(
    UUID clientId,
    UUID landlordId
) {}