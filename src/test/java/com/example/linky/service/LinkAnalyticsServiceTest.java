package com.example.linky.service;

import com.example.linky.dto.response.DailyClicksResponse;
import com.example.linky.dto.response.LinkAnalyticsResponse;
import com.example.linky.entity.Role;
import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.ShortUrlDailyStats;
import com.example.linky.entity.User;
import com.example.linky.exception.ForbiddenException;
import com.example.linky.exception.NotFoundException;
import com.example.linky.repository.ShortUrlDailyStatsRepository;
import com.example.linky.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkAnalyticsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ShortUrlDailyStatsRepository dailyStatsRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    private LinkAnalyticsService linkAnalyticsService;

    private final User owner = user(1L, "CUSTOMER");
    private final User otherUser = user(2L, "CUSTOMER");
    private final User admin = user(3L, "ADMIN");

    @BeforeEach
    void setUp() {
        linkAnalyticsService = new LinkAnalyticsService(redisTemplate, shortUrlRepository, dailyStatsRepository, transactionTemplate);
    }

    @Test
    void shouldReturnAnalyticsForLinkOwner() {
        ShortUrl link = shortUrl(10L, "Abc1234", owner, 7);
        LocalDate today = LocalDate.of(2026, 10, 30);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));
        when(dailyStatsRepository.findByShortUrlIdOrderByStatDateDesc(10L)).thenReturn(List.of(
                dailyStats(link, today, 4),
                dailyStats(link, today.minusDays(1), 3)));

        LinkAnalyticsResponse response = linkAnalyticsService.getAnalytics("Abc1234", owner);

        assertThat(response.shortKey()).isEqualTo("Abc1234");
        assertThat(response.totalClicks()).isEqualTo(7);
        assertThat(response.dailyClicks()).containsExactly(
                new DailyClicksResponse(today, 4),
                new DailyClicksResponse(today.minusDays(1), 3));
    }

    @Test
    void shouldAllowAdminToViewAnyLinkAnalytics() {
        ShortUrl link = shortUrl(10L, "Abc1234", owner, 2);
        LocalDate today = LocalDate.of(2026, 10, 30);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));
        when(dailyStatsRepository.findByShortUrlIdOrderByStatDateDesc(10L))
                .thenReturn(List.of(dailyStats(link, today, 2)));

        LinkAnalyticsResponse response = linkAnalyticsService.getAnalytics("Abc1234", admin);

        assertThat(response.shortKey()).isEqualTo("Abc1234");
        assertThat(response.totalClicks()).isEqualTo(2);
        assertThat(response.dailyClicks()).containsExactly(new DailyClicksResponse(today, 2));
    }

    @Test
    void shouldThrowForbiddenWhenViewingAnotherUsersAnalytics() {
        ShortUrl link = shortUrl(10L, "Abc1234", owner, 2);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Abc1234", otherUser))
                .isInstanceOf(ForbiddenException.class);

        verify(dailyStatsRepository, never()).findByShortUrlIdOrderByStatDateDesc(anyLong());
    }

    @Test
    void shouldThrowNotFoundForNonexistentLink() {
        when(shortUrlRepository.findByShortKey("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Missing", admin))
                .isInstanceOf(NotFoundException.class);

        verify(dailyStatsRepository, never()).findByShortUrlIdOrderByStatDateDesc(anyLong());
    }

    private static User user(Long id, String roleName) {
        return User.builder()
                .id(id)
                .email("user" + id + "@linky.test")
                .roles(List.of(Role.builder().name(roleName).build()))
                .build();
    }

    private static ShortUrl shortUrl(Long id, String shortKey, User owner, long usedCount) {
        ShortUrl shortUrl = new ShortUrl(shortKey, "https://example.com", owner);
        ReflectionTestUtils.setField(shortUrl, "id", id);
        ReflectionTestUtils.setField(shortUrl, "usedCount", usedCount);
        return shortUrl;
    }

    private static ShortUrlDailyStats dailyStats(ShortUrl shortUrl, LocalDate date, long clicks) {
        ShortUrlDailyStats stats = BeanUtils.instantiateClass(ShortUrlDailyStats.class);
        ReflectionTestUtils.setField(stats, "shortUrl", shortUrl);
        ReflectionTestUtils.setField(stats, "statDate", date);
        ReflectionTestUtils.setField(stats, "clicks", clicks);
        return stats;
    }
}
