package com.example.bbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// 1. CSRF 보호 비활성화
			.csrf(csrf -> csrf.disable())
			// 2. 경로별 권한 설정
			.authorizeHttpRequests(auth -> auth
				// 모든 요청에 대해 인증 없이 허용
				.anyRequest().permitAll())
			// 3. 기본 로그인 페이지 비활성화
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable());

		return http.build();
	}
}
