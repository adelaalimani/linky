package com.example.linky.service;

import com.example.linky.dto.response.DailyClicksResponse;
import com.example.linky.dto.response.LinkAnalyticsResponse;
import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.User;
import com.example.linky.exception.ForbiddenException;
import com.example.linky.exception.NotFoundException;
import com.example.linky.repository.ShortUrlDailyStatsRepository;
import com.example.linky.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class LinkAnalyticsService {

    private final StringRedisTemplate redisTemplate;
    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlDailyStatsRepository dailyStatsRepository;
    private final TransactionTemplate transactionTemplate;

    private static final String DAILY_PREFIX = "daily:";
    private static final String PROCESSING_PREFIX = "daily-processing:";


    public LinkAnalyticsService(StringRedisTemplate redisTemplate, ShortUrlRepository shortUrlRepository, ShortUrlDailyStatsRepository dailyStatsRepository, TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.shortUrlRepository = shortUrlRepository;
        this.dailyStatsRepository = dailyStatsRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void recordClick(String shortKey) {
        Instant now = Instant.now();
        String today = LocalDate.ofInstant(now, ZoneOffset.UTC).toString();
        String redisKey = DAILY_PREFIX + shortKey;

        redisTemplate.opsForHash().increment(redisKey, today, 1L);

        redisTemplate.opsForHash().put(redisKey, today + ":lastAccessedAt", String.valueOf(now.toEpochMilli()));
    }

    @Scheduled(fixedDelayString = "${linky.analytics.persist-interval-ms:30000}")
    public void persistPendingClicks() {
        log.info("Linky analytics scheduler started");
        retryPendingBatches();
        Set<String> keys = redisTemplate.keys(DAILY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            String shortKey = key.substring(DAILY_PREFIX.length());
            String processingKey = PROCESSING_PREFIX + shortKey;

            try {
                Boolean moved = redisTemplate.renameIfAbsent(key, processingKey);
                if (Boolean.TRUE.equals(moved)) {
                    persistClickBatch(processingKey);
                }
            } catch (Exception ex) {
                log.error("Failed to flush clicks for {}", shortKey, ex);
            }
        }
    }

    private void retryPendingBatches() {
        Set<String> keys = redisTemplate.keys(PROCESSING_PREFIX + "*");

        if (keys == null) {
            return;
        }

        for (String key : keys) {
            try {
                persistClickBatch(key);
            } catch (Exception ex) {
                log.error("Failed to retry analytics batch {}", key, ex);
            }
        }
    }

    private void persistClickBatch(String processingKey) {
        Map<Object, Object> dailyClicks = redisTemplate.opsForHash().entries(processingKey);

        if (dailyClicks.isEmpty()) {
            return;
        }

        String shortKey = processingKey.substring(PROCESSING_PREFIX.length());

        transactionTemplate.executeWithoutResult(status -> {
            ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey)
                    .orElseThrow(() -> new NotFoundException("Short URL not found: " + shortKey));

            long totalClicks = 0;
            Instant lastAccessedAt = null;

            for (Map.Entry<Object, Object> entry : dailyClicks.entrySet()) {
                String field = entry.getKey().toString();

                if (field.endsWith(":lastAccessedAt")) {
                    continue;
                }

                LocalDate date = LocalDate.parse(field);
                long clicks = Long.parseLong(entry.getValue().toString());

                dailyStatsRepository.upsertDailyClicks(shortUrl.getId(), date, clicks);

                totalClicks += clicks;
                Object timestamp = dailyClicks.get(field + ":lastAccessedAt");

                if (timestamp != null) {
                    Instant accessedAt = Instant.ofEpochMilli(Long.parseLong(timestamp.toString()));

                    if (lastAccessedAt == null || accessedAt.isAfter(lastAccessedAt)) {
                        lastAccessedAt = accessedAt;
                    }
                }
            }
            shortUrlRepository.addClicks(shortUrl.getId(), totalClicks, lastAccessedAt);
        });

        redisTemplate.delete(processingKey);

        log.info("Persisted analytics batch for {}", shortKey);
    }

    @Transactional(readOnly = true)
    public LinkAnalyticsResponse getAnalytics(String shortKey, User user) {

        ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey).orElseThrow(() -> new NotFoundException("Short URL not found"));

        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !shortUrl.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You cannot view analytics for another user's link");
        }

        List<DailyClicksResponse> dailyClicks = dailyStatsRepository.findByShortUrlIdOrderByStatDateDesc(shortUrl.getId()).stream().map(stats -> new DailyClicksResponse(stats.getStatDate(), stats.getClicks())).toList();

        return new LinkAnalyticsResponse(shortUrl.getShortKey(), shortUrl.getUsedCount(), dailyClicks);
    }
}
