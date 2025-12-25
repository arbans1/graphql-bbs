package com.example.bbs.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.GraphQLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.service.AuthService;
import com.example.bbs.global.aop.GqlMutation;
import com.example.bbs.global.common.MutationResult;
import com.example.bbs.global.security.JwtProperties;

/**
 * 인증 관련 GraphQL Mutation 컨트롤러
 *
 * - `auth` 루트 타입 제공
 * - 회원가입, 로그인 처리 후 리프레시 토큰 쿠키 설정
 * - 민감 정보 로그 출력 금지. 사용자 비밀번호 등 기록 금지
 */
@Slf4j
@NullMarked
@Controller
@RequiredArgsConstructor
public class AuthMutationController {

	private final AuthService authService;

	/**
	 * 인증 루트 Mutation 반환
	 *
	 * @return 인증 루트 타입 `AuthMutation`
	 */
	@MutationMapping
	public AuthMutation auth() {
		log.debug("Auth mutation called");
		return new AuthMutation();
	}

	/**
	 * 회원가입 처리
	 *
	 * 입력값 검증은 서비스 레이어에서 수행. 비밀번호 등 민감 정보 로그 금지
	 * 회원가입 성공 시 리프레시 토큰을 쿠키로 설정
	 *
	 * @param input 회원가입 입력값
	 * @param context GraphQL 컨텍스트 (쿠키 설정에 사용)
	 * @return 클라이언트로 반환할 Mutation 결과 페이로드
	 */
	@SchemaMapping(typeName = "AuthMutation", field = "register")
	@GqlMutation
	public MutationResult register(@Argument RegisterInput input, GraphQLContext context) {
		AuthTokens tokens = authService.register(input);
		// 리프레시 토큰을 컨텍스트에 저장하여 인터셉터에서 쿠키로 변환
		addRefreshTokenCookie(context, tokens.refreshToken());
		return tokens.payload();
	}

	/**
	 * 로그인 처리
	 *
	 * 성공 시 리프레시 토큰을 쿠키로 설정. 사용자 인증 실패 시 예외는 서비스 레이어에서 발생
	 *
	 * @param input 로그인 입력값
	 * @param context GraphQL 컨텍스트 (쿠키 설정에 사용)
	 * @return 클라이언트로 반환할 Mutation 결과 페이로드
	 */
	@SchemaMapping(typeName = "AuthMutation", field = "login")
	@GqlMutation
	public MutationResult login(@Argument LoginInput input, GraphQLContext context) {
		AuthTokens tokens = authService.login(input);
		// 리프레시 토큰을 컨텍스트에 저장하여 인터셉터에서 쿠키로 변환
		addRefreshTokenCookie(context, tokens.refreshToken());
		return tokens.payload();
	}

	/**
	 * 리프레시 토큰을 GraphQL 컨텍스트에 저장
	 *
	 * 실제 쿠키 설정은 인터셉터에서 수행. 컨텍스트에 토큰을 보관하여 응답 헤더에 쿠키 추가
	 *
	 * @param context GraphQL 컨텍스트
	 * @param refreshToken 리프레시 토큰 문자열
	 */
	private void addRefreshTokenCookie(GraphQLContext context, String refreshToken) {
		// 민감 정보 직접 로그 금지. 컨텍스트에만 저장
		context.put(JwtProperties.RefreshTokenCookie.class, new JwtProperties.RefreshTokenCookie(refreshToken));
	}

	public static class AuthMutation {
	}
}
