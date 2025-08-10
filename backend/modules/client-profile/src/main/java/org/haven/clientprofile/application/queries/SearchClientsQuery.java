package org.haven.clientprofile.application.queries;

public record SearchClientsQuery(
    String name,
    boolean activeOnly
) {}