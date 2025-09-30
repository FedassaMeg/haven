package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCeAssessmentRepository extends JpaRepository<JpaCeAssessmentEntity, UUID> {

    List<JpaCeAssessmentEntity> findByEnrollmentIdOrderByAssessmentDateDesc(UUID enrollmentId);

    List<JpaCeAssessmentEntity> findByClientIdOrderByAssessmentDateDesc(UUID clientId);
}
