package org.haven.programenrollment.application.security;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.Consent;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CePacket;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crypto utility for building consent-aware CE packets.
 * Generates hashed identifiers and deterministic packet checksums.
 */
@Service
public class CePacketCryptoService {

    private static final int DEFAULT_SALT_BYTES = 16;
    private static final int DEFAULT_SHA_ITERATIONS = 100_000;
    private static final int DEFAULT_BCRYPT_ROUNDS = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final HexFormat hex = HexFormat.of();

    public CePacket createPacket(ClientId clientId,
                                 ProgramEnrollmentId enrollmentId,
                                 Consent consent,
                                 Set<CeShareScope> requestedScopes,
                                 CeHashAlgorithm algorithm,
                                 String encryptionScheme,
                                 String encryptionKeyId,
                                 Map<String, String> encryptionMetadata,
                                 List<String> encryptionTags) {

        CeHashAlgorithm resolvedAlgorithm = algorithm == null ? CeHashAlgorithm.SHA256_SALT : algorithm;
        EnumSet<CeShareScope> scopes = requestedScopes == null || requestedScopes.isEmpty()
            ? EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY)
            : EnumSet.copyOf(requestedScopes);

        byte[] salt;
        String clientHash;
        int iterations;

        if (resolvedAlgorithm == CeHashAlgorithm.BCRYPT) {
            iterations = DEFAULT_BCRYPT_ROUNDS;
            String saltString = BCrypt.gensalt(iterations);
            salt = saltString.getBytes(StandardCharsets.UTF_8);
            clientHash = BCrypt.hashpw(clientId.value().toString(), saltString);
        } else {
            iterations = DEFAULT_SHA_ITERATIONS;
            salt = generateSalt();
            clientHash = hashWithSha256(clientId.value().toString(), salt, iterations);
        }

        Map<String, String> metadata = enrichMetadata(encryptionMetadata, consent, resolvedAlgorithm, encryptionScheme);
        List<String> tags = enrichmentTags(encryptionTags, consent.getId(), consent.getStatus(), scopes);
        String checksum = computePacketChecksum(clientHash, salt, scopes, metadata, tags, consent.getId(), enrollmentId);

        return CePacket.builder()
            .clientId(clientId)
            .enrollmentId(enrollmentId)
            .consentId(consent.getId())
            .consentStatus(consent.getStatus())
            .consentVersion(consent.getVersion())
            .consentEffectiveAt(defaultEffectiveAt(consent))
            .consentExpiresAt(consent.getExpiresAt())
            .clientHash(clientHash)
            .hashAlgorithm(resolvedAlgorithm)
            .hashSalt(salt)
            .hashIterations(iterations)
            .allowedShareScopes(scopes)
            .encryptionScheme(encryptionScheme == null ? "AES-256-GCM" : encryptionScheme)
            .encryptionKeyId(encryptionKeyId)
            .encryptionMetadata(metadata)
            .encryptionTags(tags)
            .packetChecksum(checksum)
            .build();
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private String hashWithSha256(String personalId, byte[] salt, int iterations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = personalId.getBytes(StandardCharsets.UTF_8);
            byte[] hash = new byte[salt.length + input.length];
            System.arraycopy(salt, 0, hash, 0, salt.length);
            System.arraycopy(input, 0, hash, salt.length, input.length);

            for (int i = 0; i < iterations; i++) {
                digest.reset();
                digest.update(salt);
                hash = digest.digest(hash);
            }
            return hex.formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private Map<String, String> enrichMetadata(Map<String, String> metadata,
                                               Consent consent,
                                               CeHashAlgorithm algorithm,
                                               String encryptionScheme) {
        var builder = new java.util.HashMap<String, String>();
        if (metadata != null) {
            builder.putAll(metadata);
        }
        builder.putIfAbsent("consentStatus", consent.getStatus().name());
        builder.putIfAbsent("consentVersion", Long.toString(consent.getVersion()));
        builder.putIfAbsent("hashAlgorithm", algorithm.name());
        builder.putIfAbsent("encryptionScheme", encryptionScheme == null ? "AES-256-GCM" : encryptionScheme);
        if (consent.getExpiresAt() != null) {
            builder.putIfAbsent("consentExpiresAt", consent.getExpiresAt().toString());
        }
        if (consent.isVAWAProtected()) {
            builder.put("vawaProtected", "true");
        }
        return Map.copyOf(builder);
    }

    private List<String> enrichmentTags(List<String> tags,
                                        ConsentId consentId,
                                        org.haven.clientprofile.domain.consent.ConsentStatus status,
                                        Set<CeShareScope> scopes) {
        var tagBuilder = new java.util.LinkedHashSet<String>();
        if (tags != null) {
            tagBuilder.addAll(tags);
        }
        tagBuilder.add("consent:" + consentId.value());
        tagBuilder.add("status:" + status.name());
        scopes.forEach(scope -> tagBuilder.add("scope:" + scope.name()));
        return List.copyOf(tagBuilder);
    }

    private String computePacketChecksum(String clientHash,
                                         byte[] salt,
                                         Set<CeShareScope> scopes,
                                         Map<String, String> metadata,
                                         List<String> tags,
                                         ConsentId consentId,
                                         ProgramEnrollmentId enrollmentId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(clientHash.getBytes(StandardCharsets.UTF_8));
            digest.update(salt);
            digest.update(consentId.value().toString().getBytes(StandardCharsets.UTF_8));
            if (enrollmentId != null) {
                digest.update(enrollmentId.value().toString().getBytes(StandardCharsets.UTF_8));
            }
            for (CeShareScope scope : scopes) {
                digest.update(scope.name().getBytes(StandardCharsets.UTF_8));
            }
            metadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                    digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                });
            for (String tag : tags) {
                digest.update(tag.getBytes(StandardCharsets.UTF_8));
            }
            return hex.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private Instant defaultEffectiveAt(Consent consent) {
        Instant granted = consent.getGrantedAt();
        return granted != null ? granted : Instant.now();
    }

    // Additional methods for backward compatibility
    public byte[] getEncryptionKey(String keyId) {
        // Generate a deterministic key based on keyId for now
        // In production, this should retrieve from a secure key store
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(keyId.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public String hashClientId(String clientId, CeHashAlgorithm algorithm) {
        if (algorithm == CeHashAlgorithm.BCRYPT) {
            String salt = BCrypt.gensalt(DEFAULT_BCRYPT_ROUNDS);
            return BCrypt.hashpw(clientId, salt);
        } else {
            byte[] salt = generateSalt();
            return hashWithSha256(clientId, salt, DEFAULT_SHA_ITERATIONS);
        }
    }

    public String calculateChecksum(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return hex.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
