package com.example.linky.security.ratelimiter;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public BucketConfiguration defaultBucketConfiguration(
            @Value("${app.rate-limit.default.capacity}") long capacity,
            @Value("${app.rate-limit.default.refill-period}") Duration refillPeriod
    ) {
        return buildConfiguration(capacity, refillPeriod);
    }

    @Bean
    public BucketConfiguration authBucketConfiguration(
            @Value("${app.rate-limit.auth.capacity}") long capacity,
            @Value("${app.rate-limit.auth.refill-period}") Duration refillPeriod
    ) {
        return buildConfiguration(capacity, refillPeriod);
    }

    private static BucketConfiguration buildConfiguration(long capacity, Duration refillPeriod) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
                .build();
    }
}
