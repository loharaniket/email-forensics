package com.emailforensics.backend.visual.fixtures;

public interface OcrFixtureProvider {
    boolean hasMock(String sha256Hash);
    String getMockOcrText(String sha256Hash);
    void registerMockOcrText(String sha256Hash, String text);
}
