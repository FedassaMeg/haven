package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.Program;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramRepository;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

import java.util.UUID;

@Component
@Lazy
public class ProgramEnrollmentAssembler {
    
    private final ProgramRepository programRepository;
    
    public ProgramEnrollmentAssembler(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }
    
    public JpaProgramEnrollmentEntity toEntity(ProgramEnrollment domainObject) {
        JpaProgramEnrollmentEntity entity = new JpaProgramEnrollmentEntity(
            domainObject.getId().value(),
            domainObject.getClientId().value(),
            domainObject.getProgramId(),
            domainObject.getEnrollmentDate()
        );
        
        // Map basic fields
        entity.setStatus(domainObject.getStatus());
        entity.setPredecessorEnrollmentId(domainObject.getPredecessorEnrollmentId());
        entity.setResidentialMoveInDate(domainObject.getResidentialMoveInDate());
        entity.setHouseholdId(domainObject.getHouseholdId());
        
        // Map HMIS fields
        entity.setRelationshipToHead(mapRelationshipToHead(domainObject.getHmisRelationshipToHoH()));
        entity.setResidencePriorToEntry(mapResidencePrior(domainObject.getHmisPriorLivingSituation()));
        entity.setLengthOfStayPriorToEntry(mapLengthOfStay(domainObject.getHmisLengthOfStay()));
        
        // Map periods
        if (domainObject.getEnrollmentPeriod() != null) {
            entity.setEnrollmentPeriodStart(domainObject.getEnrollmentPeriod().start());
            entity.setEnrollmentPeriodEnd(domainObject.getEnrollmentPeriod().end());
        }
        
        entity.setCreatedAt(domainObject.getCreatedAt());
        
        return entity;
    }
    
    public ProgramEnrollment toDomainObject(JpaProgramEnrollmentEntity entity) {
        // Create enrollment from transition if it has a predecessor
        if (entity.getPredecessorEnrollmentId() != null) {
            return ProgramEnrollment.createFromTransition(
                ProgramEnrollmentId.of(entity.getId()),
                new ClientId(entity.getClientId()),
                entity.getProgramId(),
                entity.getPredecessorEnrollmentId(),
                entity.getEnrollmentDate(),
                entity.getResidentialMoveInDate(),
                entity.getHouseholdId(),
                mapRelationshipToHead(entity.getRelationshipToHead()),
                mapResidencePrior(entity.getResidencePriorToEntry()),
                mapLengthOfStay(entity.getLengthOfStayPriorToEntry()),
                DisablingCondition.DATA_NOT_COLLECTED, // Would need to be stored in entity
                inferProjectTypeFromProgram(entity.getProgramId()) // Infer from program
            );
        } else {
            // Create regular enrollment
            ProgramEnrollment enrollment = ProgramEnrollment.create(
                new ClientId(entity.getClientId()),
                entity.getProgramId(),
                entity.getEnrollmentDate(),
                null, // relationshipToHead as CodeableConcept
                null, // residencePriorToEntry as CodeableConcept
                "system"
            );
            
            // Set additional fields
            enrollment.setProjectType(inferProjectTypeFromProgram(entity.getProgramId()));
            enrollment.updateHouseholdId(entity.getHouseholdId());
            enrollment.updateHmisRelationshipToHoH(mapRelationshipToHead(entity.getRelationshipToHead()));
            enrollment.updateHmisPriorLivingSituation(mapResidencePrior(entity.getResidencePriorToEntry()));
            enrollment.updateHmisLengthOfStay(mapLengthOfStay(entity.getLengthOfStayPriorToEntry()));
            
            return enrollment;
        }
    }
    
    /**
     * Get project type from program ID using the Program repository
     */
    private HmisProjectType inferProjectTypeFromProgram(UUID programId) {
        return programRepository.findById(programId)
            .map(Program::getHmisProjectType)
            .orElse(HmisProjectType.SERVICES_ONLY); // Safe default
    }
    
    // Status enum mapping no longer needed since we use domain enum directly
    
