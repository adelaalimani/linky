package com.adela.linky.integration;

import com.adela.linky.AbstractIntegrationTest;
import com.adela.linky.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String SHORT_KEY = "Abc1234";

    private User owner;

    @BeforeEach
    void setUp() {
        owner = createCustomer();
        createShortUrl(SHORT_KEY, "https://example.com", owner);
    }

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/short-urls"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/short-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl": "https://example.com"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/short-urls/{shortKey}/deactivate", SHORT_KEY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/links/{shortKey}/analytics", SHORT_KEY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/links/{shortKey}/analytics", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserAccessToAnotherUsersLink() throws Exception {
        User otherUser = createCustomer();

        mockMvc.perform(get("/api/v1/links/{shortKey}/analytics", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/short-urls/{shortKey}/deactivate", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isForbidden());

        // The other user's link list must not leak the owner's link
        mockMvc.perform(get("/api/v1/short-urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldAllowAdminAccessToAnyLinkAnalytics() throws Exception {
        User admin = createAdmin();

        mockMvc.perform(get("/api/v1/links/{shortKey}/analytics", SHORT_KEY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortKey").value(SHORT_KEY))
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.dailyClicks").isEmpty());
    }
}
