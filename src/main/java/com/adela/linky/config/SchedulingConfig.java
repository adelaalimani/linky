package com.adela.linky.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled jobs. Turned off with linky.scheduling.enabled=false, so tests can run the jobs themselves.
 */
@Configuration
@EnableScheduling
@ConditionalOnBooleanProperty(name = "linky.scheduling.enabled", matchIfMissing = true)
public class SchedulingConfig {
}
