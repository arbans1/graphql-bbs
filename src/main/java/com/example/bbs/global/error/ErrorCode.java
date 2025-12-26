package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 에러 코드 enum.
 * GraphQL errors.graphqls의 에러 타입들과 매핑됩니다.
 */
@Getter
@RequiredArgsConstructor
@NullMarked
public enum ErrorCode {

	// 400 - 입력값 검증 실패
	BAD_USER_INPUT("BAD_USER_INPUT", "입력값이 올바르지 않습니다."),

	// 401 - 인증 실패
	UNAUTHENTICATED("UNAUTHENTICATED", "인증이 필요합니다."),
	TOKEN_EXPIRED("TOKEN_EXPIRED", "세션이 만료되었습니다."),

	// 403 - 권한 없음
	FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),

	// 404 - 리소스 없음
	NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),

	// 500 - 서버 내부 오류
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

	private final String code;
	private final String defaultMessage;
}
