package com.adela.linky.mapper;

import com.adela.linky.dto.response.ShortUrlResponse;
import com.adela.linky.entity.ShortUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlMapper {

    private final String baseUrl;

    public ShortUrlMapper(@Value("${linky.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getShortKey(),
                this.baseUrl + "/" + shortUrl.getShortKey(),
                shortUrl.getLongUrl(),
                shortUrl.isActive(),
                shortUrl.getUsedCount(),
                shortUrl.getLastAccessedAt(),
                shortUrl.getCreatedAt()
        );
    }

}
