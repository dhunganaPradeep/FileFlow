package com.fileconverter.security;

import com.fileconverter.config.AllowedFileTypes;
import com.fileconverter.config.AppConfig;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileValidator {

    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

    private final Tika tika;
    private final long maxFileSize;

    public FileValidator(AppConfig config) {
        this.tika = new Tika();
        this.maxFileSize = config.getStorage().getMaxFileSize();
    }

    /**
     * Validate file by checking MIME type via magic bytes
     */
    public Mono<ValidationResult> validate(Path filePath) {
        return Mono.fromCallable(() -> {
            // Check file exists
            if (!Files.exists(filePath)) {
                return ValidationResult.error("File not found");
            }

            // Check file size
            long size = Files.size(filePath);
            if (size > maxFileSize) {
                return ValidationResult.error(
                        "File too large. Maximum size is " + (maxFileSize / 1024 / 1024) + "MB");
            }

            if (size == 0) {
                return ValidationResult.error("Empty file");
            }

            // Detect MIME type via magic bytes
            String detectedMime;
            try (InputStream is = Files.newInputStream(filePath)) {
                detectedMime = tika.detect(is);
            }

            log.debug("Detected MIME type: {}", detectedMime);

            // Check against allow-list
            if (!AllowedFileTypes.isAllowed(detectedMime)) {
                return ValidationResult.error(
                        "File type not allowed: " + detectedMime);
            }

            return ValidationResult.success(detectedMime, size);
        });
    }

    /**
     * Quick validation of declared content type (for early rejection)
     */
    public boolean isContentTypeAllowed(String contentType) {
        return AllowedFileTypes.isAllowed(contentType);
    }

    public record ValidationResult(
            boolean valid,
            String mimeType,
            long fileSize,
            String errorMessage) {
        public static ValidationResult success(String mimeType, long fileSize) {
            return new ValidationResult(true, mimeType, fileSize, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, null, 0, message);
        }
    }
}
