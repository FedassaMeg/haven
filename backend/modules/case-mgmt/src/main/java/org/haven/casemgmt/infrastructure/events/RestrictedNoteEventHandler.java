package org.haven.casemgmt.infrastructure.events;

import org.haven.casemgmt.domain.events.*;
import org.haven.casemgmt.infrastructure.persistence.RestrictedNoteReadModel;
import org.haven.casemgmt.infrastructure.persistence.RestrictedNoteReadModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

import java.time.Instant;

@Component
public class RestrictedNoteEventHandler {
    
    private final RestrictedNoteReadModelRepository readModelRepository;
    
    @Autowired
    public RestrictedNoteEventHandler(RestrictedNoteReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }
    
    @EventListener
    @Transactional
    public void handle(RestrictedNoteCreated event) {
        RestrictedNoteReadModel readModel = new RestrictedNoteReadModel(
            event.aggregateId(),
            event.getClientId(),
            event.getClientName(),
            event.getCaseId(),
            event.getCaseNumber(),
            mapNoteType(event.getNoteType()),
            event.getContent(),
            event.getTitle(),
            event.getAuthorId(),
            event.getAuthorName(),
            event.occurredAt(),
            mapVisibilityScope(event.getVisibilityScope())
        );
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void handle(RestrictedNoteUpdated event) {
        readModelRepository.findById(event.aggregateId())
            .ifPresent(readModel -> {
                readModel.setContent(event.getContent());
                readModel.setLastModified(event.occurredAt());
                readModel.setVisibilityScope(mapVisibilityScope(event.getVisibilityScope()));
                readModelRepository.save(readModel);
            });
    }
    
    @EventListener
    @Transactional
    public void handle(RestrictedNoteSealed event) {
        readModelRepository.findById(event.aggregateId())
            .ifPresent(readModel -> {
                readModel.setSealed(true);
                readModel.setSealReason(event.getSealReason());
                readModel.setSealedAt(event.occurredAt());
                readModel.setSealedBy(event.getSealedBy());
                readModel.setSealedByName(event.getSealedByName());
                readModel.setLastModified(event.occurredAt());
                readModelRepository.save(readModel);
            });
    }
    
    @EventListener
    @Transactional
    public void handle(RestrictedNoteUnsealed event) {
        readModelRepository.findById(event.aggregateId())
            .ifPresent(readModel -> {
                readModel.setSealed(false);
                readModel.setSealReason(null);
                readModel.setSealedAt(null);
                readModel.setSealedBy(null);
                readModel.setSealedByName(null);
                readModel.setLastModified(event.occurredAt());
                readModelRepository.save(readModel);
            });
    }
    
    
    private RestrictedNoteReadModel.NoteType mapNoteType(String noteTypeString) {
        org.haven.casemgmt.domain.RestrictedNote.NoteType domainType;
        try {
            domainType = org.haven.casemgmt.domain.RestrictedNote.NoteType.valueOf(noteTypeString);
        } catch (IllegalArgumentException e) {
            domainType = org.haven.casemgmt.domain.RestrictedNote.NoteType.STANDARD;
        }
        return switch (domainType) {
            case STANDARD -> RestrictedNoteReadModel.NoteType.STANDARD;
            case COUNSELING -> RestrictedNoteReadModel.NoteType.COUNSELING;
            case PRIVILEGED_COUNSELING -> RestrictedNoteReadModel.NoteType.PRIVILEGED_COUNSELING;
            case LEGAL_ADVOCACY -> RestrictedNoteReadModel.NoteType.LEGAL_ADVOCACY;
            case ATTORNEY_CLIENT -> RestrictedNoteReadModel.NoteType.ATTORNEY_CLIENT;
            case SAFETY_PLAN -> RestrictedNoteReadModel.NoteType.SAFETY_PLAN;
            case MEDICAL -> RestrictedNoteReadModel.NoteType.MEDICAL;
            case THERAPEUTIC -> RestrictedNoteReadModel.NoteType.THERAPEUTIC;
            case INTERNAL_ADMIN -> RestrictedNoteReadModel.NoteType.INTERNAL_ADMIN;
            case WORKFLOW_PROGRESS -> RestrictedNoteReadModel.NoteType.WORKFLOW_PROGRESS;
            case COMPLIANCE_VERIFICATION -> RestrictedNoteReadModel.NoteType.COMPLIANCE_VERIFICATION;
            case ALERT -> RestrictedNoteReadModel.NoteType.ALERT;
            case INVESTIGATION_UPDATE -> RestrictedNoteReadModel.NoteType.INVESTIGATION_UPDATE;
            case MANDATED_REPORT -> RestrictedNoteReadModel.NoteType.MANDATED_REPORT;
        };
    }
    
    private RestrictedNoteReadModel.VisibilityScope mapVisibilityScope(String visibilityScopeString) {
        org.haven.casemgmt.domain.RestrictedNote.VisibilityScope domainScope;
        try {
            domainScope = org.haven.casemgmt.domain.RestrictedNote.VisibilityScope.valueOf(visibilityScopeString);
        } catch (IllegalArgumentException e) {
            domainScope = org.haven.casemgmt.domain.RestrictedNote.VisibilityScope.PUBLIC;
        }
        return switch (domainScope) {
            case PUBLIC -> RestrictedNoteReadModel.VisibilityScope.PUBLIC;
            case CASE_TEAM -> RestrictedNoteReadModel.VisibilityScope.CASE_TEAM;
            case CLINICAL_ONLY -> RestrictedNoteReadModel.VisibilityScope.CLINICAL_ONLY;
            case LEGAL_TEAM -> RestrictedNoteReadModel.VisibilityScope.LEGAL_TEAM;
            case SAFETY_TEAM -> RestrictedNoteReadModel.VisibilityScope.SAFETY_TEAM;
            case MEDICAL_TEAM -> RestrictedNoteReadModel.VisibilityScope.MEDICAL_TEAM;
            case ADMIN_ONLY -> RestrictedNoteReadModel.VisibilityScope.ADMIN_ONLY;
            case AUTHOR_ONLY -> RestrictedNoteReadModel.VisibilityScope.AUTHOR_ONLY;
            case ATTORNEY_CLIENT -> RestrictedNoteReadModel.VisibilityScope.ATTORNEY_CLIENT;
            case CUSTOM -> RestrictedNoteReadModel.VisibilityScope.CUSTOM;
        };
    }
}