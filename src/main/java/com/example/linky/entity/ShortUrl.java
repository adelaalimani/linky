package com.example.linky.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "used_count", nullable = false)
    private long usedCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ShortUrl(String shortKey, String longUrl) {
        this.shortKey = shortKey;
        this.longUrl = longUrl;
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
