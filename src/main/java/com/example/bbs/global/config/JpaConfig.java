package com.example.bbs.global.config;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@NullMarked
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class JpaConfig {

	@Bean
	DateTimeProvider dateTimeProvider() {
		return () -> Optional.of(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
	}
}
