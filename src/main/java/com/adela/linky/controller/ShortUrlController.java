package com.adela.linky.controller;

import com.adela.linky.dto.request.CreateShortUrlRequest;
import com.adela.linky.dto.response.ShortUrlResponse;
import com.adela.linky.security.AuthUser;
import com.adela.linky.service.ShortUrlService;
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

/**
 * Endpoints for creating, listing, and deactivating short URLs; requires login.
 */
@RestController
@RequestMapping("/api/v1/short-urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    /**
     * Creates a short URL with a random 7-character key for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
                                                   @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(request, user.id()));
    }

    /**
     * Paginated list of short URLs, newest first. Customers get their own links, admins get all.
     */
    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> getAll(
            @AuthenticationPrincipal AuthUser user,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(shortUrlService.getAll(user, pageable));
    }

    /**
     * Deactivates a short URL so it no longer redirects. Customers can only deactivate their own links, admins any.
     */
    @PatchMapping("/{shortKey}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String shortKey, @AuthenticationPrincipal AuthUser user) {
        shortUrlService.deactivate(shortKey, user);
        return ResponseEntity.noContent().build();
    }
}
