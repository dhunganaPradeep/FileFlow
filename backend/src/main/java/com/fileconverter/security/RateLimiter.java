package com.fileconverter.security;

import com.fileconverter.config.AppConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets;
    private final int requestsPerMinute;
    private final int burstCapacity;

    public RateLimiter(AppConfig config) {
        this.buckets = new ConcurrentHashMap<>();
        this.requestsPerMinute = config.getSecurity().getRateLimit().getRequestsPerMinute();
        this.burstCapacity = config.getSecurity().getRateLimit().getBurstCapacity();
    }

    public boolean tryConsume(String clientId) {
        Bucket bucket = buckets.computeIfAbsent(clientId, this::createBucket);
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(String clientId) {
        Bucket bucket = buckets.get(clientId);
        return bucket != null ? bucket.getAvailableTokens() : burstCapacity;
    }

    private Bucket createBucket(String clientId) {
        Bandwidth limit = Bandwidth.classic(
                burstCapacity,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    public void cleanup() {
        // Remove buckets that haven't been accessed (simple cleanup)
        // In production, use a cache with TTL
        if (buckets.size() > 10000) {
            buckets.clear();
        }
    }
}
