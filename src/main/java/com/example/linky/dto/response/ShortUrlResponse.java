package com.example.linky.dto.response;

import java.time.Instant;

public record ShortUrlResponse(
        String shortKey,
        String shortUrl,
        String longUrl,
        boolean active,
        long usedCount,
        Instant lastAccessedAt,
        Instant createdAt
) {
}
