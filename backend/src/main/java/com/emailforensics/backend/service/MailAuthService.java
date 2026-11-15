package com.emailforensics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class MailAuthService {

    private static final Logger log = LoggerFactory.getLogger(MailAuthService.class);
    private static final String DEFAULT_FALLBACK = "{\"spf\":{\"status\":\"NOT_VERIFIABLE\"},\"dkim\":{\"status\":\"NOT_VERIFIABLE\"},\"dmarc\":{\"status\":\"NOT_VERIFIABLE\"},\"arc\":{\"status\":\"NOT_VERIFIABLE\"},\"receivedChain\":[]}";

    private final String helperScriptPath;

    public MailAuthService(@Value("${emailforensics.mailauth.script:tools/mailauth/verify.mjs}") String helperScriptPath) {
        this.helperScriptPath = helperScriptPath;
    }

    public String authenticateEmail(Path emailFilePath) {
        if (emailFilePath == null) {
            return DEFAULT_FALLBACK;
        }

        File scriptFile = Paths.get(helperScriptPath).toFile();
        if (!scriptFile.exists()) {
            scriptFile = Paths.get("..", helperScriptPath).toFile();
        }
        if (!scriptFile.exists()) {
            log.warn("mailauth script not found at {}", scriptFile.getAbsolutePath());
            return DEFAULT_FALLBACK;
        }

        try {
            return executeHelper(scriptFile, emailFilePath.toFile());
        } catch (Exception e) {
            log.warn("Failed to execute mailauth helper: {}", e.getMessage());
            return DEFAULT_FALLBACK;
        }
    }

    private String executeHelper(File scriptFile, File emailFile) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "node",
                scriptFile.getAbsolutePath(),
                emailFile.getAbsolutePath()
        );
        processBuilder.directory(scriptFile.getParentFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("mailauth verification timed out for file: {}", emailFile.getName());
            return DEFAULT_FALLBACK;
        }

        String result = output.toString().trim();
        if (result.startsWith("{") && result.endsWith("}")) {
            return result;
        }

        return DEFAULT_FALLBACK;
    }
}
