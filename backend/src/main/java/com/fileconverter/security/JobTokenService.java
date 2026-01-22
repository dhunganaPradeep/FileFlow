package com.fileconverter.security;

import com.fileconverter.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JobTokenService {

    private final SecretKey secretKey;
    private final int ttlMinutes;

    public JobTokenService(AppConfig config) {
        String secret = config.getSecurity().getTokenSecret();
        // Ensure key is at least 256 bits
        String paddedSecret = String.format("%-32s", secret).substring(0, 32);
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = config.getSecurity().getTokenTtlMinutes();
    }

    public String generateToken(String jobId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlMinutes * 60L);

        return Jwts.builder()
                .subject(jobId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public TokenValidation validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jobId = claims.getSubject();
            Instant expiry = claims.getExpiration().toInstant();

            if (Instant.now().isAfter(expiry)) {
                return TokenValidation.expiredToken();
            }

            return TokenValidation.valid(jobId, expiry);

        } catch (Exception e) {
            return TokenValidation.invalid(e.getMessage());
        }
    }

    public record TokenValidation(
            boolean valid,
            boolean expired,
            String jobId,
            Instant expiresAt,
            String error) {
        public static TokenValidation valid(String jobId, Instant expiresAt) {
            return new TokenValidation(true, false, jobId, expiresAt, null);
        }

        public static TokenValidation expiredToken() {
            return new TokenValidation(false, true, null, null, "Token expired");
        }

        public static TokenValidation invalid(String error) {
            return new TokenValidation(false, false, null, null, error);
        }
    }
}
