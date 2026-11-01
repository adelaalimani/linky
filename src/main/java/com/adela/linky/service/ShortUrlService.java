package com.adela.linky.service;

import com.adela.linky.dto.request.CreateShortUrlRequest;
import com.adela.linky.dto.response.ShortUrlResponse;
import com.adela.linky.entity.ShortUrl;
import com.adela.linky.security.AuthUser;
import com.adela.linky.exception.BadRequestException;
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

/**
 * Creates, lists, resolves and deactivates short URLs.
 */
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

    /**
     * Saves the long URL under a random 7-character key, trying a new key (up to 5 times) if it's already taken.
     */
    public ShortUrlResponse create(CreateShortUrlRequest request, Long userId) {
        String longUrl = request.longUrl().trim();
        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            String key = randomKey();
            try {
                ShortUrl shortUrl = shortUrlInsertService.insert(key, longUrl, userId);
                log.info("Created short URL with key {} for user {}", key, userId);
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

    /**
     * Returns a page of links: all links for admins, only their own for customers.
     */
    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> getAll(AuthUser user, Pageable pageable) {
        Page<ShortUrl> shortUrls = user.isAdmin()
                ? shortUrlRepository.findAll(pageable)
                : shortUrlRepository.findByUserId(user.id(), pageable);
        log.info("User {} listed {} short URLs, page {}", user.id(), shortUrls.getNumberOfElements(), pageable.getPageNumber());
        return shortUrls.map(shortUrlMapper::toResponse);
    }

    /**
     * Returns the long URL for a redirect (from the cache when possible) and records the click.
     */
    public String resolve(String shortKey) {
        if (shortKey.length() != KEY_LENGTH) {
            throw new BadRequestException("Short URL key is not valid");
        }
        String longUrl = shortUrlCacheService.findLongUrl(shortKey);
        linkAnalyticsService.recordClick(shortKey);
        log.info("Redirecting {} to {}", shortKey, longUrl);
        return longUrl;
    }

    /**
     * Deactivates a link (owner or admin only) and removes it from the cache after the commit,
     * so a redirect running at the same time can't put the old value back.
     */
    @Transactional
    public void deactivate(String shortKey, AuthUser user) {
        ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new NotFoundException("Short url " + shortKey + " not found"));

        if (!user.isAdmin() && !shortUrl.getUser().getId().equals(user.id())) {
            throw new ForbiddenException("You are not authorized to deactivate this short URL");
        }
        shortUrl.setActive(false);
        log.info("Short URL {} deactivated by user {}", shortKey, user.id());

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

    /**
     * Builds a random 7-character key from letters and digits, using SecureRandom so keys can't be guessed.
     */
    private static String randomKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * True if the error is the unique constraint on short_key.
     */
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
