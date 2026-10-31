package com.adela.linky.service;

import com.adela.linky.dto.request.CreateShortUrlRequest;
import com.adela.linky.dto.response.ShortUrlResponse;
import com.adela.linky.entity.Role;
import com.adela.linky.entity.ShortUrl;
import com.adela.linky.entity.User;
import com.adela.linky.exception.ForbiddenException;
import com.adela.linky.mapper.ShortUrlMapper;
import com.adela.linky.repository.ShortUrlRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ShortUrlInsertService shortUrlInsertService;
    @Mock
    private ShortUrlCacheService shortUrlCacheService;
    @Mock
    private LinkAnalyticsService linkAnalyticsService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private ShortUrlService shortUrlService;

    private final User owner = user(1L, "CUSTOMER");
    private final User otherUser = user(2L, "CUSTOMER");
    private final User admin = user(3L, "ADMIN");

    @BeforeEach
    void setUp() {
        shortUrlService = new ShortUrlService(shortUrlRepository, shortUrlInsertService, shortUrlCacheService,
                linkAnalyticsService, cacheManager, new ShortUrlMapper(BASE_URL));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void shouldCreateShortUrlSuccessfully() {
        when(shortUrlInsertService.insert(anyString(), anyString(), anyLong()))
                .thenAnswer(inv -> shortUrl(10L, inv.getArgument(0), inv.getArgument(1), owner));

        ShortUrlResponse response = shortUrlService.create(
                new CreateShortUrlRequest("  https://example.com/page  "), owner.getId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(shortUrlInsertService).insert(keyCaptor.capture(), eq("https://example.com/page"), eq(owner.getId()));
        String generatedKey = keyCaptor.getValue();

        assertThat(generatedKey).matches("[0-9A-Za-z]{7}");
        assertThat(response.shortKey()).isEqualTo(generatedKey);
        assertThat(response.shortUrl()).isEqualTo(BASE_URL + "/" + generatedKey);
        assertThat(response.longUrl()).isEqualTo("https://example.com/page");
        assertThat(response.active()).isTrue();
        assertThat(response.usedCount()).isZero();
    }

    @Test
    void shouldRetryWhenShortKeyCollisionOccurs() {
        DataIntegrityViolationException collision = new DataIntegrityViolationException("duplicate key",
                new ConstraintViolationException("duplicate key", new SQLException("duplicate key"),
                        "uk_short_urls_short_key"));

        when(shortUrlInsertService.insert(anyString(), anyString(), anyLong()))
                .thenThrow(collision)
                .thenAnswer(inv -> shortUrl(10L, inv.getArgument(0), inv.getArgument(1), owner));

        ShortUrlResponse response = shortUrlService.create(
                new CreateShortUrlRequest("https://example.com"), owner.getId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(shortUrlInsertService, times(2)).insert(keyCaptor.capture(), eq("https://example.com"), eq(owner.getId()));
        List<String> attemptedKeys = keyCaptor.getAllValues();

        assertThat(response.shortKey()).isEqualTo(attemptedKeys.get(1));
        assertThat(attemptedKeys).allMatch(key -> key.matches("[0-9A-Za-z]{7}"));
    }

    @Test
    void shouldReturnOnlyUsersOwnLinks() {
        Pageable pageable = PageRequest.of(0, 20);
        ShortUrl ownLink = shortUrl(10L, "Abc1234", "https://example.com/own", owner);
        when(shortUrlRepository.findByUserId(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(ownLink), pageable, 1));

        Page<ShortUrlResponse> result = shortUrlService.getAll(owner, pageable);

        assertThat(result.getContent())
                .extracting(ShortUrlResponse::shortKey)
                .containsExactly("Abc1234");
        verify(shortUrlRepository).findByUserId(owner.getId(), pageable);
        verify(shortUrlRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void shouldReturnAllLinksForAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        List<ShortUrl> allLinks = List.of(
                shortUrl(10L, "Abc1234", "https://example.com/one", owner),
                shortUrl(11L, "Xyz9876", "https://example.com/two", otherUser));
        when(shortUrlRepository.findAll(pageable)).thenReturn(new PageImpl<>(allLinks, pageable, 2));

        Page<ShortUrlResponse> result = shortUrlService.getAll(admin, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ShortUrlResponse::shortKey)
                .containsExactly("Abc1234", "Xyz9876");
        verify(shortUrlRepository).findAll(pageable);
        verify(shortUrlRepository, never()).findByUserId(anyLong(), any(Pageable.class));
    }

    @Test
    void shouldDeactivateOwnedLink() {
        ShortUrl link = shortUrl(10L, "Abc1234", "https://example.com", owner);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));
        when(cacheManager.getCache("shortUrls")).thenReturn(cache);

        shortUrlService.deactivate("Abc1234", owner);

        assertThat(link.isActive()).isFalse();
        // The cache must only be evicted once the transaction has committed
        verifyNoInteractions(cache);

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(cache).evict("Abc1234");
    }

    @Test
    void shouldThrowForbiddenWhenDeactivatingAnotherUsersLink() {
        ShortUrl link = shortUrl(10L, "Abc1234", "https://example.com", owner);
        when(shortUrlRepository.findByShortKey("Abc1234")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> shortUrlService.deactivate("Abc1234", otherUser))
                .isInstanceOf(ForbiddenException.class);

        assertThat(link.isActive()).isTrue();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verifyNoInteractions(cacheManager);
    }

    private static User user(Long id, String roleName) {
        return User.builder()
                .id(id)
                .email("user" + id + "@linky.test")
                .roles(List.of(Role.builder().name(roleName).build()))
                .build();
    }

    private static ShortUrl shortUrl(Long id, String shortKey, String longUrl, User owner) {
        ShortUrl shortUrl = new ShortUrl(shortKey, longUrl, owner);
        ReflectionTestUtils.setField(shortUrl, "id", id);
        return shortUrl;
    }
}
