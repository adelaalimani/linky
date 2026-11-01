package com.adela.linky.integration;

import com.adela.linky.AbstractIntegrationTest;
import com.adela.linky.entity.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortUrlIntegrationTest extends AbstractIntegrationTest {

    private static final String SHORT_KEY = "Abc1234";
    private static final String LONG_URL = "https://example.com/landing";

    @Autowired
    private CacheManager cacheManager;

    private Cache shortUrlCache;
    private User owner;

    @BeforeEach
    void setUp() {
        shortUrlCache = cacheManager.getCache("shortUrls");
        owner = createCustomer();
    }

    @Test
    void shouldPersistShortUrlInPostgres() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/short-urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl": "%s"}
                                """.formatted(LONG_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortKey").isString())
                .andExpect(jsonPath("$.longUrl").value(LONG_URL))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        String shortKey = JsonPath.read(responseBody, "$.shortKey");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT long_url, active, used_count, user_id, created_at FROM linky.short_urls WHERE short_key = ?",
                shortKey);
        assertThat(row.get("long_url")).isEqualTo(LONG_URL);
        assertThat(row.get("active")).isEqualTo(true);
        assertThat(row.get("used_count")).isEqualTo(0L);
        assertThat(row.get("user_id")).isEqualTo(owner.getId());
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void shouldCacheShortUrlAfterFirstRedirect() throws Exception {
        createShortUrl(SHORT_KEY, LONG_URL, owner);
        assertThat(shortUrlCache.get(SHORT_KEY)).isNull();

        mockMvc.perform(get("/" + SHORT_KEY))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, LONG_URL));

        assertThat(shortUrlCache.get(SHORT_KEY, String.class)).isEqualTo(LONG_URL);

        // Change the row behind the cache's back: the next redirect must still be served from Redis
        jdbcTemplate.update("UPDATE linky.short_urls SET long_url = ? WHERE short_key = ?",
                "https://example.com/changed", SHORT_KEY);

        mockMvc.perform(get("/" + SHORT_KEY))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, LONG_URL));
    }

    @Test
    void shouldInvalidateCacheWhenLinkIsDeactivated() throws Exception {
        createShortUrl(SHORT_KEY, LONG_URL, owner);
        mockMvc.perform(get("/" + SHORT_KEY)).andExpect(status().isFound());
        assertThat(shortUrlCache.get(SHORT_KEY)).isNotNull();

        mockMvc.perform(patch("/api/v1/short-urls/{shortKey}/deactivate", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        assertThat(shortUrlCache.get(SHORT_KEY)).isNull();
        Boolean active = jdbcTemplate.queryForObject(
                "SELECT active FROM linky.short_urls WHERE short_key = ?", Boolean.class, SHORT_KEY);
        assertThat(active).isFalse();
    }

    @Test
    void shouldNotRedirectDeactivatedLink() throws Exception {
        createShortUrl(SHORT_KEY, LONG_URL, owner);
        // Warm the cache first, so this also proves a stale cache entry can't keep the link alive
        mockMvc.perform(get("/" + SHORT_KEY)).andExpect(status().isFound());

        mockMvc.perform(patch("/api/v1/short-urls/{shortKey}/deactivate", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + SHORT_KEY))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void shouldRejectShortKeyWithWrongLength() throws Exception {
        mockMvc.perform(get("/sss"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Short URL key is not valid"));
    }

    @Test
    void shouldReturnNotFoundForUnknownPath() throws Exception {
        mockMvc.perform(get("/no/such/path")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("The requested resource was not found"));
    }
}
