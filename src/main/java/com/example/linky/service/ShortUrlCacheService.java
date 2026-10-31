package com.example.linky.service;

import com.example.linky.entity.ShortUrl;
import com.example.linky.exception.NotFoundException;
import com.example.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
@Slf4j
public class ShortUrlCacheService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlCacheService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Cacheable(cacheNames = "shortUrls", key = "#shortKey")
    @Transactional(readOnly = true)
    public String findLongUrl(String shortKey) {

        log.info("Cache MISS - querying PostgreSQL for {}", shortKey);
        ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new NotFoundException("Short URL not found"));

        if (!shortUrl.isActive()) {
            throw new NotFoundException("Short URL inactive");
        }
        return shortUrl.getLongUrl();
    }
}
