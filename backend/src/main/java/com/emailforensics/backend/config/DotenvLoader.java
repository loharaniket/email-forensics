package com.emailforensics.backend.config;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class DotenvLoader {

    private static boolean loaded = false;

    static {
        load();
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        String[] searchPaths = new String[]{
                ".env",
                "../.env",
                "backend/.env",
                System.getProperty("user.dir") + File.separator + ".env",
                System.getProperty("user.dir") + File.separator + "backend" + File.separator + ".env"
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

        String groqKey = envMap.get("GROQ_API_KEY");
        if (groqKey != null && !groqKey.isBlank()) {
            if (System.getProperty("spring.ai.openai.api-key") == null) {
                System.setProperty("spring.ai.openai.api-key", groqKey);
            }
            if (System.getProperty("SPRING_AI_API_KEY") == null) {
                System.setProperty("SPRING_AI_API_KEY", groqKey);
            }
        }

        String groqBaseUrl = envMap.get("GROQ_BASE_URL");
        if (groqBaseUrl != null && !groqBaseUrl.isBlank()) {
            if (System.getProperty("spring.ai.openai.base-url") == null) {
                System.setProperty("spring.ai.openai.base-url", groqBaseUrl);
            }
            if (System.getProperty("SPRING_AI_BASE_URL") == null) {
                System.setProperty("SPRING_AI_BASE_URL", groqBaseUrl);
            }
        }

        String groqModel = envMap.get("GROQ_MODEL");
        if (groqModel != null && !groqModel.isBlank()) {
            if (System.getProperty("spring.ai.openai.chat.options.model") == null) {
                System.setProperty("spring.ai.openai.chat.options.model", groqModel);
            }
            if (System.getProperty("SPRING_AI_MODEL") == null) {
                System.setProperty("SPRING_AI_MODEL", groqModel);
            }
        }

        String provider = envMap.get("SPRING_AI_PROVIDER");
        if (provider != null && !provider.isBlank()) {
            if (System.getProperty("spring.ai.provider") == null) {
                System.setProperty("spring.ai.provider", provider);
            }
        }
    }
}
