package com.emailforensics.backend.dto.aiassessment;

import java.util.ArrayList;
import java.util.List;

public class AiStructuredOutput {

    private String threatClassification;
    private String confidenceAssessment;
    private String summaryExplanation;
    private List<String> keyIndicators = new ArrayList<>();
    private List<String> evidenceReferences = new ArrayList<>();
    private String limitations;

    public AiStructuredOutput() {
    }

    public AiStructuredOutput(String threatClassification, String confidenceAssessment,
                              String summaryExplanation, List<String> keyIndicators,
                              List<String> evidenceReferences, String limitations) {
        this.threatClassification = threatClassification;
        this.confidenceAssessment = confidenceAssessment;
        this.summaryExplanation = summaryExplanation;
        this.keyIndicators = keyIndicators != null ? keyIndicators : new ArrayList<>();
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.limitations = limitations;
    }

    public String getThreatClassification() {
        return threatClassification;
    }

    public void setThreatClassification(String threatClassification) {
        this.threatClassification = threatClassification;
    }

    public String getConfidenceAssessment() {
        return confidenceAssessment;
    }

    public void setConfidenceAssessment(String confidenceAssessment) {
        this.confidenceAssessment = confidenceAssessment;
    }

    public String getSummaryExplanation() {
        return summaryExplanation;
    }

    public void setSummaryExplanation(String summaryExplanation) {
        this.summaryExplanation = summaryExplanation;
    }

    public List<String> getKeyIndicators() {
        return keyIndicators;
    }

    public void setKeyIndicators(List<String> keyIndicators) {
        this.keyIndicators = keyIndicators;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }
}
