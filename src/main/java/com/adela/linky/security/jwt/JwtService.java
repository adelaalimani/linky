package com.adela.linky.security.jwt;

import com.adela.linky.exception.BadRequestException;
import com.adela.linky.security.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtService {

    private static final String ROLES = "roles";
    private static final String USER_ID = "user_id";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpiration;

    public JwtService(
            @Value("${JWT_PRIVATE_KEY:}") String privateKeyValue,
            @Value("${JWT_PUBLIC_KEY:}") String publicKeyValue,
            @Value("${app.security.jwt.access-token-expiration}") long accessTokenExpiration
    ) throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey("keys/private_key.pem", privateKeyValue);
        this.publicKey = KeyUtils.loadPublicKey("keys/public_key.pem", publicKeyValue);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(AuthUser user) {
        Map<String, Object> claims = Map.of(
                USER_ID, user.id(),
                ROLES, user.roles());
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(user.email())
                .issuedAt(new Date(now))
                .expiration(new Date(now + this.accessTokenExpiration))
                .signWith(privateKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry, then builds the user from the claims.
     * Roles come from the token, so role changes apply only after the user logs in again.
     */
    public AuthUser parseAccessToken(String token) {
        Claims claims = extractClaims(token);
        if (claims.getSubject() == null
                || !(claims.get(USER_ID) instanceof Number userId)
                || !(claims.get(ROLES) instanceof List<?> roles)) {
            throw new BadRequestException("Invalid JWT token");
        }
        List<String> roleNames = roles.stream().map(String::valueOf).toList();
        return new AuthUser(userId.longValue(), claims.getSubject(), roleNames);
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

}
