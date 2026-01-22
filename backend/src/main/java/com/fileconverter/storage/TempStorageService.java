package com.fileconverter.storage;

import com.fileconverter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class TempStorageService {

    private static final Logger log = LoggerFactory.getLogger(TempStorageService.class);

    private final Path tempDir;

    public TempStorageService(AppConfig config) {
        this.tempDir = Paths.get(config.getStorage().getTempDir());
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(tempDir);
        log.info("Temp storage initialized at: {}", tempDir);
    }

    public Mono<Path> saveUpload(FilePart filePart, String jobId) {
        return Mono.fromCallable(() -> {
            Path jobDir = tempDir.resolve(jobId);
            Files.createDirectories(jobDir);
            return jobDir.resolve(sanitizeFileName(filePart.filename()));
        }).flatMap(destPath -> filePart.transferTo(destPath).thenReturn(destPath));
    }

    public Mono<Path> saveStream(Flux<DataBuffer> dataStream, String jobId, String fileName) {
        return Mono.fromCallable(() -> {
            Path jobDir = tempDir.resolve(jobId);
            Files.createDirectories(jobDir);
            return jobDir.resolve(sanitizeFileName(fileName));
        }).flatMap(destPath -> DataBufferUtils.write(dataStream, destPath)
                .then(Mono.just(destPath)));
    }

    public Path createOutputPath(String jobId, String fileName) throws IOException {
        Path jobDir = tempDir.resolve(jobId);
        Files.createDirectories(jobDir);
        return jobDir.resolve(sanitizeFileName(fileName));
    }

    public Flux<DataBuffer> readFile(Path filePath) {
        return DataBufferUtils.read(
                filePath,
                new org.springframework.core.io.buffer.DefaultDataBufferFactory(),
                8192);
    }

    public Mono<Void> deleteJob(String jobId) {
        return Mono.fromRunnable(() -> {
            try {
                Path jobDir = tempDir.resolve(jobId);
                if (Files.exists(jobDir)) {
                    Files.walk(jobDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception ignored) {
                                }
                            });
                    log.debug("Deleted job directory: {}", jobId);
                }
            } catch (IOException e) {
                log.warn("Failed to delete job {}: {}", jobId, e.getMessage());
            }
        });
    }

    public Path getTempDir() {
        return tempDir;
    }

    private String sanitizeFileName(String fileName) {
        // Remove path traversal attempts and invalid characters
        String sanitized = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\.\\.", "_");

        if (sanitized.isEmpty() || sanitized.equals("_")) {
            sanitized = "file_" + UUID.randomUUID().toString().substring(0, 8);
        }

        return sanitized;
    }
}
