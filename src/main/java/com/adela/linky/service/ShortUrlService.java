package com.adela.linky.service;

import com.adela.linky.dto.request.CreateShortUrlRequest;
import com.adela.linky.dto.response.ShortUrlResponse;
import com.adela.linky.entity.ShortUrl;
import com.adela.linky.entity.User;
import com.adela.linky.exception.ConflictException;
import com.adela.linky.exception.ForbiddenException;
import com.adela.linky.exception.NotFoundException;
import com.adela.linky.mapper.ShortUrlMapper;
import com.adela.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;

@Service
@Slf4j
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlInsertService shortUrlInsertService;
    private final ShortUrlCacheService shortUrlCacheService;
    private final LinkAnalyticsService linkAnalyticsService;
    private final CacheManager cacheManager;
    private final ShortUrlMapper shortUrlMapper;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 7;
    private static final int MAX_KEY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ShortUrlService(ShortUrlRepository shortUrlRepository, ShortUrlInsertService shortUrlInsertService, ShortUrlCacheService shortUrlCacheService, LinkAnalyticsService linkAnalyticsService, CacheManager cacheManager, ShortUrlMapper shortUrlMapper) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlInsertService = shortUrlInsertService;
        this.shortUrlCacheService = shortUrlCacheService;
        this.linkAnalyticsService = linkAnalyticsService;
        this.cacheManager = cacheManager;
        this.shortUrlMapper = shortUrlMapper;
    }

    public ShortUrlResponse create(CreateShortUrlRequest request, Long userId) {
        String longUrl = request.longUrl().trim();
        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            String key = randomKey();
            try {
                ShortUrl shortUrl = shortUrlInsertService.insert(key, longUrl, userId);
                log.info("Created short URL with key {}", key);
                return shortUrlMapper.toResponse(shortUrl);
            } catch (DataIntegrityViolationException ex) {

                if (!isShortKeyCollision(ex)) {
                    throw ex;
                }
                log.warn("Short key collision for {}, retrying...", key);
            }
        }
        throw new ConflictException("Could not generate a unique short key, please try again");
    }


    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> getAll(User user, Pageable pageable) {

        Page<ShortUrl> shortUrls = isAdmin(user)
                ? shortUrlRepository.findAll(pageable)
                : shortUrlRepository.findByUserId(user.getId(), pageable);
        return shortUrls.map(shortUrlMapper::toResponse);
    }

    public String resolve(String shortKey) {
        String longUrl = shortUrlCacheService.findLongUrl(shortKey);
        linkAnalyticsService.recordClick(shortKey);
        return longUrl;
    }

    @Transactional
    public void deactivate(String shortKey, User user) {
        ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new NotFoundException("Short url " + shortKey + " not found"));

        if (!isAdmin(user) && !shortUrl.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "You are not authorized to deactivate this short URL"
            );
        }
        shortUrl.deactivate();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        Cache cache = cacheManager.getCache("shortUrls");
                        if (cache != null) {
                            cache.evict(shortKey);
                        }
                    }
                }
        );
    }

    private boolean isAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private static String randomKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private boolean isShortKeyCollision(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                return "uk_short_urls_short_key".equals(cve.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
