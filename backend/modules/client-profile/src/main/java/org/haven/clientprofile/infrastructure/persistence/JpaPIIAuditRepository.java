package org.haven.clientprofile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaPIIAuditRepository extends JpaRepository<JpaPIIAccessLogEntity, UUID> {
}