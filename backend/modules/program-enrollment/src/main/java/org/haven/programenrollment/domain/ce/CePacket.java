package org.haven.programenrollment.domain.ce;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable snapshot describing a consent-scoped Coordinated Entry packet.
 * The packet stores hashed identifiers plus encryption metadata required for exports.
 */
public class CePacket {

    private final CePacketId packetId;
    private final ClientId clientId;
    private final ProgramEnrollmentId enrollmentId;
    private final ConsentId consentId;
    private final ConsentStatus consentStatus;
    private final long consentVersion;
    private final Instant consentEffectiveAt;
    private final Instant consentExpiresAt;
    private final String clientHash;
    private final CeHashAlgorithm hashAlgorithm;
    private final byte[] hashSalt;
    private final int hashIterations;
    private final Set<CeShareScope> allowedShareScopes;
    private final String encryptionScheme;
    private final String encryptionKeyId;
    private final Map<String, String> encryptionMetadata;
    private final List<String> encryptionTags;
    private final String packetChecksum;
    private final UUID ledgerEntryId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CePacket(Builder builder) {
        this.packetId = builder.packetId;
        this.clientId = builder.clientId;
        this.enrollmentId = builder.enrollmentId;
        this.consentId = builder.consentId;
        this.consentStatus = builder.consentStatus;
        this.consentVersion = builder.consentVersion;
        this.consentEffectiveAt = builder.consentEffectiveAt;
        this.consentExpiresAt = builder.consentExpiresAt;
        this.clientHash = builder.clientHash;
        this.hashAlgorithm = builder.hashAlgorithm;
        this.hashSalt = builder.hashSalt == null ? null : builder.hashSalt.clone();
        this.hashIterations = builder.hashIterations;
        this.allowedShareScopes = Collections.unmodifiableSet(EnumSet.copyOf(builder.allowedShareScopes));
        this.encryptionScheme = builder.encryptionScheme;
        this.encryptionKeyId = builder.encryptionKeyId;
        this.encryptionMetadata = Collections.unmodifiableMap(builder.encryptionMetadata);
        this.encryptionTags = List.copyOf(builder.encryptionTags);
        this.packetChecksum = builder.packetChecksum;
        this.ledgerEntryId = builder.ledgerEntryId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CePacketId getPacketId() {
        return packetId;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public ProgramEnrollmentId getEnrollmentId() {
        return enrollmentId;
    }

    public ConsentId getConsentId() {
        return consentId;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public long getConsentVersion() {
        return consentVersion;
    }

    public Instant getConsentEffectiveAt() {
        return consentEffectiveAt;
    }

    public Instant getConsentExpiresAt() {
        return consentExpiresAt;
    }

    public String getClientHash() {
        return clientHash;
    }

    public CeHashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public byte[] getHashSalt() {
        return hashSalt == null ? null : hashSalt.clone();
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public Set<CeShareScope> getAllowedShareScopes() {
        return allowedShareScopes;
    }

    public String getEncryptionScheme() {
        return encryptionScheme;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public Map<String, String> getEncryptionMetadata() {
        return encryptionMetadata;
    }

    public List<String> getEncryptionTags() {
        return encryptionTags;
    }

    public String getPacketChecksum() {
        return packetChecksum;
    }

    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean allowsScope(CeShareScope scope) {
        return allowedShareScopes.contains(scope);
    }

    public boolean isConsentActive() {
        return consentStatus == ConsentStatus.GRANTED &&
            (consentExpiresAt == null || consentExpiresAt.isAfter(Instant.now()));
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CePacket)) return false;
        CePacket other = (CePacket) o;
        return Objects.equals(packetId, other.packetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetId);
    }

    public static final class Builder {
        private CePacketId packetId = CePacketId.newId();
        private ClientId clientId;
        private ProgramEnrollmentId enrollmentId;
        private ConsentId consentId;
        private ConsentStatus consentStatus;
        private long consentVersion;
        private Instant consentEffectiveAt;
        private Instant consentExpiresAt;
        private String clientHash;
        private CeHashAlgorithm hashAlgorithm = CeHashAlgorithm.SHA256_SALT;
        private byte[] hashSalt;
        private int hashIterations;
        private EnumSet<CeShareScope> allowedShareScopes = EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY);
        private String encryptionScheme = "AES-256-GCM";
        private String encryptionKeyId;
        private Map<String, String> encryptionMetadata = Map.of();
        private List<String> encryptionTags = List.of();
        private String packetChecksum;
        private UUID ledgerEntryId;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        private Builder() {}

        private Builder(CePacket existing) {
            this.packetId = existing.packetId;
            this.clientId = existing.clientId;
            this.enrollmentId = existing.enrollmentId;
            this.consentId = existing.consentId;
            this.consentStatus = existing.consentStatus;
            this.consentVersion = existing.consentVersion;
            this.consentEffectiveAt = existing.consentEffectiveAt;
            this.consentExpiresAt = existing.consentExpiresAt;
            this.clientHash = existing.clientHash;
            this.hashAlgorithm = existing.hashAlgorithm;
            this.hashSalt = existing.getHashSalt();
            this.hashIterations = existing.hashIterations;
            this.allowedShareScopes = EnumSet.copyOf(existing.allowedShareScopes);
            this.encryptionScheme = existing.encryptionScheme;
            this.encryptionKeyId = existing.encryptionKeyId;
            this.encryptionMetadata = existing.encryptionMetadata;
            this.encryptionTags = existing.encryptionTags;
            this.packetChecksum = existing.packetChecksum;
            this.ledgerEntryId = existing.ledgerEntryId;
            this.createdAt = existing.createdAt;
            this.updatedAt = existing.updatedAt;
        }

        public Builder packetId(CePacketId packetId) {
            this.packetId = packetId;
            return this;
        }

        public Builder clientId(ClientId clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder enrollmentId(ProgramEnrollmentId enrollmentId) {
            this.enrollmentId = enrollmentId;
            return this;
        }

        public Builder consentId(ConsentId consentId) {
            this.consentId = consentId;
            return this;
        }

        public Builder consentStatus(ConsentStatus consentStatus) {
            this.consentStatus = consentStatus;
            return this;
        }

        public Builder consentVersion(long consentVersion) {
            this.consentVersion = consentVersion;
            return this;
        }

        public Builder consentEffectiveAt(Instant consentEffectiveAt) {
            this.consentEffectiveAt = consentEffectiveAt;
            return this;
        }

        public Builder consentExpiresAt(Instant consentExpiresAt) {
            this.consentExpiresAt = consentExpiresAt;
            return this;
        }

        public Builder clientHash(String clientHash) {
            this.clientHash = clientHash;
            return this;
        }

        public Builder hashAlgorithm(CeHashAlgorithm hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
            return this;
        }

        public Builder hashSalt(byte[] hashSalt) {
            this.hashSalt = hashSalt == null ? null : hashSalt.clone();
            return this;
        }

        public Builder hashIterations(int hashIterations) {
            this.hashIterations = hashIterations;
            return this;
        }

        public Builder allowedShareScopes(Set<CeShareScope> scopes) {
            this.allowedShareScopes = scopes == null || scopes.isEmpty()
                ? EnumSet.noneOf(CeShareScope.class)
                : EnumSet.copyOf(scopes);
            return this;
        }

        public Builder encryptionScheme(String encryptionScheme) {
            this.encryptionScheme = encryptionScheme;
            return this;
        }

        public Builder encryptionKeyId(String encryptionKeyId) {
            this.encryptionKeyId = encryptionKeyId;
            return this;
        }

        public Builder encryptionMetadata(Map<String, String> encryptionMetadata) {
            this.encryptionMetadata = encryptionMetadata == null ? Map.of() : Map.copyOf(encryptionMetadata);
            return this;
        }

        public Builder encryptionTags(List<String> encryptionTags) {
            this.encryptionTags = encryptionTags == null ? List.of() : List.copyOf(encryptionTags);
            return this;
        }

        public Builder packetChecksum(String packetChecksum) {
            this.packetChecksum = packetChecksum;
            return this;
        }

        public Builder ledgerEntryId(UUID ledgerEntryId) {
            this.ledgerEntryId = ledgerEntryId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CePacket build() {
            Objects.requireNonNull(packetId, "packetId is required");
            Objects.requireNonNull(clientId, "clientId is required");
            Objects.requireNonNull(consentId, "consentId is required");
            Objects.requireNonNull(consentStatus, "consentStatus is required");
            Objects.requireNonNull(consentEffectiveAt, "consentEffectiveAt is required");
            Objects.requireNonNull(clientHash, "clientHash is required");
            Objects.requireNonNull(hashAlgorithm, "hashAlgorithm is required");
            if (hashSalt == null || hashSalt.length == 0) {
                throw new IllegalArgumentException("hashSalt is required");
            }
            Objects.requireNonNull(encryptionScheme, "encryptionScheme is required");
            Objects.requireNonNull(encryptionKeyId, "encryptionKeyId is required");
            Objects.requireNonNull(packetChecksum, "packetChecksum is required");
            if (allowedShareScopes == null || allowedShareScopes.isEmpty()) {
                throw new IllegalArgumentException("At least one share scope must be defined");
            }
            return new CePacket(this);
        }
    }
}
