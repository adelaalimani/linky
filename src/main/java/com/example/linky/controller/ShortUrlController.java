package com.example.linky.controller;

import com.example.linky.dto.request.CreateShortUrlRequest;
import com.example.linky.dto.response.ShortUrlResponse;
import com.example.linky.entity.User;
import com.example.linky.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/short-urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(request, user.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> getAll(
            @AuthenticationPrincipal User user,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(shortUrlService.getAll(user, pageable));
    }

    @PatchMapping("/{shortKey}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String shortKey, @AuthenticationPrincipal User user) {
        shortUrlService.deactivate(shortKey, user);
        return ResponseEntity.noContent().build();
    }
}
