package com.example.linky.security.ratelimiter;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

import static com.example.linky.security.utils.SecurityUrls.*;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SKIPPED_URLS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean authRequest = AUTH_MATCHER.matches(request);
        String key = authRequest
                ? "ratelimit:auth:ip:" + request.getRemoteAddr()
                : resolveRateLimitKey(request);
        BucketConfiguration bucketConfiguration = authRequest
                ? authBucketConfiguration
                : defaultBucketConfiguration;

        ConsumptionProbe probe;
        try {
            probe = proxyManager.builder()
                    .build(key, () -> bucketConfiguration)
                    .tryConsumeAndReturnRemaining(1);
        } catch (Exception e) {
            log.error("Rate limiter unavailable, letting request through", e);
            filterChain.doFilter(request, response);
            return;
        }

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = (probe.getNanosToWaitForRefill() + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND;
        log.warn("Rate limit exceeded for {}", key);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("Rate limit exceeded, please try again later");
    }

    private String resolveRateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "ratelimit:user:" + authentication.getName();
        }
        return "ratelimit:ip:" + request.getRemoteAddr();
    }

    private static RequestMatcher matcherFor(String[]... urlGroups) {
        return new OrRequestMatcher(
                Stream.of(urlGroups)
                        .flatMap(Arrays::stream)
                        .map(url -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(url))
                        .toList()
        );
    }
}
