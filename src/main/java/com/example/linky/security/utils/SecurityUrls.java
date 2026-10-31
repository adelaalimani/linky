package com.example.linky.security.utils;

public final class SecurityUrls {

    private SecurityUrls() {
    }

    public static final String[] AUTH_URLS = {
            "/api/v1/auth/**"
    };

    public static final String[] DOCS_URLS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public static final String[] ACTUATOR_URLS = {
            "/actuator/health/**",
            "/actuator/info"
    };

    public static final String REDIRECT_URL = "/{shortKey:[0-9A-Za-z]{7}}";
}
