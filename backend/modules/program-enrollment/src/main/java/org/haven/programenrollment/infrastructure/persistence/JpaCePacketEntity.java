package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CePacket;
import org.haven.programenrollment.domain.ce.CePacketId;
import org.haven.programenrollment.domain.ce.CeShareScope;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA entity mapping the ce_packets consent snapshot table.
 */
@Entity
@Table(name = "ce_packets")
public class JpaCePacketEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false)
    private ConsentStatus consentStatus;

    @Column(name = "consent_version", nullable = false)
    private Long consentVersion;

    @Column(name = "consent_effective_at", nullable = false)
    private Instant consentEffectiveAt;

    @Column(name = "consent_expires_at")
    private Instant consentExpiresAt;

    @Column(name = "client_hash", nullable = false, length = 128)
    private String clientHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "hash_algorithm", nullable = false)
    private CeHashAlgorithm hashAlgorithm;

    @Column(name = "hash_salt", nullable = false)
    private byte[] hashSalt;

    @Column(name = "hash_iterations", nullable = false)
    private Integer hashIterations;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_share_scopes", columnDefinition = "ce_share_scope[]", nullable = false)
    private CeShareScope[] allowedShareScopes;

    @Column(name = "encryption_scheme", nullable = false, length = 50)
    private String encryptionScheme;

    @Column(name = "encryption_key_id", nullable = false, length = 100)
    private String encryptionKeyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "encryption_metadata", nullable = false)
    private Map<String, String> encryptionMetadata;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "encryption_tags", columnDefinition = "text[]", nullable = false)
    private String[] encryptionTags;

    @Column(name = "packet_checksum", nullable = false, length = 128)
    private String packetChecksum;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaCePacketEntity() {
        // JPA
    }

    public JpaCePacketEntity(CePacket packet) {
        this.id = packet.getPacketId().value();
        this.clientId = packet.getClientId().value();
        this.enrollmentId = packet.getEnrollmentId() != null ? packet.getEnrollmentId().value() : null;
        this.consentId = packet.getConsentId().value();
        this.consentStatus = packet.getConsentStatus();
        this.consentVersion = packet.getConsentVersion();
        this.consentEffectiveAt = packet.getConsentEffectiveAt();
        this.consentExpiresAt = packet.getConsentExpiresAt();
        this.clientHash = packet.getClientHash();
        this.hashAlgorithm = packet.getHashAlgorithm();
        this.hashSalt = packet.getHashSalt();
        this.hashIterations = packet.getHashIterations();
        this.allowedShareScopes = packet.getAllowedShareScopes().toArray(CeShareScope[]::new);
        this.encryptionScheme = packet.getEncryptionScheme();
        this.encryptionKeyId = packet.getEncryptionKeyId();
        this.encryptionMetadata = packet.getEncryptionMetadata();
        this.encryptionTags = packet.getEncryptionTags().toArray(String[]::new);
        this.packetChecksum = packet.getPacketChecksum();
        this.ledgerEntryId = packet.getLedgerEntryId();
        this.createdAt = packet.getCreatedAt();
        this.updatedAt = packet.getUpdatedAt();
    }

    public CePacket toDomain() {
        var builder = CePacket.builder()
            .packetId(CePacketId.of(id))
            .clientId(new ClientId(clientId))
            .consentId(new ConsentId(consentId))
            .consentStatus(consentStatus)
            .consentVersion(consentVersion)
            .consentEffectiveAt(consentEffectiveAt)
            .consentExpiresAt(consentExpiresAt)
            .clientHash(clientHash)
            .hashAlgorithm(hashAlgorithm)
            .hashSalt(hashSalt)
            .hashIterations(hashIterations)
            .allowedShareScopes(resolveScopes())
            .encryptionScheme(encryptionScheme)
            .encryptionKeyId(encryptionKeyId)
            .encryptionMetadata(encryptionMetadata)
            .encryptionTags(Arrays.asList(encryptionTags == null ? new String[0] : encryptionTags))
            .packetChecksum(packetChecksum)
            .ledgerEntryId(ledgerEntryId)
            .createdAt(createdAt)
            .updatedAt(updatedAt);

        if (enrollmentId != null) {
            builder.enrollmentId(ProgramEnrollmentId.of(enrollmentId));
        }

        return builder.build();
    }

    private EnumSet<CeShareScope> resolveScopes() {
        if (allowedShareScopes == null || allowedShareScopes.length == 0) {
            return EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY);
        }
        return Arrays.stream(allowedShareScopes)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(CeShareScope.class)));
    }

    public UUID getId() {
        return id;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
