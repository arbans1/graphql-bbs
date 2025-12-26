package com.example.bbs.controller;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.GraphQLContext;
import lombok.RequiredArgsConstructor;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.LogoutSuccess;
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
		return new AuthMutation();
	}

	/**
	 * 회원가입 처리
	 *
	 * 입력값 검증은 서비스 레이어에서 수행. 비밀번호 등 민감 정보 로그 금지
	 * 회원가입 성공 시 리프레시 토큰을 쿠키로 설정
	 *
	 * @param input   회원가입 입력값
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
	 * @param input   로그인 입력값
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
	 * 로그아웃 처리
	 *
	 * 현재 사용자의 인증 정보를 제거하고 리프레시 토큰 쿠키를 삭제.
	 * 인증 요구: AUTHENTICATED 사용자만 로그아웃 가능
	 *
	 * @param context GraphQL 컨텍스트 (쿠키 삭제에 사용)
	 * @return 로그아웃 성공 응답
	 */
	@SchemaMapping(typeName = "AuthMutation", field = "logout")
	@GqlMutation
	public LogoutSuccess logout(GraphQLContext context) {
		LogoutSuccess result = authService.logout();
		// 리프레시 토큰 쿠키 삭제
		removeRefreshTokenCookie(context);
		return result;
	}

	/**
	 * 토큰 갱신 처리
	 *
	 * 컨텍스트에서 리프레시 토큰을 추출하여 새로운 액세스 토큰 발급.
	 * 리프레시 토큰이 없으면 예외 발생. 토큰 검증은 서비스 레이어에서 수행
	 *
	 * @param context GraphQL 컨텍스트 (리프레시 토큰 추출)
	 * @return 새로운 액세스 토큰을 포함한 인증 페이로드
	 *
	 */
	@SchemaMapping(typeName = "AuthMutation", field = "refreshToken")
	@GqlMutation
	public MutationResult refreshToken(GraphQLContext context) {
		// 컨텍스트에서 리프레시 토큰 추출
		String refreshToken = getRefreshTokenFromContext(context);
		// 새로운 액세스 토큰 발급
		AuthPayload payload = authService.refreshToken(refreshToken);

		return payload;
	}

	/**
	 * 리프레시 토큰을 GraphQL 컨텍스트에 저장
	 *
	 * 실제 쿠키 설정은 인터셉터에서 수행. 컨텍스트에 토큰을 보관하여 응답 헤더에 쿠키로 추가.
	 * 민감 정보 로깅을 피하기 위해 컨텍스트에만 저장하고 로그하지 않음.
	 *
	 * @param context      GraphQL 컨텍스트
	 * @param refreshToken 리프레시 토큰 문자열
	 */
	private void addRefreshTokenCookie(GraphQLContext context, String refreshToken) {
		// 리프레시 토큰을 컨텍스트에 저장 (민감 정보 로그 금지)
		context.put(JwtProperties.RefreshTokenCookie.class, new JwtProperties.RefreshTokenCookie(refreshToken));
	}

	/**
	 * 리프레시 토큰 쿠키 삭제 신호 전송
	 *
	 * 리프레시 토큰을 null로 설정하여 인터셉터에서 Max-Age=0 쿠키 삭제 신호 생성.
	 * 실제 브라우저 쿠키 삭제는 클라이언트에서 수행.
	 *
	 * @param context GraphQL 컨텍스트
	 */
	private void removeRefreshTokenCookie(GraphQLContext context) {
		// 리프레시 토큰을 null로 설정하여 쿠키 삭제 신호 전송
		context.put(JwtProperties.RefreshTokenCookie.class, new JwtProperties.RefreshTokenCookie(null));
	}

	/**
	 * GraphQL 컨텍스트에서 리프레시 토큰 문자열 추출
	 *
	 * 컨텍스트에 저장된 리프레시 토큰 쿠키 객체에서 토큰 값을 조회.
	 *
	 * @param context GraphQL 컨텍스트
	 * @return 리프레시 토큰 문자열, 없으면 null
	 */
	@Nullable
	private String getRefreshTokenFromContext(GraphQLContext context) {
		// 컨텍스트에서 리프레시 토큰 쿠키 객체 조회
		JwtProperties.RefreshTokenCookie cookie = context.get(JwtProperties.RefreshTokenCookie.class);
		// 쿠키 객체가 있으면 토큰 값 반환, 없으면 null
		return (cookie != null) ? cookie.value() : null;
	}

	public static class AuthMutation {
	}

}
