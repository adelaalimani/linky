package com.example.linky.integration;

import com.example.linky.AbstractIntegrationTest;
import com.example.linky.dto.response.DailyClicksResponse;
import com.example.linky.dto.response.LinkAnalyticsResponse;
import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.User;
import com.example.linky.repository.ShortUrlRepository;
import com.example.linky.service.LinkAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkAnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final String SHORT_KEY = "Abc1234";
    private static final String DAILY_KEY = "daily:" + SHORT_KEY;
    private static final String PROCESSING_KEY = "daily-processing:" + SHORT_KEY;

    @Autowired
    private LinkAnalyticsService linkAnalyticsService;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    private User owner;
    private ShortUrl shortUrl;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        // Guard: a background persistPendingClicks run would race the explicit calls below
        assertThat(applicationContext.getBeansOfType(ScheduledTaskHolder.class)).isEmpty();

        owner = createCustomer();
        shortUrl = createShortUrl(SHORT_KEY, "https://example.com", owner);
    }

    @Test
    void shouldPersistRedisClicksToPostgres() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/" + SHORT_KEY)).andExpect(status().isFound());
        }
        // Read the day the clicks were recorded under rather than recomputing it, so a run across midnight can't fail
        Map<Object, Object> pending = redisTemplate.opsForHash().entries(DAILY_KEY);
        Map<Object, Object> clicksByDay = pending.entrySet().stream()
                .filter(entry -> !entry.getKey().toString().endsWith(":lastAccessedAt"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(clicksByDay).hasSize(1).containsValue("3");
        LocalDate clickDay = LocalDate.parse(clicksByDay.keySet().iterator().next().toString());

        linkAnalyticsService.persistPendingClicks();

        assertThat(usedCount()).isEqualTo(3L);
        assertThat(dailyClicks(clickDay)).isEqualTo(3L);
        // Both the live counter and the processing batch must be gone once the batch is flushed
        assertThat(redisTemplate.hasKey(DAILY_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(PROCESSING_KEY)).isFalse();
    }

    @Test
    void shouldUpdateLastAccessedAt() {
        assertThat(lastAccessedAt()).isNull();

        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        linkAnalyticsService.recordClick(SHORT_KEY);
        Instant after = Instant.now();

        linkAnalyticsService.persistPendingClicks();

        assertThat(lastAccessedAt()).isBetween(before, after);
    }

    @Test
    void shouldAggregateDailyClicksCorrectly() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);

        redisTemplate.opsForHash().putAll(DAILY_KEY, Map.of(
                yesterday.toString(), "2",
                today.toString(), "5"));
        linkAnalyticsService.persistPendingClicks();

        // A second batch for the same day must be added to the existing row, not replace it
        redisTemplate.opsForHash().put(DAILY_KEY, today.toString(), "3");
        linkAnalyticsService.persistPendingClicks();

        assertThat(dailyClicks(yesterday)).isEqualTo(2L);
        assertThat(dailyClicks(today)).isEqualTo(8L);
        assertThat(usedCount()).isEqualTo(10L);

        LinkAnalyticsResponse analytics = linkAnalyticsService.getAnalytics(SHORT_KEY, owner);
        assertThat(analytics.totalClicks()).isEqualTo(10L);
        assertThat(analytics.dailyClicks()).containsExactly(
                new DailyClicksResponse(today, 8),
                new DailyClicksResponse(yesterday, 2));
    }

    private long usedCount() {
        return shortUrlRepository.findById(shortUrl.getId()).orElseThrow().getUsedCount();
    }

    private Instant lastAccessedAt() {
        return shortUrlRepository.findById(shortUrl.getId()).orElseThrow().getLastAccessedAt();
    }

    private Long dailyClicks(LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT clicks FROM linky.short_url_daily_stats WHERE short_url_id = ? AND stat_date = ?",
                Long.class, shortUrl.getId(), date);
    }
}
