package com.example.linky.dto;

import com.example.linky.entity.ShortUrl;

import java.time.Instant;

public record ShortUrlResponse(
        String shortKey,
        String shortUrl,
        String longUrl,
        boolean active,
        long usedCount,
        Instant createdAt
) {

    public static ShortUrlResponse from(ShortUrl shortUrl, String baseUrl) {
        return new ShortUrlResponse(
                shortUrl.getShortKey(),
                baseUrl + "/" + shortUrl.getShortKey(),
                shortUrl.getLongUrl(),
                shortUrl.isActive(),
                shortUrl.getUsedCount(),
                shortUrl.getCreatedAt()
        );
    }
}
