package com.adela.linky.service;

import com.adela.linky.security.AuthUser;
import com.adela.linky.dto.response.DailyClicksResponse;
import com.adela.linky.dto.response.LinkAnalyticsResponse;
import com.adela.linky.entity.Role;
import com.adela.linky.entity.ShortUrl;
import com.adela.linky.entity.ShortUrlDailyStats;
import com.adela.linky.entity.User;
import com.adela.linky.exception.BadRequestException;
import com.adela.linky.exception.ForbiddenException;
import com.adela.linky.exception.NotFoundException;
import com.adela.linky.repository.ShortUrlDailyStatsRepository;
import com.adela.linky.repository.ShortUrlRepository;
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
import static org.mockito.ArgumentMatchers.any;
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
        when(dailyStatsRepository.findByShortUrlIdAndStatDateBetweenOrderByStatDateDesc(10L, today.minusDays(29), today)).thenReturn(List.of(
                dailyStats(link, today, 4),
                dailyStats(link, today.minusDays(1), 3)));

        LinkAnalyticsResponse response = linkAnalyticsService.getAnalytics("Abc1234", AuthUser.from(owner), null, today);

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
        when(dailyStatsRepository.findByShortUrlIdAndStatDateBetweenOrderByStatDateDesc(10L, today.minusDays(29), today))
                .thenReturn(List.of(dailyStats(link, today, 2)));

        LinkAnalyticsResponse response = linkAnalyticsService.getAnalytics("Abc1234", AuthUser.from(admin), null, today);

        assertThat(response.shortKey()).isEqualTo("Abc1234");
        assertThat(response.totalClicks()).isEqualTo(2);
        assertThat(response.dailyClicks()).containsExactly(new DailyClicksResponse(today, 2));
    }

    @Test
    void shouldThrowForbiddenWhenViewingAnotherUsersAnalytics() {
        ShortUrl link = shortUrl(10L, "Abc1234", owner, 2);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Abc1234", AuthUser.from(otherUser), null, null))
                .isInstanceOf(ForbiddenException.class);

        verify(dailyStatsRepository, never()).findByShortUrlIdAndStatDateBetweenOrderByStatDateDesc(anyLong(), any(), any());
    }

    @Test
    void shouldThrowNotFoundForNonexistentLink() {
        when(shortUrlRepository.findByShortKey("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Missing", AuthUser.from(admin), null, null))
                .isInstanceOf(NotFoundException.class);

        verify(dailyStatsRepository, never()).findByShortUrlIdAndStatDateBetweenOrderByStatDateDesc(anyLong(), any(), any());
    }

    @Test
    void shouldRejectFromAfterTo() {
        LocalDate today = LocalDate.of(2026, 10, 30);

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Abc1234", AuthUser.from(owner), today, today.minusDays(1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectRangeLongerThan365Days() {
        LocalDate today = LocalDate.of(2026, 10, 30);

        assertThatThrownBy(() -> linkAnalyticsService.getAnalytics("Abc1234", AuthUser.from(owner), today.minusDays(365), today))
                .isInstanceOf(BadRequestException.class);
    }

    private static User user(Long id, String roleName) {
        return User.builder()
                .id(id)
                .email("user" + id + "@linky.test")
                .roles(List.of(new Role(null, roleName)))
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
