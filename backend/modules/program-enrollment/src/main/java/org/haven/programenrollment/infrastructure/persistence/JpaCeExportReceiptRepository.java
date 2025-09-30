package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCeExportReceiptRepository extends JpaRepository<JpaCeExportReceiptEntity, UUID> {
}
