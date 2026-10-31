package com.adela.linky.controller;

import com.adela.linky.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final ShortUrlService shortUrlService;

    public RedirectController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/{shortKey:[0-9A-Za-z]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortUrlService.resolve(shortKey)))
                .build();
    }
}
