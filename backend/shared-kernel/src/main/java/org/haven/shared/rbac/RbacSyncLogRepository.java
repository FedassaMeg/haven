package org.haven.shared.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RbacSyncLogRepository extends JpaRepository<RbacSyncLog, UUID> {

    List<RbacSyncLog> findBySyncTimestampAfterOrderBySyncTimestampDesc(Instant after);

    List<RbacSyncLog> findByDriftDetectedTrueOrderBySyncTimestampDesc();

    List<RbacSyncLog> findTop10ByOrderBySyncTimestampDesc();
}
