package com.adela.linky.security;

import com.adela.linky.entity.Role;
import com.adela.linky.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * The authenticated user, built from the access token claims (no database lookup).
 */
public record AuthUser(Long id, String email, List<String> roles) {

    private static final String ADMIN = "ADMIN";

    public AuthUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public static AuthUser from(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getName).toList();
        return new AuthUser(user.getId(), user.getUsername(), roles);
    }

    public boolean isAdmin() {
        return roles.contains(ADMIN);
    }

    public List<GrantedAuthority> authorities() {
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
