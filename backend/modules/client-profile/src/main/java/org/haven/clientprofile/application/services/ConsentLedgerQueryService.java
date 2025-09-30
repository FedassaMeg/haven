package org.haven.clientprofile.application.services;

import org.haven.clientprofile.application.dto.*;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.haven.clientprofile.infrastructure.persistence.ConsentAuditTrailEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentAuditTrailRepository;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Query service for consent ledger operations
 * Provides read-only access to consent data with comprehensive filtering and export capabilities
 */
@Service
@Transactional(readOnly = true)
public class ConsentLedgerQueryService {
    
    private final ConsentLedgerRepository ledgerRepository;
    private final ConsentAuditTrailRepository auditRepository;
    
    public ConsentLedgerQueryService(ConsentLedgerRepository ledgerRepository, 
                                    ConsentAuditTrailRepository auditRepository) {
        this.ledgerRepository = ledgerRepository;
        this.auditRepository = auditRepository;
    }
    
    public Page<ConsentLedgerResponse> searchConsents(UUID clientId, ConsentType consentType, 
                                                     ConsentStatus status, String recipientOrganization,
                                                     Instant grantedAfter, Instant grantedBefore,
                                                     boolean includeVAWAProtected, Pageable pageable) {
        
        List<ConsentLedgerEntity> allResults = ledgerRepository.searchConsents(
            clientId, consentType, status, recipientOrganization, grantedAfter, grantedBefore
        );
        
        // Filter VAWA protected if not explicitly included
        if (!includeVAWAProtected) {
            allResults = allResults.stream()
                .filter(entity -> !entity.isVAWAProtected())
                .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());
        List<ConsentLedgerEntity> pageResults = allResults.subList(start, end);
        
        List<ConsentLedgerResponse> responses = pageResults.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, allResults.size());
    }
    
    public List<ConsentLedgerResponse> getActiveConsentsForClient(UUID clientId) {
        List<ConsentLedgerEntity> entities = ledgerRepository.findActiveConsentsForClient(
            clientId, ConsentStatus.GRANTED, Instant.now()
        );
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentLedgerResponse> getAllConsentsForClient(UUID clientId) {
        List<ConsentLedgerEntity> entities = ledgerRepository.findByClientIdOrderByGrantedAtDesc(clientId);
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentLedgerResponse> getConsentsExpiringSoon(int daysAhead) {
        Instant now = Instant.now();
        Instant reviewThreshold = now.plus(daysAhead, ChronoUnit.DAYS);
        
        List<ConsentLedgerEntity> entities = ledgerRepository.findConsentsRequiringReview(now, reviewThreshold);
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentLedgerResponse> getExpiredConsents() {
        List<ConsentLedgerEntity> entities = ledgerRepository.findExpiredConsents(
            ConsentStatus.GRANTED, Instant.now()
        );
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentLedgerResponse> getConsentsByRecipient(String recipientOrganization) {
        List<ConsentLedgerEntity> entities = ledgerRepository.findByRecipientOrganizationOrderByGrantedAtDesc(
            recipientOrganization
        );
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentLedgerResponse> getVAWAProtectedConsents() {
        List<ConsentLedgerEntity> entities = ledgerRepository.findVAWAProtectedConsents();
        
        return entities.stream()
            .map(ConsentLedgerResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public List<ConsentAuditResponse> getConsentAuditTrail(UUID consentId) {
        List<ConsentAuditTrailEntity> entities = auditRepository.findByConsentIdOrderByOccurredAtAsc(consentId);
        
        return entities.stream()
            .map(ConsentAuditResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public byte[] exportConsentLedger(UUID clientId, ConsentType consentType, ConsentStatus status,
                                     String recipientOrganization, Instant grantedAfter, 
                                     Instant grantedBefore, boolean includeVAWAProtected) {
        
        List<ConsentLedgerEntity> entities = ledgerRepository.searchConsents(
            clientId, consentType, status, recipientOrganization, grantedAfter, grantedBefore
        );
        
        // Filter VAWA protected if not explicitly included
        if (!includeVAWAProtected) {
            entities = entities.stream()
                .filter(entity -> !entity.isVAWAProtected())
                .collect(Collectors.toList());
        }
        
        return generateCSV(entities);
    }
    
    public ConsentStatisticsResponse getConsentStatistics() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant thirtyDaysFromNow = now.plus(30, ChronoUnit.DAYS);
        
        // Get basic counts
        long totalConsents = ledgerRepository.count();
        long activeConsents = ledgerRepository.findByStatusOrderByGrantedAtDesc(ConsentStatus.GRANTED).size();
        long revokedConsents = ledgerRepository.findByStatusOrderByGrantedAtDesc(ConsentStatus.REVOKED).size();
        long expiredConsents = ledgerRepository.findByStatusOrderByGrantedAtDesc(ConsentStatus.EXPIRED).size();
        long expiringSoonCount = ledgerRepository.findConsentsRequiringReview(now, thirtyDaysFromNow).size();
        long vAWAProtectedCount = ledgerRepository.findVAWAProtectedConsents().size();
        
        // Get type breakdown
        ConsentStatisticsResponse.ConsentTypeStatistics[] typeBreakdown = Arrays.stream(ConsentType.values())
            .map(type -> {
                List<ConsentLedgerEntity> typeConsents = ledgerRepository.findByConsentTypeOrderByGrantedAtDesc(type);
                long typeActiveCount = typeConsents.stream()
                    .filter(entity -> entity.getStatus() == ConsentStatus.GRANTED)
                    .count();
                return new ConsentStatisticsResponse.ConsentTypeStatistics(
                    type.name(), 
                    typeConsents.size(), 
                    typeActiveCount
                );
            })
            .toArray(ConsentStatisticsResponse.ConsentTypeStatistics[]::new);
        
        // Get recent activity
        List<ConsentAuditTrailEntity> recentGrants = auditRepository.findByOccurredAtBetween(thirtyDaysAgo, now)
            .stream()
            .filter(audit -> "ConsentGranted".equals(audit.getEventType()))
            .collect(Collectors.toList());
            
        List<ConsentAuditTrailEntity> recentRevocations = auditRepository.findByOccurredAtBetween(thirtyDaysAgo, now)
            .stream()
            .filter(audit -> "ConsentRevoked".equals(audit.getEventType()))
            .collect(Collectors.toList());
            
        List<ConsentAuditTrailEntity> recentExpirations = auditRepository.findByOccurredAtBetween(thirtyDaysAgo, now)
            .stream()
            .filter(audit -> "ConsentExpired".equals(audit.getEventType()))
            .collect(Collectors.toList());
        
        ConsentStatisticsResponse.RecentActivitySummary recentActivity = 
            new ConsentStatisticsResponse.RecentActivitySummary(
                recentGrants.size(),
                recentRevocations.size(),
                recentExpirations.size()
            );
        
        return new ConsentStatisticsResponse(
            totalConsents,
            activeConsents,
            revokedConsents,
            expiredConsents,
            expiringSoonCount,
            vAWAProtectedCount,
            typeBreakdown,
            recentActivity
        );
    }
    
    private byte[] generateCSV(List<ConsentLedgerEntity> entities) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {
            
            // Write CSV header
            writer.println("ID,Client ID,Consent Type,Status,Purpose,Recipient Organization," +
                          "Recipient Contact,Granted At,Expires At,Revoked At,Granted By User ID," +
                          "Revoked By User ID,Revocation Reason,Is VAWA Protected,Limitations,Last Updated At");
            
            // Write data rows
            for (ConsentLedgerEntity entity : entities) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%b,\"%s\",\"%s\"%n",
                    csvEscape(entity.getId().toString()),
                    csvEscape(entity.getClientId().toString()),
                    csvEscape(entity.getConsentType().toString()),
                    csvEscape(entity.getStatus().toString()),
                    csvEscape(entity.getPurpose()),
                    csvEscape(entity.getRecipientOrganization()),
                    csvEscape(entity.getRecipientContact()),
                    entity.getGrantedAt(),
                    entity.getExpiresAt(),
                    entity.getRevokedAt(),
                    entity.getGrantedByUserId(),
                    entity.getRevokedByUserId(),
                    csvEscape(entity.getRevocationReason()),
                    entity.isVAWAProtected(),
                    csvEscape(entity.getLimitations()),
                    entity.getLastUpdatedAt()
                );
            }
            
            writer.flush();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }
    
    private String csvEscape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}