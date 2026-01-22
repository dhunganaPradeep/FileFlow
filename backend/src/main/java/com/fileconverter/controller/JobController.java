package com.fileconverter.controller;

import com.fileconverter.dto.ErrorResponse;
import com.fileconverter.dto.JobStatusResponse;
import com.fileconverter.job.JobService;
import com.fileconverter.security.JobTokenService;
import com.fileconverter.storage.TempStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;
    private final JobTokenService tokenService;
    private final TempStorageService storageService;

    public JobController(
            JobService jobService,
            JobTokenService tokenService,
            TempStorageService storageService) {
        this.jobService = jobService;
        this.tokenService = tokenService;
        this.storageService = storageService;
    }

    @GetMapping("/{jobId}")
    public Mono<ResponseEntity<?>> getJobStatus(
            @PathVariable String jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            ServerWebExchange exchange) {

        // Validate token
        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(unauthorized(exchange));
        }

        var validation = tokenService.validateToken(token);
        if (!validation.valid()) {
            String error = validation.expired() ? "Token expired" : "Invalid token";
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(401, "Unauthorized", error,
                            exchange.getRequest().getPath().value())));
        }

        if (!validation.jobId().equals(jobId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of(403, "Forbidden", "Token does not match job",
                            exchange.getRequest().getPath().value())));
        }

        return jobService.getJobStatus(jobId)
                .<ResponseEntity<?>>map(status -> ResponseEntity.ok(status))
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.of(404, "Not Found", e.getMessage(),
                                        exchange.getRequest().getPath().value()))));
    }

    @GetMapping(value = "/{jobId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @PathVariable String jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            ServerWebExchange exchange) {

        // Validate token
        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        var validation = tokenService.validateToken(token);
        if (!validation.valid() || !validation.jobId().equals(jobId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return jobService.getOutputPath(jobId)
                .flatMap(outputPath -> jobService.getOutputFileName(jobId)
                        .map(fileName -> {
                            if (!Files.exists(outputPath)) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .<Flux<DataBuffer>>build();
                            }

                            Flux<DataBuffer> fileStream = storageService.readFile(outputPath);

                            return ResponseEntity.ok()
                                    .header(HttpHeaders.CONTENT_DISPOSITION,
                                            "attachment; filename=\"" + fileName + "\"")
                                    .header("X-Job-Id", jobId)
                                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                    .body(fileStream);
                        }))
                .onErrorResume(e -> {
                    log.error("Download error for job {}: {}", jobId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{jobId}")
    public Mono<ResponseEntity<?>> deleteJob(
            @PathVariable String jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            ServerWebExchange exchange) {

        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(unauthorized(exchange));
        }

        var validation = tokenService.validateToken(token);
        if (!validation.valid() || !validation.jobId().equals(jobId)) {
            return Mono.just(unauthorized(exchange));
        }

        return storageService.deleteJob(jobId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private ResponseEntity<?> unauthorized(ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Unauthorized", "Missing or invalid token",
                        exchange.getRequest().getPath().value()));
    }
}
