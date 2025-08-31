package org.haven.programenrollment.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramEnrollmentRepository extends Repository<ProgramEnrollment, ProgramEnrollmentId> {
    
    /**
     * Find all enrollments for a specific client
     */
    List<ProgramEnrollment> findByClientId(ClientId clientId);
    
    /**
     * Find active enrollments for a client
     */
    List<ProgramEnrollment> findActiveByClientId(ClientId clientId);
    
    /**
     * Find all enrollments for a specific program
     */
    List<ProgramEnrollment> findByProgramId(UUID programId);
    
    /**
     * Find active enrollments for a program
     */
    List<ProgramEnrollment> findActiveByProgramId(UUID programId);
    
    /**
     * Find enrollments within a date range
     */
    List<ProgramEnrollment> findByEnrollmentDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find enrollments that exited within a date range
     */
    List<ProgramEnrollment> findByExitDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if client has active enrollment in specific program
     */
    boolean hasActiveEnrollment(ClientId clientId, UUID programId);
    
    /**
     * Get enrollment statistics for reporting
     */
    EnrollmentStatistics getStatistics(UUID programId, LocalDate startDate, LocalDate endDate);

    /**
     * Find the enrollment chain starting from the given enrollment id.
     */
    List<ProgramEnrollment> findEnrollmentChain(ProgramEnrollmentId enrollmentId);

    
    record EnrollmentStatistics(
        long totalEnrollments,
        long activeEnrollments, 
        long exitedEnrollments,
        long totalServiceEpisodes
    ) {}
}
