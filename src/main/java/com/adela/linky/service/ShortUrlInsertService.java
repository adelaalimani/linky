package com.adela.linky.service;

import com.adela.linky.entity.ShortUrl;
import com.adela.linky.entity.User;
import com.adela.linky.exception.NotFoundException;
import com.adela.linky.repository.ShortUrlRepository;
import com.adela.linky.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves a new short URL in its own transaction. It's a separate class so that a duplicate key only
 * rolls back this one attempt, and ShortUrlService can retry with a new key.
 */
@Service
@Slf4j
public class ShortUrlInsertService {

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;

    public ShortUrlInsertService(ShortUrlRepository shortUrlRepository, UserRepository userRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.userRepository = userRepository;
    }

    /**
     * Inserts the short URL for the user.
     */
    @Transactional
    public ShortUrl insert(String shortKey, String longUrl, Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        return shortUrlRepository.saveAndFlush(
                new ShortUrl(shortKey, longUrl, existingUser)
        );
    }
}
