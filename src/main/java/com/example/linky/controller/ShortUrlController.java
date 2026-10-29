package com.example.linky.controller;

import com.example.linky.dto.request.CreateShortUrlRequest;
import com.example.linky.dto.response.ShortUrlResponse;
import com.example.linky.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/short-urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(request));
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<ShortUrlResponse> get(@PathVariable String shortKey) {
        return ResponseEntity.ok(shortUrlService.get(shortKey));
    }

    @PatchMapping("/{shortKey}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String shortKey) {
        shortUrlService.deactivate(shortKey);
        return ResponseEntity.noContent().build();
    }
}
