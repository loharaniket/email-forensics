package com.emailforensics.backend.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DotenvLoader {

    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        String[] searchPaths = new String[]{
                ".env",
                "../.env",
                "backend/.env"
        };

        File envFile = null;
        for (String path : searchPaths) {
            File f = new File(path);
            if (f.exists() && f.isFile() && f.canRead()) {
                envFile = f;
                break;
            }
        }

        if (envFile == null) {
            return;
        }

        Map<String, String> envMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        if (val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                    }
                    envMap.put(key, val);
                }
            }
        } catch (Exception ignored) {
        }

        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, val);
            }
        }

        String dbPass = envMap.get("DB_PASSWORD");
        if (dbPass != null && System.getProperty("spring.datasource.password") == null) {
            System.setProperty("spring.datasource.password", dbPass);
        }

        String geminiKey = envMap.get("GEMINI_API_KEY");
        if (geminiKey != null && !geminiKey.isBlank()) {
            if (System.getProperty("spring.ai.gemini.api-key") == null) {
                System.setProperty("spring.ai.gemini.api-key", geminiKey);
            }
            if (System.getProperty("GEMINI_API_KEY") == null) {
                System.setProperty("GEMINI_API_KEY", geminiKey);
            }
        }

        String geminiBaseUrl = envMap.get("GEMINI_BASE_URL");
        if (geminiBaseUrl != null && !geminiBaseUrl.isBlank()) {
            if (System.getProperty("spring.ai.gemini.base-url") == null) {
                System.setProperty("spring.ai.gemini.base-url", geminiBaseUrl);
            }
        }

        String geminiModel = envMap.get("GEMINI_MODEL");
        if (geminiModel != null && !geminiModel.isBlank()) {
            if (System.getProperty("spring.ai.gemini.chat.options.model") == null) {
                System.setProperty("spring.ai.gemini.chat.options.model", geminiModel);
            }
        }

        String groqKey = envMap.get("GROQ_API_KEY");
        if (groqKey != null && !groqKey.isBlank()) {
            if (System.getProperty("spring.ai.openai.api-key") == null) {
                System.setProperty("spring.ai.openai.api-key", groqKey);
            }
            if (System.getProperty("SPRING_AI_API_KEY") == null) {
                System.setProperty("SPRING_AI_API_KEY", groqKey);
            }
            if (System.getProperty("GROQ_API_KEY") == null) {
                System.setProperty("GROQ_API_KEY", groqKey);
            }
        }
    }
}
