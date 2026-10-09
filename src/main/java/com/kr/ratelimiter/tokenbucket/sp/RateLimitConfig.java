package com.kr.ratelimiter.tokenbucket.sp;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RateLimitConfig {

	// Spring Boot does not provide a Clock bean. Tests pass their own fake one.
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
