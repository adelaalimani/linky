package com.example.linky.service;

import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.User;
import com.example.linky.exception.NotFoundException;
import com.example.linky.repository.ShortUrlRepository;
import com.example.linky.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ShortUrlInsertService {

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;

    public ShortUrlInsertService(ShortUrlRepository shortUrlRepository, UserRepository userRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShortUrl insert(String shortKey, String longUrl, Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        return shortUrlRepository.saveAndFlush(
                new ShortUrl(shortKey, longUrl, existingUser)
        );
    }
}
