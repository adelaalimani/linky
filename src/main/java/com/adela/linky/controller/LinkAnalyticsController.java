package com.adela.linky.controller;

import com.adela.linky.dto.response.LinkAnalyticsResponse;
import com.adela.linky.security.AuthUser;
import com.adela.linky.service.LinkAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class LinkAnalyticsController {

    private final LinkAnalyticsService linkAnalyticsService;

    public LinkAnalyticsController(LinkAnalyticsService linkAnalyticsService) {
        this.linkAnalyticsService = linkAnalyticsService;
    }

    @GetMapping("/{shortKey}/analytics")
    public ResponseEntity<LinkAnalyticsResponse> getAnalytics(
            @PathVariable String shortKey,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(linkAnalyticsService.getAnalytics(shortKey, user)
        );
    }
}
