package com.example.linky.repository;

import com.example.linky.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortKey(String shortKey);

    @Modifying
    @Query("update ShortUrl s set s.usedCount = s.usedCount + 1 where s.id = :id")
    void incrementUsedCount(@Param("id") Long id);
}