    private JpaProgramEnrollmentEntity.HmisRelationshipToHead mapRelationshipToHead(RelationshipToHeadOfHousehold relationship) {
        if (relationship == null) return JpaProgramEnrollmentEntity.HmisRelationshipToHead.DATA_NOT_COLLECTED;
        switch (relationship) {
            case SELF_HEAD_OF_HOUSEHOLD: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.SELF_HEAD_OF_HOUSEHOLD;
            case HEAD_OF_HOUSEHOLDS_SPOUSE_PARTNER: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.HEAD_OF_HOUSEHOLD_SPOUSE_PARTNER;
            case HEAD_OF_HOUSEHOLDS_CHILD: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.HEAD_OF_HOUSEHOLD_CHILD;
            case HEAD_OF_HOUSEHOLDS_OTHER_RELATION: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.OTHER_RELATIVE;
            case OTHER_NON_RELATION: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.UNRELATED_HOUSEHOLD_MEMBER;
            case CLIENT_DOESNT_KNOW: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.CLIENT_DOESNT_KNOW;
            case CLIENT_PREFERS_NOT_TO_ANSWER: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.DATA_NOT_COLLECTED;
            default: return JpaProgramEnrollmentEntity.HmisRelationshipToHead.DATA_NOT_COLLECTED;
        }
    }
    
    private RelationshipToHeadOfHousehold mapRelationshipToHead(JpaProgramEnrollmentEntity.HmisRelationshipToHead relationship) {
        if (relationship == null) return RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED;
        switch (relationship) {
            case SELF_HEAD_OF_HOUSEHOLD: return RelationshipToHeadOfHousehold.SELF_HEAD_OF_HOUSEHOLD;
            case HEAD_OF_HOUSEHOLD_SPOUSE_PARTNER: return RelationshipToHeadOfHousehold.HEAD_OF_HOUSEHOLDS_SPOUSE_PARTNER;
            case HEAD_OF_HOUSEHOLD_CHILD: return RelationshipToHeadOfHousehold.HEAD_OF_HOUSEHOLDS_CHILD;
            case OTHER_RELATIVE: return RelationshipToHeadOfHousehold.HEAD_OF_HOUSEHOLDS_OTHER_RELATION;
            case UNRELATED_HOUSEHOLD_MEMBER: return RelationshipToHeadOfHousehold.OTHER_NON_RELATION;
            case CLIENT_DOESNT_KNOW: return RelationshipToHeadOfHousehold.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED: return RelationshipToHeadOfHousehold.CLIENT_PREFERS_NOT_TO_ANSWER;
            case DATA_NOT_COLLECTED: return RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED;
            default: return RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED;
        }
    }
    
    private JpaProgramEnrollmentEntity.HmisResidencePrior mapResidencePrior(PriorLivingSituation situation) {
        if (situation == null) return JpaProgramEnrollmentEntity.HmisResidencePrior.DATA_NOT_COLLECTED;
        
        // Map specific situations to categories
        if (situation.isLiterallyHomeless()) {
            return JpaProgramEnrollmentEntity.HmisResidencePrior.HOMELESS_SITUATION;
        }
        if (situation.isInstitutional()) {
            return JpaProgramEnrollmentEntity.HmisResidencePrior.INSTITUTIONAL_SETTING;
        }
        if (situation.isPermanentHousing()) {
            return JpaProgramEnrollmentEntity.HmisResidencePrior.HOUSED;
        }
        
        switch (situation) {
            case CLIENT_DOESNT_KNOW: return JpaProgramEnrollmentEntity.HmisResidencePrior.CLIENT_DOESNT_KNOW;
            case CLIENT_PREFERS_NOT_TO_ANSWER: return JpaProgramEnrollmentEntity.HmisResidencePrior.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED: return JpaProgramEnrollmentEntity.HmisResidencePrior.DATA_NOT_COLLECTED;
            default: return JpaProgramEnrollmentEntity.HmisResidencePrior.DATA_NOT_COLLECTED;
        }
    }
    
