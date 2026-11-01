package com.adela.linky.controller;

import com.adela.linky.security.utils.SecurityUrls;
import com.adela.linky.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Public endpoint that sends visitors from a short URL to its original long URL.
 */
@RestController
public class RedirectController {

    private final ShortUrlService shortUrlService;

    public RedirectController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    /**
     * Redirects with 302 to the long URL. Unknown or inactive keys return 404.
     */
    @GetMapping(SecurityUrls.REDIRECT_URL)
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortUrlService.resolve(shortKey)))
                .build();
    }
}
