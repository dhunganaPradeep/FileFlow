package com.fileconverter.dto;

import java.time.Instant;

public record UploadResponse(
        String jobId,
        String token,
        String fileName,
        String mimeType,
        long fileSize,
        String targetFormat,
        Instant createdAt,
        Instant expiresAt) {
}
