package com.adela.linky.security.utils;

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

    // Any length is matched so a wrong-length key gets a clear 400 instead of falling through to static resources
    public static final String REDIRECT_URL = "/{shortKey:[0-9A-Za-z]+}";
}
