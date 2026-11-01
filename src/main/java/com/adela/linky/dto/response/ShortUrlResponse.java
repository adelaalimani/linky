package com.adela.linky.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record ShortUrlResponse(
        String shortKey,
        String shortUrl,
        String longUrl,
        boolean active,
        long usedCount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant lastAccessedAt,
        Instant createdAt
) {
}
