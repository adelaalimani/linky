package com.example.linky.service;

import com.example.linky.entity.ShortUrl;
import com.example.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ShortUrlInsertService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlInsertService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Transactional
    public ShortUrl insert(String shortKey, String longUrl) {
        return shortUrlRepository.saveAndFlush(
                new ShortUrl(shortKey, longUrl)
        );
    }
}
