package com.adela.linky.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "short_url_daily_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_short_url_daily_stats",
                columnNames = {"short_url_id", "stat_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortUrlDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_url_id", nullable = false)
    private ShortUrl shortUrl;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "clicks", nullable = false)
    private long clicks;

}
