package com.example.linky;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final int REDIS_PORT = 6379;

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
				.withExposedPorts(REDIS_PORT);
	}

	// Bucket4jConfig reads the raw spring.data.redis.* properties, which @ServiceConnection doesn't populate
	@Bean
	DynamicPropertyRegistrar redisProperties(GenericContainer<?> redisContainer) {
		return registry -> {
			registry.add("spring.data.redis.host", redisContainer::getHost);
			registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
			registry.add("spring.data.redis.username", () -> "");
			registry.add("spring.data.redis.password", () -> "");
		};
	}

}
