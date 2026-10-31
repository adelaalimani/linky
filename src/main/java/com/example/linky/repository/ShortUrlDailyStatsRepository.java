package com.example.linky.repository;

import com.example.linky.entity.ShortUrlDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShortUrlDailyStatsRepository extends JpaRepository<ShortUrlDailyStats, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO linky.short_url_daily_stats
                (short_url_id, stat_date, clicks)
            VALUES (:shortUrlId, :date, :clicks)
            ON CONFLICT (short_url_id, stat_date)
            DO UPDATE SET
                clicks = short_url_daily_stats.clicks + EXCLUDED.clicks
            """, nativeQuery = true)
    int upsertDailyClicks(@Param("shortUrlId") Long shortUrlId, @Param("date") LocalDate date, @Param("clicks") long clicks);

    List<ShortUrlDailyStats> findByShortUrlIdOrderByStatDateDesc(Long shortUrlId);
}
