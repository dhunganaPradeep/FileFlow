package com.fileconverter.storage;

import com.fileconverter.config.AppConfig;
import com.fileconverter.job.JobQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Component
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final TempStorageService storageService;
    private final JobQueue jobQueue;
    private final int ttlMinutes;

    public CleanupScheduler(TempStorageService storageService, JobQueue jobQueue, AppConfig config) {
        this.storageService = storageService;
        this.jobQueue = jobQueue;
        this.ttlMinutes = config.getStorage().getTtlMinutes();
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredJobs() {
        log.debug("Running cleanup task");

        // Clean expired jobs from queue
        jobQueue.cleanupExpired();

        // Clean old files from storage
        cleanupOldFiles();
    }

    private void cleanupOldFiles() {
        Path tempDir = storageService.getTempDir();
        Instant cutoff = Instant.now().minus(ttlMinutes, ChronoUnit.MINUTES);

        try (Stream<Path> dirs = Files.list(tempDir)) {
            dirs.filter(Files::isDirectory)
                    .forEach(jobDir -> {
                        try {
                            Instant modified = Files.getLastModifiedTime(jobDir).toInstant();
                            if (modified.isBefore(cutoff)) {
                                storageService.deleteJob(jobDir.getFileName().toString())
                                        .subscribe();
                                log.info("Cleaned up expired directory: {}", jobDir.getFileName());
                            }
                        } catch (IOException e) {
                            log.warn("Error checking directory: {}", jobDir, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error listing temp directory", e);
        }
    }
}
