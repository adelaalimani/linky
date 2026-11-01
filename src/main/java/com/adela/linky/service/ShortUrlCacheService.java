package com.adela.linky.service;

import com.adela.linky.entity.ShortUrl;
import com.adela.linky.exception.NotFoundException;
import com.adela.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

/**
 * Cached lookup of a short key's long URL. It's a separate class because @Cacheable works through
 * a Spring proxy, so calling it from inside ShortUrlService itself would skip the cache.
 */
@Service
@Slf4j
public class ShortUrlCacheService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlCacheService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    /**
     * Returns the long URL from Redis, or from Postgres on a cache miss and caches it.
     * Unknown or inactive keys throw NotFoundException and are not cached.
     */
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
