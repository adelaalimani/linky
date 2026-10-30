package com.example.linky.service;

import com.example.linky.dto.request.CreateShortUrlRequest;
import com.example.linky.dto.response.ShortUrlResponse;
import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.User;
import com.example.linky.exception.ConflictException;
import com.example.linky.exception.NotFoundException;
import com.example.linky.mapper.ShortUrlMapper;
import com.example.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@Slf4j
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlInsertService shortUrlInsertService;
    private final ShortUrlMapper shortUrlMapper;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 7;
    private static final int MAX_KEY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ShortUrlService(ShortUrlRepository shortUrlRepository, ShortUrlInsertService shortUrlInsertService, ShortUrlMapper shortUrlMapper) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlInsertService = shortUrlInsertService;
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
    public ShortUrlResponse get(String shortKey) {
        return shortUrlMapper.toResponse(findByShortKey(shortKey));
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> getAll(User user, Pageable pageable) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        Page<ShortUrl> shortUrls = isAdmin
                ? shortUrlRepository.findAll(pageable)
                : shortUrlRepository.findByUserId(user.getId(), pageable);
        return shortUrls.map(shortUrlMapper::toResponse);
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
