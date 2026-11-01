package com.adela.linky.security.ratelimiter;

import com.adela.linky.security.AuthUser;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.adela.linky.security.utils.SecurityUrls.*;

/**
 * Limits how many requests a user or IP can make, using token buckets stored in Redis.
 * Over the limit returns 429.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final RequestMatcher SKIPPED_URLS = matcherFor(DOCS_URLS, ACTUATOR_URLS);
    private static final RequestMatcher AUTH_MATCHER = matcherFor(AUTH_URLS);

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration defaultBucketConfiguration;
    private final BucketConfiguration authBucketConfiguration;

    public RateLimitFilter(ProxyManager<String> proxyManager,
                           @Qualifier("defaultBucketConfiguration") BucketConfiguration defaultBucketConfiguration,
                           @Qualifier("authBucketConfiguration") BucketConfiguration authBucketConfiguration) {
        this.proxyManager = proxyManager;
        this.defaultBucketConfiguration = defaultBucketConfiguration;
        this.authBucketConfiguration = authBucketConfiguration;
    }

    /**
     * Swagger docs and actuator endpoints are not rate limited.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return SKIPPED_URLS.matches(request);
    }

    /**
     * Takes one token from the caller's bucket, auth requests use the stricter per-IP limit.
     * Returns 429 when the bucket is empty, and lets the request through if Redis is unavailable.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        boolean authRequest = AUTH_MATCHER.matches(request);
        String key = authRequest
                ? "ratelimit:auth:ip:" + request.getRemoteAddr()
                : resolveRateLimitKey(request);
        BucketConfiguration bucketConfiguration = authRequest
                ? authBucketConfiguration
                : defaultBucketConfiguration;

        boolean allowed;
        try {
            allowed = proxyManager.builder()
                    .build(key, () -> bucketConfiguration)
                    .tryConsume(1);
        } catch (Exception e) {
            log.error("Rate limiter unavailable, letting request through", e);
            allowed = true;
        }

        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {}", key);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("Rate limit exceeded, please try again later");
    }

    /**
     * Logged-in users get a bucket per user id, everyone else a bucket per IP address.
     */
    private String resolveRateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return "ratelimit:user:" + user.id();
        }
        return "ratelimit:ip:" + request.getRemoteAddr();
    }

    /**
     * Combines URL patterns into one matcher.
     */
    private static RequestMatcher matcherFor(String[]... urlGroups) {
        return new OrRequestMatcher(
                Stream.of(urlGroups)
                        .flatMap(Arrays::stream)
                        .map(url -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(url))
                        .toList()
        );
    }
}
