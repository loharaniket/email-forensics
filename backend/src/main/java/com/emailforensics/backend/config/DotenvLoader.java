package com.emailforensics.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvLoader implements EnvironmentPostProcessor {

    private static boolean loaded = false;
    private static final Map<String, Object> loadedMap = new HashMap<>();

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
                System.getProperty("user.dir") + File.separator + ".." + File.separator + ".env",
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
            loadedMap.put(key, val);
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, val);
            }
        }

        String groqKey = envMap.get("GROQ_API_KEY");
        if (groqKey != null && !groqKey.isBlank()) {
            loadedMap.put("spring.ai.openai.api-key", groqKey);
            loadedMap.put("SPRING_AI_API_KEY", groqKey);
            if (System.getProperty("spring.ai.openai.api-key") == null) {
                System.setProperty("spring.ai.openai.api-key", groqKey);
            }
            if (System.getProperty("SPRING_AI_API_KEY") == null) {
                System.setProperty("SPRING_AI_API_KEY", groqKey);
            }
        }

        String groqBaseUrl = envMap.get("GROQ_BASE_URL");
        if (groqBaseUrl != null && !groqBaseUrl.isBlank()) {
            loadedMap.put("spring.ai.openai.base-url", groqBaseUrl);
            loadedMap.put("SPRING_AI_BASE_URL", groqBaseUrl);
            if (System.getProperty("spring.ai.openai.base-url") == null) {
                System.setProperty("spring.ai.openai.base-url", groqBaseUrl);
            }
            if (System.getProperty("SPRING_AI_BASE_URL") == null) {
                System.setProperty("SPRING_AI_BASE_URL", groqBaseUrl);
            }
        }

        String groqModel = envMap.get("GROQ_MODEL");
        if (groqModel != null && !groqModel.isBlank()) {
            loadedMap.put("spring.ai.openai.chat.options.model", groqModel);
            loadedMap.put("SPRING_AI_MODEL", groqModel);
            if (System.getProperty("spring.ai.openai.chat.options.model") == null) {
                System.setProperty("spring.ai.openai.chat.options.model", groqModel);
            }
            if (System.getProperty("SPRING_AI_MODEL") == null) {
                System.setProperty("SPRING_AI_MODEL", groqModel);
            }
        }

        String provider = envMap.get("SPRING_AI_PROVIDER");
        if (provider != null && !provider.isBlank()) {
            loadedMap.put("spring.ai.provider", provider);
            if (System.getProperty("spring.ai.provider") == null) {
                System.setProperty("spring.ai.provider", provider);
            }
        }

        String dbHost = envMap.getOrDefault("DB_HOST", "localhost");
        String dbPort = envMap.getOrDefault("DB_PORT", "5432");
        String dbName = envMap.getOrDefault("DB_NAME", "email_forensics");
        String dbUser = envMap.getOrDefault("DB_USER", "postgres");
        String dbPass = envMap.getOrDefault("DB_PASSWORD", "");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        loadedMap.put("spring.datasource.url", dbUrl);
        loadedMap.put("spring.datasource.username", dbUser);
        loadedMap.put("spring.datasource.password", dbPass);
        loadedMap.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        if (System.getProperty("spring.datasource.url") == null) {
            System.setProperty("spring.datasource.url", dbUrl);
        }
        if (System.getProperty("spring.datasource.username") == null) {
            System.setProperty("spring.datasource.username", dbUser);
        }
        if (System.getProperty("spring.datasource.password") == null) {
            System.setProperty("spring.datasource.password", dbPass);
        }
        if (System.getProperty("spring.datasource.driver-class-name") == null) {
            System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        }
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        load();
        if (!loadedMap.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", loadedMap));
        }
    }
}
