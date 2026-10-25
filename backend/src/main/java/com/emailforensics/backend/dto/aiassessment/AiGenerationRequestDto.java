package com.emailforensics.backend.dto.aiassessment;

public class AiGenerationRequestDto {

    private boolean forceRegenerate;
    private String modelOverride;

    public AiGenerationRequestDto() {
    }

    public AiGenerationRequestDto(boolean forceRegenerate, String modelOverride) {
        this.forceRegenerate = forceRegenerate;
        this.modelOverride = modelOverride;
    }

    public boolean isForceRegenerate() {
        return forceRegenerate;
    }

    public void setForceRegenerate(boolean forceRegenerate) {
        this.forceRegenerate = forceRegenerate;
    }

    public String getModelOverride() {
        return modelOverride;
    }

    public void setModelOverride(String modelOverride) {
        this.modelOverride = modelOverride;
    }
}
