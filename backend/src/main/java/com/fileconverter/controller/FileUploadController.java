package com.fileconverter.controller;

import com.fileconverter.config.AllowedFileTypes;
import com.fileconverter.converter.ConverterRegistry;
import com.fileconverter.dto.ErrorResponse;
import com.fileconverter.dto.UploadResponse;
import com.fileconverter.job.Job;
import com.fileconverter.job.JobService;
import com.fileconverter.security.FileValidator;
import com.fileconverter.security.JobTokenService;
import com.fileconverter.security.RateLimiter;
import com.fileconverter.storage.TempStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

        private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

        private final TempStorageService storageService;
        private final FileValidator fileValidator;
        private final JobService jobService;
        private final JobTokenService tokenService;
        private final RateLimiter rateLimiter;
        private final ConverterRegistry converterRegistry;

        public FileUploadController(
                        TempStorageService storageService,
                        FileValidator fileValidator,
                        JobService jobService,
                        JobTokenService tokenService,
                        RateLimiter rateLimiter,
                        ConverterRegistry converterRegistry) {
                this.storageService = storageService;
                this.fileValidator = fileValidator;
                this.jobService = jobService;
                this.tokenService = tokenService;
                this.rateLimiter = rateLimiter;
                this.converterRegistry = converterRegistry;
        }

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Mono<ResponseEntity<?>> uploadFile(
                        @RequestPart("file") Mono<FilePart> fileMono,
                        @RequestParam("targetFormat") String targetFormat,
                        ServerWebExchange exchange) {

                String clientIp = getClientIp(exchange);

                // Rate limiting check
                if (!rateLimiter.tryConsume(clientIp)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                        .body(ErrorResponse.of(429, "Too Many Requests",
                                                        "Rate limit exceeded. Please try again later.",
                                                        exchange.getRequest().getPath().value())));
                }

                return fileMono.flatMap(filePart -> {
                        String fileName = filePart.filename();
                        String contentType = filePart.headers().getContentType() != null
                                        ? filePart.headers().getContentType().toString()
                                        : "application/octet-stream";

                        log.info("Upload request: {} ({}) -> {}", fileName, contentType, targetFormat);

                        // Quick content type check
                        if (!fileValidator.isContentTypeAllowed(contentType)) {
                                return Mono.just(ResponseEntity.badRequest()
                                                .body(ErrorResponse.of(400, "Bad Request",
                                                                "File type not allowed: " + contentType,
                                                                exchange.getRequest().getPath().value())));
                        }

                        // Create temp job ID for file storage
                        String tempJobId = java.util.UUID.randomUUID().toString();

                        return storageService.saveUpload(filePart, tempJobId)
                                        .flatMap(savedPath -> fileValidator.validate(savedPath)
                                                        .<ResponseEntity<?>>flatMap(validation -> {
                                                                if (!validation.valid()) {
                                                                        return storageService.deleteJob(tempJobId)
                                                                                        .then(Mono.just(ResponseEntity
                                                                                                        .badRequest()
                                                                                                        .body((Object) ErrorResponse
                                                                                                                        .of(400, "Bad Request",
                                                                                                                                        validation.errorMessage(),
                                                                                                                                        exchange.getRequest()
                                                                                                                                                        .getPath()
                                                                                                                                                        .value()))));
                                                                }

                                                                // Check if conversion is supported
                                                                if (!converterRegistry.isConversionSupported(
                                                                                validation.mimeType(), targetFormat)) {
                                                                        return storageService.deleteJob(tempJobId)
                                                                                        .then(Mono.just(ResponseEntity
                                                                                                        .badRequest()
                                                                                                        .body((Object) ErrorResponse
                                                                                                                        .of(400, "Bad Request",
                                                                                                                                        "Conversion from "
                                                                                                                                                        + validation.mimeType()
                                                                                                                                                        +
                                                                                                                                                        " to "
                                                                                                                                                        + targetFormat
                                                                                                                                                        + " not supported",
                                                                                                                                        exchange.getRequest()
                                                                                                                                                        .getPath()
                                                                                                                                                        .value()))));
                                                                }

                                                                return jobService.createJob(
                                                                                fileName, validation.mimeType(),
                                                                                targetFormat, savedPath)
                                                                                .map(job -> {
                                                                                        String token = tokenService
                                                                                                        .generateToken(job
                                                                                                                        .getId());

                                                                                        UploadResponse response = new UploadResponse(
                                                                                                        job.getId(),
                                                                                                        token,
                                                                                                        fileName,
                                                                                                        validation.mimeType(),
                                                                                                        validation.fileSize(),
                                                                                                        targetFormat,
                                                                                                        job.getCreatedAt(),
                                                                                                        job.getExpiresAt());

                                                                                        return (ResponseEntity<?>) ResponseEntity
                                                                                                        .ok()
                                                                                                        .header("X-Job-Id",
                                                                                                                        job.getId())
                                                                                                        .body(response);
                                                                                });
                                                        }));
                }).onErrorResume(e -> {
                        log.error("Upload error", e);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ErrorResponse.of(500, "Internal Server Error",
                                                        "Failed to process upload: " + e.getMessage(),
                                                        exchange.getRequest().getPath().value())));
                });
        }

        @GetMapping("/formats")
        public Mono<ResponseEntity<Map<String, Object>>> getSupportedFormats(
                        @RequestParam(required = false) String mimeType) {

                if (mimeType != null) {
                        List<String> formats = converterRegistry.getSupportedOutputFormats(mimeType);
                        return Mono.just(ResponseEntity.ok(Map.of(
                                        "inputType", mimeType,
                                        "outputFormats", formats)));
                }

                return Mono.just(ResponseEntity.ok(Map.of(
                                "supportedInputTypes", AllowedFileTypes.ALL_ALLOWED,
                                "categories", Map.of(
                                                "image", AllowedFileTypes.IMAGE_TYPES,
                                                "document", AllowedFileTypes.DOCUMENT_TYPES,
                                                "media", AllowedFileTypes.MEDIA_TYPES))));
        }

        private String getClientIp(ServerWebExchange exchange) {
                String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                        return forwarded.split(",")[0].trim();
                }
                var remoteAddr = exchange.getRequest().getRemoteAddress();
                return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
        }
}
