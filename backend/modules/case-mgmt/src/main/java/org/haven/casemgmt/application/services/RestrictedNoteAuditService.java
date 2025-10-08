package org.haven.casemgmt.application.services;

import org.haven.casemgmt.domain.events.*;
import org.haven.eventstore.EventEnvelope;
import org.haven.eventstore.EventStore;
import org.haven.shared.events.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for querying audit logs of restricted note activities
 * Provides compliance reporting and access tracking
 */
@Service
public class RestrictedNoteAuditService {
    
    private final EventStore eventStore;
    
    @Autowired
    public RestrictedNoteAuditService(EventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    /**
     * Get complete audit trail for a specific note
     */
    public List<AuditLogEntry> getAuditTrailForNote(UUID noteId) {
        List<EventEnvelope<? extends DomainEvent>> events = eventStore.load(noteId);
        
        return events.stream()
            .map(this::toAuditLogEntry)
            .collect(Collectors.toList());
    }
    
    /**
     * Get audit trail for multiple notes (e.g., for a client or case)
     */
    public List<AuditLogEntry> getAuditTrailForNotes(List<UUID> noteIds) {
        return noteIds.stream()
            .flatMap(noteId -> getAuditTrailForNote(noteId).stream())
            .sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get access events for compliance reporting
     */
    public List<AuditLogEntry> getAccessEvents(UUID noteId) {
        return getAuditTrailForNote(noteId).stream()
            .filter(entry -> "RestrictedNoteAccessed".equals(entry.getEventType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get seal/unseal events for a note
     */
    public List<AuditLogEntry> getSealEvents(UUID noteId) {
        return getAuditTrailForNote(noteId).stream()
            .filter(entry -> "RestrictedNoteSealed".equals(entry.getEventType()) || 
                           "RestrictedNoteUnsealed".equals(entry.getEventType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get modification events for a note
     */
    public List<AuditLogEntry> getModificationEvents(UUID noteId) {
        return getAuditTrailForNote(noteId).stream()
            .filter(entry -> "RestrictedNoteCreated".equals(entry.getEventType()) || 
                           "RestrictedNoteUpdated".equals(entry.getEventType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get audit events by date range
     */
    public List<AuditLogEntry> getAuditEventsByDateRange(UUID noteId, Instant startDate, Instant endDate) {
        return getAuditTrailForNote(noteId).stream()
            .filter(entry -> !entry.getOccurredAt().isBefore(startDate) && 
                           !entry.getOccurredAt().isAfter(endDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Get audit events by user
     */
    public List<AuditLogEntry> getAuditEventsByUser(UUID noteId, UUID userId) {
        return getAuditTrailForNote(noteId).stream()
            .filter(entry -> userId.equals(entry.getPerformedBy()))
            .collect(Collectors.toList());
    }
    
    /**
     * Generate compliance report for a note
     */
    public ComplianceReport generateComplianceReport(UUID noteId) {
        List<AuditLogEntry> auditTrail = getAuditTrailForNote(noteId);
        
        ComplianceReport report = new ComplianceReport();
        report.setNoteId(noteId);
        report.setGeneratedAt(Instant.now());
        report.setTotalEvents(auditTrail.size());
        
        // Count events by type
        long accessEvents = auditTrail.stream()
            .filter(entry -> "RestrictedNoteAccessed".equals(entry.getEventType()))
            .count();
        long modificationEvents = auditTrail.stream()
            .filter(entry -> "RestrictedNoteCreated".equals(entry.getEventType()) || 
                           "RestrictedNoteUpdated".equals(entry.getEventType()))
            .count();
        long sealEvents = auditTrail.stream()
            .filter(entry -> "RestrictedNoteSealed".equals(entry.getEventType()) || 
                           "RestrictedNoteUnsealed".equals(entry.getEventType()))
            .count();
        
        report.setAccessEventCount(accessEvents);
        report.setModificationEventCount(modificationEvents);
        report.setSealEventCount(sealEvents);
        
        // Get unique users who accessed the note
        List<UUID> uniqueUsers = auditTrail.stream()
            .map(AuditLogEntry::getPerformedBy)
            .filter(userId -> userId != null)
            .distinct()
            .collect(Collectors.toList());
        report.setUniqueUsersAccessed(uniqueUsers.size());
        
        // Check for any policy violations
        report.setHasPolicyViolations(checkForPolicyViolations(auditTrail));
        
        return report;
    }
    
    private AuditLogEntry toAuditLogEntry(EventEnvelope<? extends DomainEvent> envelope) {
        DomainEvent event = envelope.event();
        AuditLogEntry entry = new AuditLogEntry();
        
        entry.setNoteId(event.aggregateId());
        entry.setEventType(event.eventType());
        entry.setOccurredAt(event.occurredAt());
        entry.setSequence(envelope.sequence());
        
        // Extract user information based on event type
        switch (event.eventType()) {
            case "RestrictedNoteCreated":
                RestrictedNoteCreated created = (RestrictedNoteCreated) event;
                entry.setPerformedBy(created.getAuthorId());
                entry.setPerformedByName(created.getAuthorName());
                entry.setDetails("Note created: " + created.getTitle());
                break;
                
            case "RestrictedNoteUpdated":
                RestrictedNoteUpdated updated = (RestrictedNoteUpdated) event;
                entry.setPerformedBy(updated.getUpdatedBy());
                entry.setPerformedByName(updated.getUpdatedByName());
                entry.setDetails("Note updated: " + updated.getUpdateReason());
                break;
                
            case "RestrictedNoteSealed":
                RestrictedNoteSealed sealed = (RestrictedNoteSealed) event;
                entry.setPerformedBy(sealed.getSealedBy());
                entry.setPerformedByName(sealed.getSealedByName());
                entry.setDetails("Note sealed: " + sealed.getSealReason());
                break;
                
            case "RestrictedNoteUnsealed":
                RestrictedNoteUnsealed unsealed = (RestrictedNoteUnsealed) event;
                entry.setPerformedBy(unsealed.getUnsealedBy());
                entry.setPerformedByName(unsealed.getUnsealedByName());
                entry.setDetails("Note unsealed: " + unsealed.getUnsealReason());
                break;
                
            case "RestrictedNoteAccessed":
                RestrictedNoteAccessed accessed = (RestrictedNoteAccessed) event;
                entry.setPerformedBy(accessed.getAccessedBy());
                entry.setPerformedByName(accessed.getAccessedByName());
                entry.setUserRoles(accessed.getUserRoles());
                entry.setAccessMethod(accessed.getAccessMethod());
                entry.setIpAddress(accessed.getIpAddress());
                entry.setUserAgent(accessed.getUserAgent());
                entry.setContentViewed(accessed.wasContentViewed());
                entry.setDetails("Note accessed: " + accessed.getAccessReason());
                break;
        }
        
        return entry;
    }
    
    private boolean checkForPolicyViolations(List<AuditLogEntry> auditTrail) {
        // Implement policy violation checks
        // For example: unauthorized access attempts, suspicious patterns, etc.
        
        // Check for access after hours (example policy)
        long afterHoursAccess = auditTrail.stream()
            .filter(entry -> "RestrictedNoteAccessed".equals(entry.getEventType()))
            .filter(entry -> isAfterHours(entry.getOccurredAt()))
            .count();
        
        return afterHoursAccess > 0;
    }
    
    private boolean isAfterHours(Instant timestamp) {
        // Simple check for access between 10 PM and 6 AM
        int hour = timestamp.atZone(java.time.ZoneId.systemDefault()).getHour();
        return hour >= 22 || hour < 6;
    }
    
    /**
     * Data class for audit log entries
     */
    public static class AuditLogEntry {
        private UUID noteId;
        private String eventType;
        private UUID performedBy;
        private String performedByName;
        private List<String> userRoles;
        private Instant occurredAt;
        private long sequence;
        private String details;
        private String accessMethod;
        private String ipAddress;
        private String userAgent;
        private boolean contentViewed;
        
        // Getters and setters
        public UUID getNoteId() { return noteId; }
        public void setNoteId(UUID noteId) { this.noteId = noteId; }
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public UUID getPerformedBy() { return performedBy; }
        public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
        
        public String getPerformedByName() { return performedByName; }
        public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
        
        public List<String> getUserRoles() { return userRoles; }
        public void setUserRoles(List<String> userRoles) { this.userRoles = userRoles; }
        
        public Instant getOccurredAt() { return occurredAt; }
        public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
        
        public long getSequence() { return sequence; }
        public void setSequence(long sequence) { this.sequence = sequence; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public String getAccessMethod() { return accessMethod; }
        public void setAccessMethod(String accessMethod) { this.accessMethod = accessMethod; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public boolean isContentViewed() { return contentViewed; }
        public void setContentViewed(boolean contentViewed) { this.contentViewed = contentViewed; }
    }
    
    /**
     * Data class for compliance reports
     */
    public static class ComplianceReport {
        private UUID noteId;
        private Instant generatedAt;
        private int totalEvents;
        private long accessEventCount;
        private long modificationEventCount;
        private long sealEventCount;
        private int uniqueUsersAccessed;
        private boolean hasPolicyViolations;
        private List<String> policyViolationDetails;
        
        // Getters and setters
        public UUID getNoteId() { return noteId; }
        public void setNoteId(UUID noteId) { this.noteId = noteId; }
        
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
        
        public int getTotalEvents() { return totalEvents; }
        public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
        
        public long getAccessEventCount() { return accessEventCount; }
        public void setAccessEventCount(long accessEventCount) { this.accessEventCount = accessEventCount; }
        
        public long getModificationEventCount() { return modificationEventCount; }
        public void setModificationEventCount(long modificationEventCount) { this.modificationEventCount = modificationEventCount; }
        
        public long getSealEventCount() { return sealEventCount; }
        public void setSealEventCount(long sealEventCount) { this.sealEventCount = sealEventCount; }
        
        public int getUniqueUsersAccessed() { return uniqueUsersAccessed; }
        public void setUniqueUsersAccessed(int uniqueUsersAccessed) { this.uniqueUsersAccessed = uniqueUsersAccessed; }
        
        public boolean isHasPolicyViolations() { return hasPolicyViolations; }
        public void setHasPolicyViolations(boolean hasPolicyViolations) { this.hasPolicyViolations = hasPolicyViolations; }
        
        public List<String> getPolicyViolationDetails() { return policyViolationDetails; }
        public void setPolicyViolationDetails(List<String> policyViolationDetails) { this.policyViolationDetails = policyViolationDetails; }
    }
}