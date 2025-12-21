package com.example.bbs.global.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

/**
 * JWT 토큰 검증 필터. 요청 헤더에서 토큰 추출 후 Authentication 설정
 */
@Component
@RequiredArgsConstructor
@NullMarked
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider tokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String token = extractTokenFromRequest(request); // Authorization 헤더에서 토큰 추출
		boolean isAccessToken = true;
		if (token != null && tokenProvider.validateToken(token, isAccessToken)) { // 토큰 유효성 검증
			Authentication auth = tokenProvider.getAuthentication(token); // 토큰에서 인증 정보 추출
			SecurityContextHolder.getContext().setAuthentication(auth); // SecurityContext에 인증 정보 저장
		}
		filterChain.doFilter(request, response); // 다음 필터로 요청 전달
	}

	/**
	 * Authorization 헤더에서 Bearer 토큰 추출
	 *
	 * @param request HttpServletRequest
	 * @return Bearer 토큰, 없으면 null
	 */
	@Nullable
	private String extractTokenFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		String tokenPrefix = "Bearer ";
		if (bearerToken != null && bearerToken.startsWith(tokenPrefix)) {
			return bearerToken.substring(tokenPrefix.length());
		}
		return null;
	}
}
