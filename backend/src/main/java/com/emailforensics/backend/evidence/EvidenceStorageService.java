package com.emailforensics.backend.evidence;

import com.emailforensics.backend.common.BadRequestException;
import com.emailforensics.backend.common.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EvidenceStorageService {

    private final Path vaultPath;
    private final Path tempPath;

    public EvidenceStorageService(@Value("${emailforensics.evidence.vault-dir:./evidence_vault}") String vaultDir) {
        this.vaultPath = Paths.get(vaultDir).toAbsolutePath().normalize();
        this.tempPath = this.vaultPath.resolve("tmp");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(vaultPath);
            Files.createDirectories(tempPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize evidence vault directory at " + vaultPath, e);
        }
    }

    public StoredEvidenceFile storeEvidence(InputStream inputStream) {
        if (inputStream == null) {
            throw new BadRequestException("Uploaded evidence stream cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path temporaryFile = Files.createTempFile(tempPath, "upload-", ".tmp");

            long totalBytes = 0;
            try (DigestInputStream dis = new DigestInputStream(inputStream, digest);
                 OutputStream out = Files.newOutputStream(temporaryFile, StandardOpenOption.WRITE)) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = dis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalBytes += read;
                }
            }

            if (totalBytes == 0) {
                Files.deleteIfExists(temporaryFile);
                throw new BadRequestException("Uploaded evidence file is empty");
            }

            String sha256Hex = HexFormat.of().formatHex(digest.digest());
            String storageKey = UUID.randomUUID().toString() + ".eml";
            Path targetFile = resolveStorageKey(storageKey);

            try {
                Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredEvidenceFile(storageKey, sha256Hex, totalBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to preserve evidence file", e);
        }
    }

    public String computeFileHash(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResourceNotFoundException("Preserved evidence file not found for storage key");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(in, digest)) {

                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read preserved evidence file", e);
        }
    }

    public Resource loadAsResource(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResourceNotFoundException("Preserved evidence file not found");
        }
        return new FileSystemResource(filePath);
    }

    public boolean deleteStoredFile(String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            return false;
        }
        try {
            Path filePath = resolveStorageKey(storageKey);
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    public String sanitizeFilename(String untrustedFilename) {
        if (untrustedFilename == null || untrustedFilename.trim().isEmpty()) {
            return "evidence.eml";
        }

        String cleaned = untrustedFilename.replace('\\', '/');
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }

        cleaned = cleaned.replaceAll("[\\r\\n\\t\\u0000]", "").trim();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) {
            return "evidence.eml";
        }

        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(cleaned.length() - 255);
        }

        return cleaned;
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.contains("..") || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new BadRequestException("Invalid storage key");
        }
        Path resolved = vaultPath.resolve(storageKey).normalize();
        if (!resolved.startsWith(vaultPath)) {
            throw new BadRequestException("Storage path traversal attempted");
        }
        return resolved;
    }

    public Path getVaultPath() {
        return vaultPath;
    }
}
