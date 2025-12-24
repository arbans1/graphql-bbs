package com.example.bbs.global.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * JWT 기반 Spring Security 설정. CSRF 비활성화, STATELESS 세션 정책 적용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@NullMarked
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * HTTP Security 필터 체인. JWT 기반 인증, GraphQL 엔드포인트 공개 접근 허용
	 *
	 * @param http HttpSecurity
	 * @return SecurityFilterChain
	 * @throws Exception 설정 실패
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// REST API이므로 CSRF 보호 불필요
			.csrf(csrf -> csrf.disable())
			// JWT 사용, 폼 로그인 미사용
			.formLogin(form -> form.disable())
			// JWT 사용, HTTP Basic 인증 미사용
			.httpBasic(basic -> basic.disable())
			// JWT 기반 무상태 인증
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// GraphQL 엔드포인트 및 GraphiQL UI 누구나 접근 가능 (메서드 레벨에서 권한 검증)
				.requestMatchers("/graphql", "/graphiql/**").permitAll()
				// 그 외 모든 요청은 인증 필수
				.anyRequest().authenticated())
			// JWT 필터 우선 실행
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * BCrypt 기반 비밀번호 인코더 빈
	 *
	 * @return PasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
