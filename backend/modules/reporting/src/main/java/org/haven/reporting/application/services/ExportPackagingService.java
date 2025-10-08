package org.haven.reporting.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.reporting.domain.ExportFormat;
import org.haven.reporting.domain.ExportJobId;
import org.haven.reporting.domain.ExportPackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packages export data into secure, tamper-evident archives.
 * Supports:
 * - ZIP archive creation with multiple files
 * - SHA-256 manifest generation
 * - HMAC-SHA256 digital signatures
 * - AES-256-GCM encryption for secure transport
 */
@Service
public class ExportPackagingService {

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;

    public ExportPackagingService(
            ObjectMapper objectMapper,
            @Value("${haven.export.signing-key:0000000000000000000000000000000000000000000000000000000000000000}") String signingKeyHex) {
        this.objectMapper = objectMapper;
        this.signingKey = hexToBytes(signingKeyHex);
    }

    /**
     * Package export files into a secure, signed ZIP archive.
     */
    public ExportPackage packageExport(
            ExportJobId exportJobId,
            Map<String, byte[]> files,
            ExportFormat format,
            boolean encrypt) {

        try {
            // Create ZIP archive
            byte[] zipData = createZipArchive(files);

            // Calculate file hashes
            Map<String, String> fileHashes = new HashMap<>();
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                fileHashes.put(file.getKey(), calculateSHA256(file.getValue()));
            }

            // Generate manifest
            Map<String, Object> manifest = new HashMap<>();
            manifest.put("exportJobId", exportJobId.value());
            manifest.put("generatedAt", LocalDateTime.now().toString());
            manifest.put("files", fileHashes);
            manifest.put("format", format.name());
            manifest.put("encrypted", encrypt);

            String manifestJson = objectMapper.writeValueAsString(manifest);
            String manifestHash = calculateSHA256(manifestJson.getBytes(StandardCharsets.UTF_8));

            // Sign manifest
            String signature = signData(manifestJson.getBytes(StandardCharsets.UTF_8));

            // Optionally encrypt
            byte[] finalZipData = zipData;
            String encryptionAlg = "none";
            if (encrypt) {
                finalZipData = encryptData(zipData);
                encryptionAlg = ENCRYPTION_ALGORITHM;
            }

            // Create metadata
            ExportPackage.PackageMetadata metadata = new ExportPackage.PackageMetadata(
                    "Haven HMIS",
                    LocalDateTime.now().toString(),
                    files.size(),
                    finalZipData.length,
                    encrypt,
                    encryptionAlg
            );

            return new ExportPackage(
                    exportJobId,
                    finalZipData,
                    fileHashes,
                    manifestHash,
                    signature,
                    LocalDateTime.now(),
                    metadata
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to package export", e);
        }
    }

    /**
     * Create a complete package with manifest and signature files included in ZIP.
     */
    public ExportPackage packageWithManifest(
            ExportJobId exportJobId,
            Map<String, byte[]> files,
            ExportFormat format,
            boolean encrypt) {

        // Create base package
        ExportPackage basePackage = packageExport(exportJobId, files, format, false);

        // Add manifest.json to files
        Map<String, Object> manifestContent = new HashMap<>();
        manifestContent.put("exportJobId", exportJobId.value());
        manifestContent.put("generatedAt", basePackage.generatedAt().toString());
        manifestContent.put("files", basePackage.fileHashes());
        manifestContent.put("manifestHash", basePackage.manifestHash());
        manifestContent.put("signature", basePackage.digitalSignature());

        try {
            String manifestJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(manifestContent);

            // Create new ZIP with manifest and signature files
            Map<String, byte[]> allFiles = new HashMap<>(files);
            allFiles.put("manifest.json", manifestJson.getBytes(StandardCharsets.UTF_8));
            allFiles.put("manifest.sha256", basePackage.manifestHash().getBytes(StandardCharsets.UTF_8));
            allFiles.put("signature.txt", basePackage.digitalSignature().getBytes(StandardCharsets.UTF_8));

            // Recreate package with all files
            return packageExport(exportJobId, allFiles, format, encrypt);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create manifest", e);
        }
    }

    /**
     * Create ZIP archive from files.
     */
    private byte[] createZipArchive(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.setLevel(9); // Maximum compression

            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(System.currentTimeMillis());
                zos.putNextEntry(entry);
                zos.write(file.getValue());
                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    /**
     * Calculate SHA-256 hash of data.
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256", e);
        }
    }

    /**
     * Sign data using HMAC-SHA256.
     */
    private String signData(byte[] data) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(signingKey, SIGNATURE_ALGORITHM);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(data);
            return bytesToHex(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }

    /**
     * Verify HMAC signature.
     */
    public boolean verifySignature(byte[] data, String signature) {
        String expectedSignature = signData(data);
        return expectedSignature.equals(signature);
    }

    /**
     * Encrypt data using AES-256-GCM.
     */
    private byte[] encryptData(byte[] plaintext) {
        try {
            // Generate random IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKey key = new SecretKeySpec(signingKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Prepend IV to ciphertext
            byte[] encrypted = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encrypted, GCM_IV_LENGTH, ciphertext.length);

            return encrypted;

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt data using AES-256-GCM.
     */
    public byte[] decryptData(byte[] encrypted) {
        try {
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);

            // Extract ciphertext
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKey key = new SecretKeySpec(signingKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Decrypt
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
