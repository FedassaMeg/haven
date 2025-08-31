package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA-backed adapter for ProgramEnrollmentRepository
 * Avoids Spring Data's Impl suffix to prevent fragment binding
 */
@Repository("jpaProgramEnrollmentRepositoryBean")
@Primary
@Lazy
@ConditionalOnProperty(name = "haven.enrollment.repository.type", havingValue = "jpa", matchIfMissing = true)
public class ProgramEnrollmentJpaRepositoryAdapter implements ProgramEnrollmentRepository {
    
    private final JpaProgramEnrollmentRepository jpaRepository;
    private final ProgramEnrollmentAssembler assembler;
    
    public ProgramEnrollmentJpaRepositoryAdapter(
            JpaProgramEnrollmentRepository jpaRepository,
            ProgramEnrollmentAssembler assembler) {
        this.jpaRepository = jpaRepository;
        this.assembler = assembler;
    }
    
    @Override
    public Optional<ProgramEnrollment> findById(ProgramEnrollmentId id) {
        return jpaRepository.findById(id.value())
            .map(assembler::toDomainObject);
    }
    
    @Override
    public List<ProgramEnrollment> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).stream()
            .map(assembler::toDomainObject)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProgramEnrollment> findByProgramId(UUID programId) {
        return jpaRepository.findByProgramId(programId).stream()
            .map(assembler::toDomainObject)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProgramEnrollment> findByEnrollmentDateBetween(LocalDate startDate, LocalDate endDate) {
        // This would need to be implemented in the JPA repository
        // For now, return all and filter (inefficient but functional)
        return jpaRepository.findAll().stream()
            .map(assembler::toDomainObject)
            .filter(e -> !e.getEnrollmentDate().isBefore(startDate) && 
                        !e.getEnrollmentDate().isAfter(endDate))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProgramEnrollment> findActiveByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).stream()
            .map(assembler::toDomainObject)
            .filter(enrollment -> enrollment.getStatus() == ProgramEnrollment.EnrollmentStatus.ACTIVE)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProgramEnrollment> findActiveByProgramId(UUID programId) {
        return jpaRepository.findByProgramId(programId).stream()
            .map(assembler::toDomainObject)
            .filter(enrollment -> enrollment.getStatus() == ProgramEnrollment.EnrollmentStatus.ACTIVE)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProgramEnrollment> findByExitDateBetween(LocalDate startDate, LocalDate endDate) {
        // For simplicity, return empty for now - would need to implement exit date tracking
        return Collections.emptyList();
    }
    
    @Override
    public boolean hasActiveEnrollment(ClientId clientId, UUID programId) {
        return jpaRepository.findByClientId(clientId.value()).stream()
            .anyMatch(entity -> entity.getProgramId().equals(programId) && 
                     entity.getStatus() == ProgramEnrollment.EnrollmentStatus.ACTIVE);
    }
    
    @Override
    public EnrollmentStatistics getStatistics(UUID programId, LocalDate startDate, LocalDate endDate) {
        // For simplicity, return basic stats - would need more sophisticated query in production
        List<JpaProgramEnrollmentEntity> enrollments = jpaRepository.findByProgramId(programId);
        long totalEnrollments = enrollments.size();
        long activeEnrollments = enrollments.stream()
            .mapToLong(e -> e.getStatus() == ProgramEnrollment.EnrollmentStatus.ACTIVE ? 1 : 0)
            .sum();
        return new EnrollmentStatistics(totalEnrollments, activeEnrollments, 0L, 0L);
    }
    
    @Override
    public void save(ProgramEnrollment enrollment) {
        JpaProgramEnrollmentEntity entity = assembler.toEntity(enrollment);
        jpaRepository.save(entity);
    }
    
    @Override
    public void delete(ProgramEnrollment enrollment) {
        jpaRepository.deleteById(enrollment.getId().value());
    }
    
    @Override
    public ProgramEnrollmentId nextId() {
        return ProgramEnrollmentId.generate();
    }
    
    /**
     * Find enrollment chain starting from the given enrollment
     */
    @Override
    public List<ProgramEnrollment> findEnrollmentChain(ProgramEnrollmentId enrollmentId) {
        return jpaRepository.findCompleteEnrollmentChain(enrollmentId.value()).stream()
            .map(assembler::toDomainObject)
            .collect(Collectors.toList());
    }
    
    /**
     * Find enrollments by predecessor
     */
    public List<ProgramEnrollment> findByPredecessorEnrollmentId(ProgramEnrollmentId predecessorId) {
        return jpaRepository.findByPredecessorEnrollmentId(predecessorId.value()).stream()
            .map(assembler::toDomainObject)
            .collect(Collectors.toList());
    }
    
    /**
     * Find enrollments by household ID
     */
    public List<ProgramEnrollment> findByHouseholdId(String householdId) {
        return jpaRepository.findByHouseholdId(householdId).stream()
            .map(assembler::toDomainObject)
            .collect(Collectors.toList());
    }
    
    /**
     * Count linked enrollments
     */
    public long countLinkedEnrollments() {
        return jpaRepository.countLinkedEnrollments();
    }
}
