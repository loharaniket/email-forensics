package com.emailforensics.backend.health;

import java.util.List;

public record TruncateResultDto(
        String status,
        String message,
        List<String> truncatedTables,
        int evidenceFilesDeleted
) {}

