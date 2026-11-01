package com.adela.linky.security;

import com.adela.linky.security.jwt.JwtFilter;
import com.adela.linky.security.ratelimiter.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.adela.linky.security.utils.SecurityUrls.*;

/**
 * Spring Security setup: which URLs are public, stateless JWT login, and the order of the custom filters.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtFilter jwtFilter,
                          RateLimitFilter rateLimitFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Auth, docs, actuator and redirects are public, everything else needs a token; no sessions are kept.
     * JwtFilter runs first to log the user in, then RateLimitFilter so it can limit per user.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(AUTH_URLS).permitAll()
                                .requestMatchers(DOCS_URLS).permitAll()
                                .requestMatchers(ACTUATOR_URLS).permitAll()
                                .requestMatchers(HttpMethod.GET, REDIRECT_URL).permitAll()
                                .anyRequest().authenticated()

                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(this.authenticationEntryPoint))
                .addFilterBefore(this.jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(this.rateLimitFilter, JwtFilter.class)
                .build();
    }

    /**
     * Stops Spring Boot from also registering JwtFilter as a normal servlet filter, so it runs only in the security chain.
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> disableJwtAutoRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Stops Spring Boot from also registering RateLimitFilter as a normal servlet filter, so it runs only in the security chain.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitAutoRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Hashes passwords with BCrypt, used at registration and to check the password at login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager so AuthenticationService can check email and password at login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
