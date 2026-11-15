package com.emailforensics.backend.service;

import com.emailforensics.backend.common.BadRequestException;
import com.emailforensics.backend.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailStorageService {

    private final Path rootPath;

    public EmailStorageService(@Value("${emailforensics.storage.root-dir:storage/emails}") String rootDir) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + this.rootPath, e);
        }
    }

    public String saveOriginalEmail(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("File bytes cannot be empty");
        }

        String emailFolderId = UUID.randomUUID().toString();
        Path folderPath = this.rootPath.resolve(emailFolderId).normalize();

        if (!folderPath.startsWith(this.rootPath)) {
            throw new BadRequestException("Invalid storage path generated");
        }

        try {
            Files.createDirectories(folderPath);
            Path targetFile = folderPath.resolve("original.eml").normalize();
            Files.write(targetFile, fileBytes, StandardOpenOption.CREATE_NEW);
            return this.rootPath.relativize(targetFile).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write original email file to disk", e);
        }
    }

    public Resource loadOriginalEmail(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new BadRequestException("File path cannot be empty");
        }

        Path resolvedPath = this.rootPath.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(this.rootPath)) {
            throw new BadRequestException("Storage path traversal attempted");
        }

        if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            throw new ResourceNotFoundException("Original email file not found: " + relativePath);
        }

        return new FileSystemResource(resolvedPath);
    }

    public String computeSha256(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("Cannot compute SHA-256 of empty bytes");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "evidence.eml";
        }

        String cleaned = originalFilename.replace('\\', '/');
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

    public Path getRootPath() {
        return rootPath;
    }
}
