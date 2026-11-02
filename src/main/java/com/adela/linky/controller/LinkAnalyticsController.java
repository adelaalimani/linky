package com.adela.linky.controller;

import com.adela.linky.dto.response.LinkAnalyticsResponse;
import com.adela.linky.security.AuthUser;
import com.adela.linky.service.LinkAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints for viewing click statistics of short URLs.
 */
@RestController
@RequestMapping("/api/v1/links")
public class LinkAnalyticsController {

    private final LinkAnalyticsService linkAnalyticsService;

    public LinkAnalyticsController(LinkAnalyticsService linkAnalyticsService) {
        this.linkAnalyticsService = linkAnalyticsService;
    }

    /**
     * Returns total clicks and daily clicks for a date range (yyyy-MM-dd, default the last 30 days).
     * Customers can only see their own links, admins can see any.
     */
    @GetMapping("/{shortKey}/analytics")
    public ResponseEntity<LinkAnalyticsResponse> getAnalytics(
            @PathVariable String shortKey,
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(linkAnalyticsService.getAnalytics(shortKey, user, from, to));
    }
}
