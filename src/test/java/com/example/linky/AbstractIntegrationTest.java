package com.example.linky;

import com.example.linky.entity.Role;
import com.example.linky.entity.ShortUrl;
import com.example.linky.entity.User;
import com.example.linky.repository.RoleRepository;
import com.example.linky.repository.UserRepository;
import com.example.linky.security.jwt.JwtService;
import com.example.linky.service.ShortUrlInsertService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Scheduled jobs are off so tests can call them explicitly without racing a background run
@SpringBootTest(properties = "linky.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected ShortUrlInsertService shortUrlInsertService;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetState() {
        // Roles are seeded by Liquibase, so keep them and wipe everything else
        jdbcTemplate.execute("""
                TRUNCATE linky.short_url_daily_stats, linky.short_urls, linky.users_roles, linky.users
                RESTART IDENTITY CASCADE
                """);
        // Clears the shortUrls cache, pending click counters and rate limit buckets
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
    }

    protected User createUser(String roleName) {
        // Users cascade PERSIST to roles, so the role must be managed in the same transaction as the save
        return transactionTemplate.execute(status -> saveUser(roleName));
    }

    private User saveUser(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not seeded"));
        int n = USER_SEQUENCE.incrementAndGet();
        User user = User.builder()
                .firstName("Test")
                .lastName("User" + n)
                .email("user" + n + "@linky.test")
                .phoneNumber("+3551000" + String.format("%05d", n))
                .password(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(List.of(role))
                .build();
        return userRepository.save(user);
    }

    protected User createCustomer() {
        return createUser("CUSTOMER");
    }

    protected User createAdmin() {
        return createUser("ADMIN");
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getUsername());
    }

    protected ShortUrl createShortUrl(String shortKey, String longUrl, User owner) {
        return shortUrlInsertService.insert(shortKey, longUrl, owner.getId());
    }
}
