package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.RestrictedNote;
import org.haven.casemgmt.domain.RestrictedNoteId;
import org.haven.casemgmt.domain.RestrictedNoteRepository;
import org.haven.eventstore.EventEnvelope;
import org.haven.eventstore.EventStore;
import org.haven.shared.events.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EventSourcedRestrictedNoteRepository implements RestrictedNoteRepository {
    
    private final EventStore eventStore;
    private final RestrictedNoteReadModelRepository readModelRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired
    public EventSourcedRestrictedNoteRepository(EventStore eventStore, RestrictedNoteReadModelRepository readModelRepository, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.readModelRepository = readModelRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Optional<RestrictedNote> findById(RestrictedNoteId noteId) {
        List<EventEnvelope<? extends DomainEvent>> events = eventStore.load(noteId.value());
        
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        RestrictedNote note = RestrictedNote.reconstruct();
        for (EventEnvelope<? extends DomainEvent> envelope : events) {
            note.replay(envelope.event(), envelope.sequence());
        }
        
        return Optional.of(note);
    }
    
    @Override
    public void save(RestrictedNote aggregate) {
        List<DomainEvent> pendingEvents = aggregate.getPendingEvents();
        
        if (!pendingEvents.isEmpty()) {
            eventStore.append(aggregate.getId().value(), aggregate.getVersion() - pendingEvents.size(), pendingEvents);
            
            // Publish events for projection handlers
            for (DomainEvent event : pendingEvents) {
                eventPublisher.publishEvent(event);
            }
            
            aggregate.clearPendingEvents();
        }
    }
    
    @Override
    public void delete(RestrictedNote aggregate) {
        // In event sourcing, we typically don't delete but could add a "deleted" event
        // For now, this is a no-op since we're using pure event sourcing
    }
    
    @Override
    public RestrictedNoteId nextId() {
        return RestrictedNoteId.newId();
    }
    
    @Override
    public List<RestrictedNote> findByClientId(UUID clientId) {
        return readModelRepository.findByClientIdOrderByCreatedAtDesc(clientId)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByCaseId(UUID caseId) {
        return readModelRepository.findByCaseIdOrderByCreatedAtDesc(caseId)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByAuthorId(UUID authorId) {
        return readModelRepository.findByAuthorIdOrderByCreatedAtDesc(authorId)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByNoteType(RestrictedNote.NoteType noteType) {
        RestrictedNoteReadModel.NoteType readModelType = mapToReadModelNoteType(noteType);
        return readModelRepository.findByNoteTypeOrderByCreatedAtDesc(readModelType)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByVisibilityScope(RestrictedNote.VisibilityScope scope) {
        RestrictedNoteReadModel.VisibilityScope readModelScope = mapToReadModelVisibilityScope(scope);
        return readModelRepository.findByVisibilityScopeOrderByCreatedAtDesc(readModelScope)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findSealedNotes() {
        return readModelRepository.findByIsSealedOrderByCreatedAtDesc(true)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findExpiredTemporarySeals(Instant asOf) {
        return readModelRepository.findByIsTemporaryTrueAndExpiresAtBeforeOrderByExpiresAt(asOf)
            .stream()
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findAccessibleToUser(UUID userId, List<String> userRoles) {
        List<RestrictedNoteReadModel.VisibilityScope> allowedScopes = getAllowedScopes(userRoles);
        return readModelRepository.findAll()
            .stream()
            .filter(readModel -> isAccessible(readModel, userId, userRoles, allowedScopes))
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByClientIdAccessibleToUser(UUID clientId, UUID userId, List<String> userRoles) {
        List<RestrictedNoteReadModel.VisibilityScope> allowedScopes = getAllowedScopes(userRoles);
        return readModelRepository.findByClientIdAndVisibilityScopeIn(clientId, allowedScopes)
            .stream()
            .filter(readModel -> isAccessible(readModel, userId, userRoles, allowedScopes))
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByCaseIdAccessibleToUser(UUID caseId, UUID userId, List<String> userRoles) {
        List<RestrictedNoteReadModel.VisibilityScope> allowedScopes = getAllowedScopes(userRoles);
        return readModelRepository.findByCaseIdAndVisibilityScopeIn(caseId, allowedScopes)
            .stream()
            .filter(readModel -> isAccessible(readModel, userId, userRoles, allowedScopes))
            .map(this::reconstructFromReadModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasValidAccess(UUID noteId, UUID userId, List<String> userRoles) {
        Optional<RestrictedNote> note = findById(RestrictedNoteId.of(noteId));
        return note.isPresent() && note.get().isVisibleTo(userId, userRoles);
    }
    
    private Optional<RestrictedNote> reconstructFromReadModel(RestrictedNoteReadModel readModel) {
        return findById(RestrictedNoteId.of(readModel.getNoteId()));
    }
    
    private RestrictedNoteReadModel.NoteType mapToReadModelNoteType(RestrictedNote.NoteType domainType) {
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
    
    private RestrictedNoteReadModel.VisibilityScope mapToReadModelVisibilityScope(RestrictedNote.VisibilityScope domainScope) {
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
    
    private List<RestrictedNoteReadModel.VisibilityScope> getAllowedScopes(List<String> userRoles) {
        List<RestrictedNoteReadModel.VisibilityScope> allowedScopes = new ArrayList<>();
        
        allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.PUBLIC);
        allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.CASE_TEAM);
        
        if (userRoles.contains("CLINICIAN") || userRoles.contains("COUNSELOR")) {
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.CLINICAL_ONLY);
        }
        
        if (userRoles.contains("ATTORNEY") || userRoles.contains("LEGAL_ADVOCATE")) {
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.LEGAL_TEAM);
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.ATTORNEY_CLIENT);
        }
        
        if (userRoles.contains("SAFETY_COORDINATOR")) {
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.SAFETY_TEAM);
        }
        
        if (userRoles.contains("DOCTOR") || userRoles.contains("NURSE") || userRoles.contains("MEDICAL_ADVOCATE")) {
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.MEDICAL_TEAM);
        }
        
        if (userRoles.contains("ADMINISTRATOR") || userRoles.contains("HMIS_LEAD")) {
            allowedScopes.add(RestrictedNoteReadModel.VisibilityScope.ADMIN_ONLY);
        }
        
        return allowedScopes;
    }
    
    private boolean isAccessible(RestrictedNoteReadModel readModel, UUID userId, List<String> userRoles, List<RestrictedNoteReadModel.VisibilityScope> allowedScopes) {
        if (readModel.isSealed()) {
            return userRoles.contains("ADMINISTRATOR") || userRoles.contains("COMPLIANCE_OFFICER");
        }
        
        if (readModel.getVisibilityScope() == RestrictedNoteReadModel.VisibilityScope.AUTHOR_ONLY) {
            return readModel.getAuthorId().equals(userId);
        }
        
        return allowedScopes.contains(readModel.getVisibilityScope());
    }
}