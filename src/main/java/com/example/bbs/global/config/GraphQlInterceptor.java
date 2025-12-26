package com.example.bbs.global.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import com.example.bbs.global.security.JwtProperties;
import com.example.bbs.global.security.JwtTokenProvider;

/**
 * GraphQL 응답에 리프레시 토큰 쿠키를 추가하는 인터셉터
 *
 * - Resolver가 GraphQL 컨텍스트에 `JwtProperties.RefreshTokenCookie`를 저장하면, 응답 헤더에 쿠키 추가
 * - 민감 정보(토큰 원문) 로그 출력 금지
 * - 보안 설정: HttpOnly, Secure, SameSite 등은 `JwtTokenProvider`에서 일원화 관리
 */
@Component
@NullMarked
@RequiredArgsConstructor
public class GraphQlInterceptor implements WebGraphQlInterceptor {
	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
		// 체인을 통해 생성된 응답에 쿠키를 부착
		return chain.next(request).map(this::attachRefreshTokenCookieIfPresent);
	}

	/**
	 * GraphQL 컨텍스트에 저장된 리프레시 토큰 존재 시 응답 헤더에 쿠키 추가
	 *
	 * 토큰이 null이면 쿠키 삭제 신호 (Max-Age=0)를 전송
	 *
	 * @param response GraphQL 응답 객체
	 * @return 쿠키가 필요 시 추가된 응답 객체
	 */
	private WebGraphQlResponse attachRefreshTokenCookieIfPresent(WebGraphQlResponse response) {
		JwtProperties.RefreshTokenCookie refreshTokenCookie = response.getExecutionInput()
			.getGraphQLContext()
			.get(JwtProperties.RefreshTokenCookie.class);

		if (refreshTokenCookie == null) {
			return response;
		}

		// 토큰이 null이면 쿠키 삭제 (Max-Age=0), 토큰이 있으면 쿠키 생성
		ResponseCookie cookie = (refreshTokenCookie.value() == null)
			? jwtTokenProvider.createDeleteRefreshTokenCookie()
			: jwtTokenProvider.createRefreshTokenCookie(refreshTokenCookie.value());

		response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
		return response;
	}

}