    private PriorLivingSituation mapResidencePrior(JpaProgramEnrollmentEntity.HmisResidencePrior situation) {
        if (situation == null) return PriorLivingSituation.DATA_NOT_COLLECTED;
        switch (situation) {
            case HOMELESS_SITUATION: return PriorLivingSituation.PLACE_NOT_MEANT_FOR_HABITATION; // Default homeless
            case INSTITUTIONAL_SETTING: return PriorLivingSituation.PSYCHIATRIC_HOSPITAL; // Default institutional
            case HOUSED: return PriorLivingSituation.RENTAL_HOUSING; // Default housed
            case CLIENT_DOESNT_KNOW: return PriorLivingSituation.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED: return PriorLivingSituation.CLIENT_PREFERS_NOT_TO_ANSWER;
            case DATA_NOT_COLLECTED: return PriorLivingSituation.DATA_NOT_COLLECTED;
            default: return PriorLivingSituation.DATA_NOT_COLLECTED;
        }
    }
    
    private JpaProgramEnrollmentEntity.HmisLengthOfStay mapLengthOfStay(LengthOfStay lengthOfStay) {
        if (lengthOfStay == null) return JpaProgramEnrollmentEntity.HmisLengthOfStay.DATA_NOT_COLLECTED;
        switch (lengthOfStay) {
            case ONE_NIGHT_OR_LESS: return JpaProgramEnrollmentEntity.HmisLengthOfStay.ONE_WEEK_OR_LESS;
            case TWO_TO_SIX_NIGHTS: return JpaProgramEnrollmentEntity.HmisLengthOfStay.ONE_WEEK_OR_LESS;
            case ONE_WEEK_TO_LESS_THAN_ONE_MONTH: return JpaProgramEnrollmentEntity.HmisLengthOfStay.MORE_THAN_ONE_WEEK_BUT_LESS_THAN_ONE_MONTH;
            case ONE_MONTH_TO_LESS_THAN_THREE_MONTHS: return JpaProgramEnrollmentEntity.HmisLengthOfStay.ONE_TO_THREE_MONTHS;
            case THREE_MONTHS_TO_LESS_THAN_ONE_YEAR: return JpaProgramEnrollmentEntity.HmisLengthOfStay.MORE_THAN_THREE_MONTHS_BUT_LESS_THAN_ONE_YEAR;
            case ONE_YEAR_OR_LONGER: return JpaProgramEnrollmentEntity.HmisLengthOfStay.ONE_YEAR_OR_LONGER;
            case CLIENT_DOESNT_KNOW: return JpaProgramEnrollmentEntity.HmisLengthOfStay.CLIENT_DOESNT_KNOW;
            case CLIENT_PREFERS_NOT_TO_ANSWER: return JpaProgramEnrollmentEntity.HmisLengthOfStay.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED: return JpaProgramEnrollmentEntity.HmisLengthOfStay.DATA_NOT_COLLECTED;
            default: return JpaProgramEnrollmentEntity.HmisLengthOfStay.DATA_NOT_COLLECTED;
        }
    }
    
    private LengthOfStay mapLengthOfStay(JpaProgramEnrollmentEntity.HmisLengthOfStay lengthOfStay) {
        if (lengthOfStay == null) return LengthOfStay.DATA_NOT_COLLECTED;
        switch (lengthOfStay) {
            case ONE_WEEK_OR_LESS: return LengthOfStay.TWO_TO_SIX_NIGHTS; // Default short stay
            case MORE_THAN_ONE_WEEK_BUT_LESS_THAN_ONE_MONTH: return LengthOfStay.ONE_WEEK_TO_LESS_THAN_ONE_MONTH;
            case ONE_TO_THREE_MONTHS: return LengthOfStay.ONE_MONTH_TO_LESS_THAN_THREE_MONTHS;
            case MORE_THAN_THREE_MONTHS_BUT_LESS_THAN_ONE_YEAR: return LengthOfStay.THREE_MONTHS_TO_LESS_THAN_ONE_YEAR;
            case ONE_YEAR_OR_LONGER: return LengthOfStay.ONE_YEAR_OR_LONGER;
            case CLIENT_DOESNT_KNOW: return LengthOfStay.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED: return LengthOfStay.CLIENT_PREFERS_NOT_TO_ANSWER;
            case DATA_NOT_COLLECTED: return LengthOfStay.DATA_NOT_COLLECTED;
            default: return LengthOfStay.DATA_NOT_COLLECTED;
        }
    }
}
