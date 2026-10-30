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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Page<ShortUrlResponse>> getAll(
            @AuthenticationPrincipal User user,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(shortUrlService.getAll(user, pageable));
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
