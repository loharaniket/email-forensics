package com.emailforensics.backend.visual.fixtures;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultOcrFixtureProvider implements OcrFixtureProvider {

    private final Map<String, String> mockOcrStore = new ConcurrentHashMap<>();

    @Override
    public boolean hasMock(String sha256Hash) {
        if (sha256Hash == null) {
            return false;
        }
        return mockOcrStore.containsKey(sha256Hash.toLowerCase());
    }

    @Override
    public String getMockOcrText(String sha256Hash) {
        if (sha256Hash == null) {
            return null;
        }
        return mockOcrStore.get(sha256Hash.toLowerCase());
    }

    @Override
    public void registerMockOcrText(String sha256Hash, String text) {
        if (sha256Hash != null && text != null) {
            mockOcrStore.put(sha256Hash.toLowerCase(), text);
        }
    }
}
