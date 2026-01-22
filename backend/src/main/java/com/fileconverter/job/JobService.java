package com.fileconverter.job;

import com.fileconverter.config.AppConfig;
import com.fileconverter.dto.JobStatusResponse;
import com.fileconverter.storage.TempStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobQueue jobQueue;
    private final TempStorageService storageService;
    private final int ttlMinutes;

    public JobService(JobQueue jobQueue, TempStorageService storageService, AppConfig config) {
        this.jobQueue = jobQueue;
        this.storageService = storageService;
        this.ttlMinutes = config.getStorage().getTtlMinutes();
    }

    public Mono<Job> createJob(String originalFileName, String sourceMimeType,
            String targetFormat, Path inputPath) {
        return Mono.fromCallable(() -> {
            String jobId = UUID.randomUUID().toString();
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(ttlMinutes * 60L);

            String outputFileName = generateOutputFileName(originalFileName, targetFormat);
            Path outputPath = storageService.createOutputPath(jobId, outputFileName);

            Job job = new Job(
                    jobId, originalFileName, sourceMimeType,
                    targetFormat, inputPath, outputPath,
                    now, expiresAt);

            if (!jobQueue.submit(job)) {
                throw new IllegalStateException("Job queue is full. Please try again later.");
            }

            return job;
        });
    }

    public Mono<JobStatusResponse> getJobStatus(String jobId) {
        return Mono.fromCallable(() -> {
            Job job = jobQueue.getJob(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            String downloadUrl = null;
            if (job.getStatus() == JobStatus.COMPLETED) {
                downloadUrl = "/api/jobs/" + jobId + "/download";
            }

            return new JobStatusResponse(
                    job.getId(),
                    job.getStatus().name(),
                    job.getOriginalFileName(),
                    job.getSourceMimeType(),
                    job.getTargetFormat(),
                    job.getProgress(),
                    job.getErrorMessage(),
                    job.getCreatedAt(),
                    job.getCompletedAt(),
                    downloadUrl);
        });
    }

    public Mono<Path> getOutputPath(String jobId) {
        return Mono.fromCallable(() -> {
            Job job = jobQueue.getJob(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            if (job.getStatus() != JobStatus.COMPLETED) {
                throw new IllegalStateException("Job not completed yet");
            }

            return job.getOutputPath();
        });
    }

    public Mono<String> getOutputFileName(String jobId) {
        return Mono.fromCallable(() -> {
            Job job = jobQueue.getJob(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            return generateOutputFileName(job.getOriginalFileName(), job.getTargetFormat());
        });
    }

    private String generateOutputFileName(String originalName, String targetFormat) {
        int dotIndex = originalName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
        return baseName + "." + targetFormat;
    }
}
