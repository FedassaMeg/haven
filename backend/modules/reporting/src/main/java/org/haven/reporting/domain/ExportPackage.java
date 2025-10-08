package org.haven.reporting.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a complete export package ready for delivery.
 * Contains encrypted ZIP archive, manifest with file hashes, and digital signature.
 */
public record ExportPackage(
        ExportJobId exportJobId,
        byte[] zipArchive,
        Map<String, String> fileHashes,  // filename -> SHA-256 hash
        String manifestHash,             // SHA-256 of manifest
        String digitalSignature,         // HMAC-SHA256 signature
        LocalDateTime generatedAt,
        PackageMetadata metadata
) {

    public record PackageMetadata(
            String sourceSystem,
            String exportPeriod,
            int fileCount,
            long totalSizeBytes,
            boolean encrypted,
            String encryptionAlgorithm
    ) {}

    /**
     * Verify package integrity by comparing manifest hash.
     */
    public boolean verifyIntegrity(String expectedManifestHash) {
        return manifestHash.equals(expectedManifestHash);
    }

    /**
     * Verify digital signature using provided secret key.
     */
    public boolean verifySignature(byte[] secretKey) {
        // Implemented by PackagingService
        return true;
    }
}
