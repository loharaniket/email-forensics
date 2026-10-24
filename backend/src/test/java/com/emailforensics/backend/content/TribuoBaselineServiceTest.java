package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.MlEvaluationMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribuoBaselineServiceTest {

    private TribuoBaselineService service;

    @BeforeEach
    void setUp() {
        service = new TribuoBaselineService();
    }

    @Test
    void shouldInitializeAndEvaluateModelSuccessfully() {
        assertEquals(TribuoBaselineService.MODEL_VERSION, service.getModelVersion());

        MlEvaluationMetricsDto metrics = service.getModelMetrics();
        assertNotNull(metrics);
        assertEquals(20, metrics.getTrainingSampleCount());
        assertEquals(8, metrics.getTestSampleCount());
        assertTrue(metrics.getAccuracy() >= 0.75);
        assertTrue(metrics.getPrecision() >= 0.75);
        assertTrue(metrics.getRecall() >= 0.75);
        assertTrue(metrics.getF1() >= 0.75);
        assertTrue(metrics.getTruePositives() + metrics.getTrueNegatives() >= 6);
    }

    @Test
    void shouldPredictPhishingForUrgentCredentialHarvesting() {
        String phish = "Urgent: your corporate credentials have expired. Verify your account password and restore access.";
        TribuoBaselineService.PredictionResult result = service.predict(phish);

        assertNotNull(result);
        assertEquals("PHISHING", result.getPredictedLabel());
        assertEquals("EVALUATED", result.getMlStatus());
        assertNotNull(result.getProbability());
        assertTrue(result.getProbability() > 0.5);
    }

    @Test
    void shouldPredictBenignForStandardInternalAnnouncement() {
        String benign = "Engineering sprint planning notes and weekly holiday schedule updates for the development team.";
        TribuoBaselineService.PredictionResult result = service.predict(benign);

        assertNotNull(result);
        assertEquals("BENIGN", result.getPredictedLabel());
        assertEquals("EVALUATED", result.getMlStatus());
        assertNotNull(result.getProbability());
    }

    @Test
    void shouldHandleEmptyAndBlankTextGracefully() {
        TribuoBaselineService.PredictionResult nullResult = service.predict(null);
        assertNotNull(nullResult);
        assertEquals("SKIPPED_EMPTY_TEXT", nullResult.getMlStatus());

        TribuoBaselineService.PredictionResult blankResult = service.predict("   ");
        assertNotNull(blankResult);
        assertEquals("SKIPPED_EMPTY_TEXT", blankResult.getMlStatus());
    }
}
