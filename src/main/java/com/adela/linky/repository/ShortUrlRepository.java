package com.adela.linky.repository;

import com.adela.linky.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortKey(String shortKey);

    Page<ShortUrl> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query(value = """
        UPDATE linky.short_urls
        SET used_count = used_count + :clicks,
            last_accessed_at = GREATEST(
                last_accessed_at,
                CAST(:lastAccessedAt AS TIMESTAMP WITH TIME ZONE)
            )
        WHERE id = :id
        """, nativeQuery = true)
    int addClicks(
            @Param("id") Long id,
            @Param("clicks") long clicks,
            @Param("lastAccessedAt") Instant lastAccessedAt
    );
}
