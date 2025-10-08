package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple in-memory implementation of ProgramEnrollmentRepository
 * This is a temporary implementation until proper persistence is added
 */
@Component("inMemoryProgramEnrollmentRepository")
@ConditionalOnProperty(name = "haven.enrollment.repository.type", havingValue = "memory")
public class ProgramEnrollmentRepositoryImpl implements ProgramEnrollmentRepository {
    
    private final java.util.Map<ProgramEnrollmentId, ProgramEnrollment> store = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Override
    public ProgramEnrollmentId nextId() {
        return ProgramEnrollmentId.generate();
    }
    
    @Override
    public void save(ProgramEnrollment enrollment) {
        store.put(enrollment.getId(), enrollment);
    }
    
    @Override
    public Optional<ProgramEnrollment> findById(ProgramEnrollmentId id) {
        return Optional.ofNullable(store.get(id));
    }
    
    @Override
    public List<ProgramEnrollment> findByClientId(ClientId clientId) {
        return store.values().stream()
            .filter(e -> e.getClientId().equals(clientId))
            .toList();
    }
    
    @Override
    public List<ProgramEnrollment> findActiveByClientId(ClientId clientId) {
        return store.values().stream()
            .filter(e -> e.getClientId().equals(clientId))
            .filter(ProgramEnrollment::isActive)
            .toList();
    }
    
    @Override
    public List<ProgramEnrollment> findByProgramId(UUID programId) {
        return store.values().stream()
            .filter(e -> e.getProgramId().equals(programId))
            .toList();
    }
    
    @Override
    public List<ProgramEnrollment> findActiveByProgramId(UUID programId) {
        return store.values().stream()
            .filter(e -> e.getProgramId().equals(programId))
            .filter(ProgramEnrollment::isActive)
            .toList();
    }
    
    @Override
    public List<ProgramEnrollment> findByEnrollmentDateBetween(LocalDate startDate, LocalDate endDate) {
        return store.values().stream()
            .filter(e -> !e.getEnrollmentDate().isBefore(startDate) && !e.getEnrollmentDate().isAfter(endDate))
            .toList();
    }
    
    @Override
    public List<ProgramEnrollment> findByExitDateBetween(LocalDate startDate, LocalDate endDate) {
        return store.values().stream()
            .filter(ProgramEnrollment::hasExited)
            .filter(e -> e.getProjectExit() != null)
            .filter(e -> {
                LocalDate exitDate = e.getProjectExit().getExitDate();
                return exitDate != null && !exitDate.isBefore(startDate) && !exitDate.isAfter(endDate);
            })
            .toList();
    }
    
    @Override
    public boolean hasActiveEnrollment(ClientId clientId, UUID programId) {
        return store.values().stream()
            .anyMatch(e -> e.getClientId().equals(clientId)
                && e.getProgramId().equals(programId)
                && e.isActive());
    }

    @Override
    public Optional<ProgramEnrollment> findByClientIdAndProgramId(ClientId clientId, UUID programId) {
        return store.values().stream()
            .filter(e -> e.getClientId().equals(clientId) &&
                        e.getProgramId().equals(programId))
            .findFirst();
    }

    @Override
    public EnrollmentStatistics getStatistics(UUID programId, LocalDate startDate, LocalDate endDate) {
        var enrollments = store.values().stream()
            .filter(e -> e.getProgramId().equals(programId))
            .toList();
            
        long totalEnrollments = enrollments.stream()
            .filter(e -> !e.getEnrollmentDate().isBefore(startDate) && !e.getEnrollmentDate().isAfter(endDate))
            .count();
            
        long activeEnrollments = enrollments.stream()
            .filter(ProgramEnrollment::isActive)
            .count();
            
        long exitedEnrollments = enrollments.stream()
            .filter(ProgramEnrollment::hasExited)
            .filter(e -> {
                LocalDate exitDate = e.getProjectExit().getExitDate();
                return exitDate != null && !exitDate.isBefore(startDate) && !exitDate.isAfter(endDate);
            })
            .count();
            
        long totalServiceEpisodes = enrollments.stream()
            .mapToInt(ProgramEnrollment::getServiceEpisodeCount)
            .sum();
        
        return new EnrollmentStatistics(totalEnrollments, activeEnrollments, exitedEnrollments, totalServiceEpisodes);
    }

    @Override
    public List<ProgramEnrollment> findEnrollmentChain(ProgramEnrollmentId enrollmentId) {
        // Minimal in-memory chain resolution (self + predecessor if present)
        List<ProgramEnrollment> chain = new java.util.ArrayList<>();
        Optional<ProgramEnrollment> enrollment = findById(enrollmentId);
        if (enrollment.isPresent()) {
            chain.add(enrollment.get());
            if (enrollment.get().getPredecessorEnrollmentId() != null) {
                Optional<ProgramEnrollment> predecessor = findById(
                    ProgramEnrollmentId.of(enrollment.get().getPredecessorEnrollmentId())
                );
                predecessor.ifPresent(chain::add);
            }
        }
        return chain;
    }
    @Override
    public void delete(ProgramEnrollment enrollment) {
        store.remove(enrollment.getId());
    }
}
