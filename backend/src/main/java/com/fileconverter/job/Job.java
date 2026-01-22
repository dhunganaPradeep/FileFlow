package com.fileconverter.job;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Job {

    private final String id;
    private final String originalFileName;
    private final String sourceMimeType;
    private final String targetFormat;
    private final Path inputPath;
    private final Path outputPath;
    private final Instant createdAt;
    private final Instant expiresAt;

    private final AtomicReference<JobStatus> status;
    private final AtomicInteger progress;
    private final AtomicReference<String> errorMessage;
    private final AtomicReference<Instant> completedAt;

    public Job(String id, String originalFileName, String sourceMimeType,
            String targetFormat, Path inputPath, Path outputPath,
            Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.sourceMimeType = sourceMimeType;
        this.targetFormat = targetFormat;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = new AtomicReference<>(JobStatus.QUEUED);
        this.progress = new AtomicInteger(0);
        this.errorMessage = new AtomicReference<>(null);
        this.completedAt = new AtomicReference<>(null);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getSourceMimeType() {
        return sourceMimeType;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public JobStatus getStatus() {
        return status.get();
    }

    public int getProgress() {
        return progress.get();
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public Instant getCompletedAt() {
        return completedAt.get();
    }

    // State transitions
    public void startProcessing() {
        status.set(JobStatus.PROCESSING);
        progress.set(10);
    }

    public void updateProgress(int value) {
        progress.set(Math.min(99, Math.max(0, value)));
    }

    public void complete() {
        status.set(JobStatus.COMPLETED);
        progress.set(100);
        completedAt.set(Instant.now());
    }

    public void fail(String message) {
        status.set(JobStatus.FAILED);
        errorMessage.set(message);
        completedAt.set(Instant.now());
    }

    public void expire() {
        status.set(JobStatus.EXPIRED);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
