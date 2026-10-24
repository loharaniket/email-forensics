package com.emailforensics.backend.dto.casefile;

import com.emailforensics.backend.casefile.CaseStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateCaseNotesRequest {

    private String analystNotes;

    @NotNull(message = "Case status is required")
    private CaseStatus status;

    public UpdateCaseNotesRequest() {
    }

    public UpdateCaseNotesRequest(String analystNotes, CaseStatus status) {
        this.analystNotes = analystNotes;
        this.status = status;
    }

    public String getAnalystNotes() {
        return analystNotes;
    }

    public void setAnalystNotes(String analystNotes) {
        this.analystNotes = analystNotes;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }
}
