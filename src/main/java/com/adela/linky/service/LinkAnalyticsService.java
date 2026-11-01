package com.adela.linky.service;

import com.adela.linky.dto.response.DailyClicksResponse;
import com.adela.linky.dto.response.LinkAnalyticsResponse;
import com.adela.linky.entity.ShortUrl;
import com.adela.linky.security.AuthUser;
import com.adela.linky.exception.ForbiddenException;
import com.adela.linky.exception.NotFoundException;
import com.adela.linky.repository.ShortUrlDailyStatsRepository;
import com.adela.linky.repository.ShortUrlRepository;
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

/**
 * Counts clicks in Redis on each redirect and saves them to Postgres in batches.
 * The batch runs every 30 seconds only to make testing faster.
 */
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

    /**
     * Adds one click for today to the link's Redis hash (daily:shortKey) and stores the time of the click.
     */
    public void recordClick(String shortKey) {
        Instant now = Instant.now();
        String today = LocalDate.ofInstant(now, ZoneOffset.UTC).toString();
        String redisKey = DAILY_PREFIX + shortKey;

        redisTemplate.opsForHash().increment(redisKey, today, 1L);
        redisTemplate.opsForHash().put(redisKey, today + ":lastAccessedAt", String.valueOf(now.toEpochMilli()));
    }

    /**
     * Scheduled job: renames each daily: key to daily-processing: so new clicks go to a fresh key, then saves it.
     * Batches left over from a failed run are retried first.
     */
    @Scheduled(fixedDelayString = "${linky.analytics.persist-interval-ms:30000}")
    public void persistPendingClicks() {
        log.debug("Linky analytics scheduler started");
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

    /**
     * Saves daily-processing: batches that a previous run didn't finish.
     */
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

    /**
     * In one transaction, adds the batch's clicks per day and to the link's total, then deletes the batch from Redis.
     */
    private void persistClickBatch(String processingKey) {
        Map<Object, Object> dailyClicks = redisTemplate.opsForHash().entries(processingKey);
        if (dailyClicks.isEmpty()) {
            return;
        }
        log.info("Processing analytics batch {}", processingKey);
        String shortKey = processingKey.substring(PROCESSING_PREFIX.length());
        Long savedClicks = transactionTemplate.execute(status -> {
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
            return totalClicks;
        });
        redisTemplate.delete(processingKey);
        log.info("Saved {} clicks for {}", savedClicks, shortKey);
    }

    /**
     * Returns total and daily clicks for a link, only its owner or an admin may see them.
     */
    @Transactional(readOnly = true)
    public LinkAnalyticsResponse getAnalytics(String shortKey, AuthUser user) {
        ShortUrl shortUrl = shortUrlRepository
                .findByShortKey(shortKey)
                .orElseThrow(() -> new NotFoundException("Short URL not found"));

        if (!user.isAdmin() && !shortUrl.getUser().getId().equals(user.id())) {
            throw new ForbiddenException("You are not authorized for this action");
        }
        List<DailyClicksResponse> dailyClicks = dailyStatsRepository
                .findByShortUrlIdOrderByStatDateDesc(shortUrl.getId())
                .stream()
                .map(stats ->
                        new DailyClicksResponse(stats.getStatDate(), stats.getClicks())).toList();
        log.info("User with id {} fetched analytics for {}", user.id(), shortKey);
        return new LinkAnalyticsResponse(shortUrl.getShortKey(), shortUrl.getUsedCount(), dailyClicks);
    }
}
