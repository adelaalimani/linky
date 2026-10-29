package com.example.linky.service;

import com.example.linky.dto.CreateShortUrlRequest;
import com.example.linky.dto.ShortUrlResponse;
import com.example.linky.entity.ShortUrl;
import com.example.linky.exception.ConflictException;
import com.example.linky.exception.NotFoundException;
import com.example.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@Slf4j
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlInsertService shortUrlInsertService;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 7;
    private static final int MAX_KEY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${linky.base-url}")
    private String baseUrl;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, ShortUrlInsertService shortUrlInsertService) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlInsertService = shortUrlInsertService;
    }

    public ShortUrlResponse create(CreateShortUrlRequest request) {
        String longUrl = request.longUrl().trim();
        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            String key = randomKey();
            try {
                ShortUrl shortUrl = shortUrlInsertService.insert(key, longUrl);
                log.info("Created short URL with key {}", key);
                return ShortUrlResponse.from(shortUrl, baseUrl);
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
    public ShortUrlResponse get(String shortKey) {
        return ShortUrlResponse.from(findByShortKey(shortKey), baseUrl);
    }

    @Transactional
    public String resolve(String shortKey) {
        ShortUrl shortUrl = findByShortKey(shortKey);
        if (!shortUrl.isActive()) {
            throw new NotFoundException("Short url " + shortKey + " is not active");
        }
        shortUrlRepository.incrementUsedCount(shortUrl.getId());
        return shortUrl.getLongUrl();
    }

    @Transactional
    public void deactivate(String shortKey) {
        findByShortKey(shortKey).deactivate();
    }

    private ShortUrl findByShortKey(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new NotFoundException("Short url " + shortKey + " not found"));
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
