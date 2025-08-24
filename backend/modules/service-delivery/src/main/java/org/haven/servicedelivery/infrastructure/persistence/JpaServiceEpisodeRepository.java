package org.haven.servicedelivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceEpisodeRepository extends JpaRepository<JpaServiceEpisodeEntity, UUID> {
    
    List<JpaServiceEpisodeEntity> findByClientId(UUID clientId);
    
    List<JpaServiceEpisodeEntity> findByEnrollmentId(String enrollmentId);
    
    List<JpaServiceEpisodeEntity> findByProgramId(String programId);
    
    List<JpaServiceEpisodeEntity> findByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<JpaServiceEpisodeEntity> findByPrimaryProviderId(String providerId);
    
    List<JpaServiceEpisodeEntity> findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(LocalDate date);
    
    @Query("SELECT s FROM ServiceDeliveryEpisodeEntity s WHERE s.isBillable = true AND s.serviceDate BETWEEN :startDate AND :endDate")
    List<JpaServiceEpisodeEntity> findBillableServicesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM ServiceDeliveryEpisodeEntity s WHERE s.isConfidential = true AND s.serviceDate BETWEEN :startDate AND :endDate")
    List<JpaServiceEpisodeEntity> findConfidentialServices(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    List<JpaServiceEpisodeEntity> findByCourtOrderNumber(String courtOrderNumber);
}