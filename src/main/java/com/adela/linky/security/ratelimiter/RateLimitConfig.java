package com.adela.linky.security.ratelimiter;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Defines the rate limits from app.rate-limit in application.yaml.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Limit for all endpoints except auth: 60 requests per minute per user, or per IP when not logged in.
     */
    @Bean
    public BucketConfiguration defaultBucketConfiguration(
            @Value("${app.rate-limit.default.capacity}") long capacity,
            @Value("${app.rate-limit.default.refill-period}") Duration refillPeriod
    ) {
        return buildConfiguration(capacity, refillPeriod);
    }

    /**
     * Stricter limit for /api/v1/auth/**: 5 requests per minute per IP, to slow down password guessing.
     */
    @Bean
    public BucketConfiguration authBucketConfiguration(
            @Value("${app.rate-limit.auth.capacity}") long capacity,
            @Value("${app.rate-limit.auth.refill-period}") Duration refillPeriod
    ) {
        return buildConfiguration(capacity, refillPeriod);
    }

    /**
     * A bucket holding `capacity` requests that refills gradually over the refill period.
     */
    private static BucketConfiguration buildConfiguration(long capacity, Duration refillPeriod) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
                .build();
    }
}
