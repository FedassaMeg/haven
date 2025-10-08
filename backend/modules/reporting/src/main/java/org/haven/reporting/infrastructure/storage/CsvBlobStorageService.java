package org.haven.reporting.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Blob storage service for HUD export CSV artifacts
 *
 * Provides:
 * - File-based storage for CSV exports (can be extended to S3/Azure Blob)
 * - ZIP compression for multi-file exports
 * - SHA-256 hash generation for integrity verification
 * - Retention policy enforcement (90-day default per HUD guidance)
 * - Automatic cleanup of expired exports
 *
 * Storage structure:
 * {baseDir}/exports/{year}/{month}/{exportJobId}/
 *   - Client.csv
 *   - Enrollment.csv
 *   - Exit.csv
 *   - Services.csv
 *   - Export.csv (metadata)
 *   - export-{exportJobId}.zip (combined archive)
 */
@Service
public class CsvBlobStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CsvBlobStorageService.class);

    private final Path baseStoragePath;
    private final int retentionDays;

    public CsvBlobStorageService(
            @Value("${haven.reporting.storage.base-path:./data/exports}") String baseStoragePath,
            @Value("${haven.reporting.storage.retention-days:90}") int retentionDays) {
        this.baseStoragePath = Paths.get(baseStoragePath);
        this.retentionDays = retentionDays;

        try {
            Files.createDirectories(this.baseStoragePath);
            logger.info("CSV blob storage initialized at: {} (retention: {} days)",
                    this.baseStoragePath.toAbsolutePath(), retentionDays);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize blob storage", e);
        }
    }

    /**
     * Store CSV files for an export job
     * Returns blob storage URL and SHA-256 hash
     */
    public StorageResult storeCsvFiles(
            UUID exportJobId,
            Map<String, String> csvFileContents) throws IOException, NoSuchAlgorithmException {

        Path exportDir = createExportDirectory(exportJobId);

        List<String> storedFiles = new ArrayList<>();
        StringBuilder combinedContent = new StringBuilder();

        // Write individual CSV files
        for (Map.Entry<String, String> entry : csvFileContents.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();

            Path filePath = exportDir.resolve(fileName);
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            storedFiles.add(fileName);
            combinedContent.append(fileName).append(":").append(content);

            logger.debug("Stored CSV file: {}", filePath);
        }

        // Create ZIP archive
        String zipFileName = "export-" + exportJobId + ".zip";
        Path zipPath = exportDir.resolve(zipFileName);
        createZipArchive(exportDir, csvFileContents.keySet(), zipPath);

        // Calculate SHA-256 hash of combined content for integrity verification
        String sha256Hash = calculateSha256(combinedContent.toString());

        // Calculate expiration date
        Instant expiresAt = Instant.now().plus(retentionDays, ChronoUnit.DAYS);

        String storageUrl = exportDir.toAbsolutePath().toString();

        logger.info("Stored export {} with {} files. Hash: {}, Expires: {}",
                exportJobId, storedFiles.size(), sha256Hash, expiresAt);

        return new StorageResult(storageUrl, zipPath.toString(), sha256Hash, storedFiles, expiresAt);
    }

    /**
     * Create ZIP archive of CSV files
     */
    private void createZipArchive(Path exportDir, Set<String> fileNames, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (String fileName : fileNames) {
                Path csvFile = exportDir.resolve(fileName);
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                Files.copy(csvFile, zos);
                zos.closeEntry();
            }
        }
        logger.debug("Created ZIP archive: {}", zipPath);
    }

    /**
     * Create directory structure for export
     * Pattern: {baseDir}/exports/{year}/{month}/{exportJobId}/
     */
    private Path createExportDirectory(UUID exportJobId) throws IOException {
        Instant now = Instant.now();
        int year = now.atZone(java.time.ZoneId.systemDefault()).getYear();
        int month = now.atZone(java.time.ZoneId.systemDefault()).getMonthValue();

        Path exportDir = baseStoragePath
                .resolve(String.valueOf(year))
                .resolve(String.format("%02d", month))
                .resolve(exportJobId.toString());

        Files.createDirectories(exportDir);
        return exportDir;
    }

    /**
     * Calculate SHA-256 hash of content
     */
    private String calculateSha256(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Cleanup expired exports based on retention policy
     * Returns count of deleted export directories
     */
    public int cleanupExpiredExports() throws IOException {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deletedCount = 0;

        // Walk directory tree looking for export directories
        if (!Files.exists(baseStoragePath)) {
            return 0;
        }

        try (var yearDirs = Files.list(baseStoragePath)) {
            for (Path yearDir : yearDirs.toList()) {
                if (!Files.isDirectory(yearDir)) continue;

                try (var monthDirs = Files.list(yearDir)) {
                    for (Path monthDir : monthDirs.toList()) {
                        if (!Files.isDirectory(monthDir)) continue;

                        try (var exportDirs = Files.list(monthDir)) {
                            for (Path exportDir : exportDirs.toList()) {
                                if (!Files.isDirectory(exportDir)) continue;

                                Instant lastModified = Files.getLastModifiedTime(exportDir).toInstant();
                                if (lastModified.isBefore(cutoffDate)) {
                                    deleteDirectory(exportDir);
                                    deletedCount++;
                                    logger.info("Deleted expired export: {}", exportDir);
                                }
                            }
                        }
                    }
                }
            }
        }

        logger.info("Cleanup completed. Deleted {} expired exports older than {} days",
                deletedCount, retentionDays);
        return deletedCount;
    }

    /**
     * Recursively delete directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.error("Failed to delete: {}", path, e);
                        }
                    });
        }
    }

    /**
     * Storage result DTO
     */
    public static class StorageResult {
        private final String storageUrl;
        private final String zipFilePath;
        private final String sha256Hash;
        private final List<String> storedFiles;
        private final Instant expiresAt;

        public StorageResult(String storageUrl, String zipFilePath, String sha256Hash,
                           List<String> storedFiles, Instant expiresAt) {
            this.storageUrl = storageUrl;
            this.zipFilePath = zipFilePath;
            this.sha256Hash = sha256Hash;
            this.storedFiles = storedFiles;
            this.expiresAt = expiresAt;
        }

        public String getStorageUrl() {
            return storageUrl;
        }

        public String getZipFilePath() {
            return zipFilePath;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }

        public List<String> getStoredFiles() {
            return storedFiles;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
