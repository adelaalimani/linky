package com.example.linky;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LinkyApplicationTests {

	@Autowired
	private ScheduledTaskHolder scheduledTaskHolder;

	@Test
	void contextLoads() {
	}

	@Test
	void analyticsJobIsScheduledByDefault() {
		assertThat(scheduledTaskHolder.getScheduledTasks())
				.anySatisfy(scheduled -> {
					assertThat(scheduled.toString()).endsWith("LinkAnalyticsService.persistPendingClicks");
					assertThat(scheduled.getTask()).isInstanceOfSatisfying(FixedDelayTask.class,
							task -> assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofSeconds(30)));
				});
	}

}
