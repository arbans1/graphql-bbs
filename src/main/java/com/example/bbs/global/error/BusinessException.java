package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;

/**
 * 비즈니스 로직에서 발생하는 예외의 기본 클래스.
 * GraphQL ExceptionResolver에서 이 예외를 캐치하여 적절한 에러 응답으로 변환합니다.
 */
@NullMarked
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getDefaultMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public String getCode() {
		return errorCode.getCode();
	}
}
