package org.haven.programenrollment.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.Program;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.domain.ProgramRepository;
import org.haven.shared.vo.hmis.HmisProjectType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProgramEnrollmentAppService {
    
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final ProgramRepository programRepository;
    
    public ProgramEnrollmentAppService(
            ProgramEnrollmentRepository enrollmentRepository,
            ProgramRepository programRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.programRepository = programRepository;
    }
    
    @Transactional
    public TransitionToRrhResult transitionToRrh(TransitionToRrhCommand command) {
        // Validate inputs
        if (command.thEnrollmentId() == null || command.rrhProgramId() == null) {
            throw new IllegalArgumentException("TH enrollment ID and RRH program ID are required");
        }
        
        // Load TH enrollment
        Optional<ProgramEnrollment> thEnrollmentOpt = enrollmentRepository.findById(
            ProgramEnrollmentId.of(command.thEnrollmentId())
        );
        
        if (thEnrollmentOpt.isEmpty()) {
            throw new IllegalArgumentException("TH enrollment not found: " + command.thEnrollmentId());
        }
        
        ProgramEnrollment thEnrollment = thEnrollmentOpt.get();
        
        // Load TH program to get project type
        Optional<Program> thProgramOpt = programRepository.findById(thEnrollment.getProgramId());
        if (thProgramOpt.isEmpty()) {
            throw new IllegalArgumentException("TH program not found: " + thEnrollment.getProgramId());
        }
        Program thProgram = thProgramOpt.get();
        
        // Load RRH program to validate compatibility
        Optional<Program> rrhProgramOpt = programRepository.findById(command.rrhProgramId());
        if (rrhProgramOpt.isEmpty()) {
            throw new IllegalArgumentException("RRH program not found: " + command.rrhProgramId());
        }
        Program rrhProgram = rrhProgramOpt.get();
        
        // Validate transition eligibility
        if (!thEnrollment.isActive()) {
            throw new IllegalStateException("Can only transition from active TH enrollment");
        }
        
        // Validate program types support transition
        if (!thProgram.isThComponent()) {
            throw new IllegalArgumentException("Source program is not a TH component");
        }
        
        if (!rrhProgram.isRrhComponent()) {
            throw new IllegalArgumentException("Target program is not an RRH component");
        }
        
        // Validate joint project compatibility if both have group codes
        if (thProgram.getJointProjectGroupCode() != null && rrhProgram.getJointProjectGroupCode() != null) {
            if (!thProgram.isJointWith(rrhProgram)) {
                throw new IllegalArgumentException("Programs are not part of the same Joint TH/RRH project");
            }
        }
        
        // Perform the transition
        ProgramEnrollmentId rrhEnrollmentId = thEnrollment.transitionToRrh(
            command.rrhProgramId(),
            command.residentialMoveInDate(),
            rrhProgram.getHmisProjectType()
        );
        
        // Create the new RRH enrollment
        ProgramEnrollment rrhEnrollment = ProgramEnrollment.createFromTransition(
            rrhEnrollmentId,
            thEnrollment.getClientId(),
            command.rrhProgramId(),
            command.thEnrollmentId(),
            command.rrhEnrollmentDate() != null ? command.rrhEnrollmentDate() : LocalDate.now(),
            command.residentialMoveInDate(),
            thEnrollment.getHouseholdId(),
            thEnrollment.getHmisRelationshipToHoH(),
            thEnrollment.getHmisPriorLivingSituation(),
            thEnrollment.getHmisLengthOfStay(),
            thEnrollment.getHmisDisablingCondition(),
            rrhProgram.getHmisProjectType()
        );
        
        // Save both enrollments
        enrollmentRepository.save(thEnrollment);
        enrollmentRepository.save(rrhEnrollment);
        
        return new TransitionToRrhResult(
            rrhEnrollmentId.value(),
            rrhEnrollment.getClientId().value(),
            command.rrhProgramId(),
            rrhEnrollment.getEnrollmentDate(),
            command.residentialMoveInDate(),
            rrhEnrollment.getHouseholdId()
        );
    }
    
    @Transactional(readOnly = true)
    public List<EnrollmentSummary> getEnrollmentChain(UUID enrollmentId) {
        // Use the JPA repository's proper chain resolution
        ProgramEnrollmentId id = ProgramEnrollmentId.of(enrollmentId);
        
        List<ProgramEnrollment> chain;
        
        // Check if the repository supports chain resolution (JPA implementation does)
        if (enrollmentRepository instanceof org.haven.programenrollment.infrastructure.persistence.JpaProgramEnrollmentRepositoryImpl) {
            var jpaRepo = (org.haven.programenrollment.infrastructure.persistence.JpaProgramEnrollmentRepositoryImpl) enrollmentRepository;
            chain = jpaRepo.findEnrollmentChain(id);
        } else {
            // Fallback to minimal implementation
            chain = new ArrayList<>();
            Optional<ProgramEnrollment> enrollment = enrollmentRepository.findById(id);
            if (enrollment.isPresent()) {
                chain.add(enrollment.get());
                
                // Add predecessor if exists
                if (enrollment.get().getPredecessorEnrollmentId() != null) {
                    Optional<ProgramEnrollment> predecessor = enrollmentRepository.findById(
                        ProgramEnrollmentId.of(enrollment.get().getPredecessorEnrollmentId())
                    );
                    predecessor.ifPresent(chain::add);
                }
            }
        }
        
        return chain.stream()
            .map(e -> new EnrollmentSummary(
                e.getId().value(),
                e.getClientId().value(),
                e.getProgramId(),
                e.getEnrollmentDate(),
                e.getPredecessorEnrollmentId(),
                e.getResidentialMoveInDate(),
                e.getHouseholdId(),
                e.getStatus().name()
            ))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true) 
    public List<EnrollmentSummary> getClientEnrollments(UUID clientId) {
        List<ProgramEnrollment> enrollments = enrollmentRepository.findByClientId(new ClientId(clientId));
        
        return enrollments.stream()
            .map(e -> new EnrollmentSummary(
                e.getId().value(),
                e.getClientId().value(),
                e.getProgramId(),
                e.getEnrollmentDate(),
                e.getPredecessorEnrollmentId(),
                e.getResidentialMoveInDate(),
                e.getHouseholdId(),
                e.getStatus().name()
            ))
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void updateResidentialMoveInDate(UUID enrollmentId, LocalDate moveInDate) {
        Optional<ProgramEnrollment> enrollmentOpt = enrollmentRepository.findById(
            ProgramEnrollmentId.of(enrollmentId)
        );
        
        if (enrollmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Enrollment not found: " + enrollmentId);
        }
        
        ProgramEnrollment enrollment = enrollmentOpt.get();
        enrollment.updateResidentialMoveInDate(moveInDate);
        
        enrollmentRepository.save(enrollment);
    }
    
    // Command and Result records
    public record TransitionToRrhCommand(
        UUID thEnrollmentId,
        UUID rrhProgramId,
        LocalDate rrhEnrollmentDate,
        LocalDate residentialMoveInDate
    ) {}
    
    public record TransitionToRrhResult(
        UUID rrhEnrollmentId,
        UUID clientId,
        UUID rrhProgramId,
        LocalDate enrollmentDate,
        LocalDate residentialMoveInDate,
        String householdId
    ) {}
    
    public record EnrollmentSummary(
        UUID id,
        UUID clientId,
        UUID programId,
        LocalDate enrollmentDate,
        UUID predecessorEnrollmentId,
        LocalDate residentialMoveInDate,
        String householdId,
        String status
    ) {}
}