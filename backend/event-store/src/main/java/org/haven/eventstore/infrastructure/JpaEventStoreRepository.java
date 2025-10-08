package org.haven.eventstore.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEventStoreRepository extends JpaRepository<JpaEventStoreEntity, Long> {
    
    @Query("SELECT e FROM JpaEventStoreEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.sequence")
    List<JpaEventStoreEntity> findByAggregateIdOrderBySequence(@Param("aggregateId") UUID aggregateId);
    
    @Query("SELECT MAX(e.sequence) FROM JpaEventStoreEntity e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findMaxSequenceByAggregateId(@Param("aggregateId") UUID aggregateId);
    
    @Query("SELECT COUNT(e) FROM JpaEventStoreEntity e WHERE e.aggregateId = :aggregateId")
    long countByAggregateId(@Param("aggregateId") UUID aggregateId);
    
    boolean existsByAggregateIdAndSequence(UUID aggregateId, Long sequence);
}