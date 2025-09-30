package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCeEventRepository extends JpaRepository<JpaCeEventEntity, UUID> {

    List<JpaCeEventEntity> findByEnrollmentIdOrderByEventDateDesc(UUID enrollmentId);

    List<JpaCeEventEntity> findByClientIdOrderByEventDateDesc(UUID clientId);
}
