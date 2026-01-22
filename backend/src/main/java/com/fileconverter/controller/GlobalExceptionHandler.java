package com.fileconverter.controller;

import com.fileconverter.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(IllegalArgumentException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(
                        IllegalArgumentException ex, ServerWebExchange exchange) {
                log.warn("Bad request: {}", ex.getMessage());
                return Mono.just(ResponseEntity.badRequest()
                                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(),
                                                exchange.getRequest().getPath().value())));
        }

        @ExceptionHandler(IllegalStateException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleIllegalState(
                        IllegalStateException ex, ServerWebExchange exchange) {
                log.warn("Conflict: {}", ex.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(),
                                                exchange.getRequest().getPath().value())));
        }

        @ExceptionHandler(Exception.class)
        public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
                        Exception ex, ServerWebExchange exchange) {
                log.error("Unhandled error", ex);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of(500, "Internal Server Error",
                                                ex.getMessage() != null ? ex.getMessage()
                                                                : "An unexpected error occurred",
                                                exchange.getRequest().getPath().value())));
        }
}
