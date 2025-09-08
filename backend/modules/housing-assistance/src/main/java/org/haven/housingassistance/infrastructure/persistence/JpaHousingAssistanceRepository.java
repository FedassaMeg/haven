package org.haven.housingassistance.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.housingassistance.domain.HousingAssistance;
import org.haven.housingassistance.domain.HousingAssistanceId;
import org.haven.housingassistance.domain.HousingAssistanceRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface JpaHousingAssistanceRepository extends JpaRepository<JpaHousingAssistanceEntity, UUID>, HousingAssistanceRepository {
    
    @Override
    default Optional<HousingAssistance> findById(HousingAssistanceId id) {
        return findById(id.getValue())
            .map(JpaHousingAssistanceEntity::toDomain);
    }
    
    @Override
    default void save(HousingAssistance aggregate) {
        JpaHousingAssistanceEntity entity = JpaHousingAssistanceEntity.fromDomain(aggregate);
        save(entity);
    }
    
    @Override
    default void delete(HousingAssistance aggregate) {
        deleteById(aggregate.getId().getValue());
    }
    
    @Override
    default HousingAssistanceId nextId() {
        return HousingAssistanceId.generate();
    }
    
    @Override
    default List<HousingAssistance> findByClientId(ClientId clientId) {
        return findByClientId(clientId.getValue()).stream()
            .map(JpaHousingAssistanceEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaHousingAssistanceEntity> findByClientId(UUID clientId);
    
    @Override
    default List<HousingAssistance> findByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        return findByEnrollmentId(enrollmentId.getValue()).stream()
            .map(JpaHousingAssistanceEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaHousingAssistanceEntity> findByEnrollmentId(UUID enrollmentId);
    
    @Override
    default List<HousingAssistance> findByStatus(HousingAssistance.AssistanceStatus status) {
        return findJpaEntitiesByStatus(status).stream()
            .map(JpaHousingAssistanceEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaHousingAssistanceEntity> findJpaEntitiesByStatus(HousingAssistance.AssistanceStatus status);
    
    @Override
    default Optional<HousingAssistance> findByClientIdAndStatus(ClientId clientId, HousingAssistance.AssistanceStatus status) {
        return findByClientIdAndStatus(clientId.getValue(), status)
            .map(JpaHousingAssistanceEntity::toDomain);
    }
    
    Optional<JpaHousingAssistanceEntity> findByClientIdAndStatus(UUID clientId, HousingAssistance.AssistanceStatus status);
    
    @Override
    default List<HousingAssistance> findActiveByClientId(ClientId clientId) {
        return findActiveByClientId(clientId.getValue()).stream()
            .map(JpaHousingAssistanceEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Query("SELECT ha FROM JpaHousingAssistanceEntity ha WHERE ha.clientId = :clientId AND ha.status IN ('ACTIVE', 'APPROVED')")
    List<JpaHousingAssistanceEntity> findActiveByClientId(@Param("clientId") UUID clientId);
}