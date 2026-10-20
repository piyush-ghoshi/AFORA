package com.afora.backend.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * The single injected time source required by requirement 2.10.
 *
 * Every attendance collaborator derives both kinds of value from this one bean:
 *   - "current server date"      -> LocalDate.now(clock)
 *   - "current server timestamp" -> LocalDateTime.now(clock)
 *
 * Tests override it with Clock.fixed(...), which needs no mocking at all.
 *
 * Deployment note: the relevant columns are TIMESTAMP/DATE without time zone,
 * so the JVM default zone decides what "today" means. Pin the JVM TZ to the
 * institution's zone, otherwise the 14-day activation window and the 168-hour
 * amendment window shift by up to a day at the boundary.
 */
@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}