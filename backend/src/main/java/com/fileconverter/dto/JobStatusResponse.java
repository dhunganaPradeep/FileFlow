package com.fileconverter.dto;

import java.time.Instant;

public record JobStatusResponse(
        String jobId,
        String status,
        String fileName,
        String sourceFormat,
        String targetFormat,
        int progress,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        String downloadUrl) {
}
