package com.example.linky.security.jwt;

import com.example.linky.exception.BadRequestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${JWT_PRIVATE_KEY:}") String privateKeyValue,
            @Value("${JWT_PUBLIC_KEY:}") String publicKeyValue,
            @Value("${app.security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.security.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey("keys/private_key.pem", privateKeyValue);
        this.publicKey = KeyUtils.loadPublicKey("keys/public_key.pem", publicKeyValue);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String username) {
        Map<String, Object> claims = Map.of(TOKEN_TYPE, ACCESS_TOKEN);
        return buildToken(username, claims, this.accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = Map.of(TOKEN_TYPE, REFRESH_TOKEN);
        return buildToken(username, claims, this.refreshTokenExpiration);
    }

    private String buildToken(String username, Map<String, Object> claims, long tokenExpiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(privateKey)
                .compact();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        return isTokenValid(token, expectedUsername, ACCESS_TOKEN);
    }

    public boolean isRefreshTokenValid(String token, String expectedUsername) {
        return isTokenValid(token, expectedUsername, REFRESH_TOKEN);
    }

    private boolean isTokenValid(String token, String expectedUsername, String expectedType) {
        Claims claims = extractClaims(token);
        return claims.getSubject().equals(expectedUsername)
                && expectedType.equals(claims.get(TOKEN_TYPE))
                && claims.getExpiration().after(new Date());
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new BadRequestException("Invalid JWT token");
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

}
