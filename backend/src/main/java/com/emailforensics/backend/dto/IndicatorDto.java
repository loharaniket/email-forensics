package com.emailforensics.backend.dto;

import com.emailforensics.backend.entity.Indicator;
import java.time.Instant;

public class IndicatorDto {

    private Long id;
    private Long emailId;
    private String type;
    private String value;
    private String normalizedValue;
    private String origin;
    private String verdict;
    private String details;
    private Instant checkedAt;

    public IndicatorDto() {
    }

    public IndicatorDto(Long id, Long emailId, String type, String value, String normalizedValue,
                        String origin, String verdict, String details, Instant checkedAt) {
        this.id = id;
        this.emailId = emailId;
        this.type = type;
        this.value = value;
        this.normalizedValue = normalizedValue;
        this.origin = origin;
        this.verdict = verdict;
        this.details = details;
        this.checkedAt = checkedAt;
    }

    public static IndicatorDto fromEntity(Indicator indicator) {
        if (indicator == null) {
            return null;
        }
        return new IndicatorDto(
                indicator.getId(),
                indicator.getEmailId(),
                indicator.getType(),
                indicator.getValue(),
                indicator.getNormalizedValue(),
                indicator.getOrigin(),
                indicator.getVerdict(),
                indicator.getDetails(),
                indicator.getCheckedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmailId() {
        return emailId;
    }

    public void setEmailId(Long emailId) {
        this.emailId = emailId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
