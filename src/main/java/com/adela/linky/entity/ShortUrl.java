package com.adela.linky.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a short URL created by a user, with its long URL and click statistics.
 */
@Entity
@Table(
        name = "short_urls",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_short_urls_short_key",
                columnNames = "short_key"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_key", nullable = false, length = 7)
    private String shortKey;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "used_count", nullable = false)
    private long usedCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ShortUrl(String shortKey, String longUrl, User user) {
        this.shortKey = shortKey;
        this.longUrl = longUrl;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
