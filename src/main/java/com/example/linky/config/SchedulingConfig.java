package com.example.linky.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Enabled unless linky.scheduling.enabled=false, so tests can trigger scheduled jobs explicitly
@Configuration
@EnableScheduling
@ConditionalOnBooleanProperty(name = "linky.scheduling.enabled", matchIfMissing = true)
public class SchedulingConfig {
}
