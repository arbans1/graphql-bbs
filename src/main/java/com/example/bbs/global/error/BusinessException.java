package com.example.bbs.global.error;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import lombok.Getter;

/** 비즈니스 예외의 최상위 추상 클래스 */
@Getter
@NullMarked
public abstract class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	protected BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getDefaultMessage());
	}

	protected BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	// ==========================================
	// 내부 비즈니스 예외들
	// ==========================================

	/**
	 * 입력값 검증 실패 예외 (HTTP 400 대응)
	 * 스키마의 UserInputError 타입과 매핑
	 */
	@Getter
	public static class InvalidInputException extends BusinessException {
		private final List<FieldError> fieldErrors;

		public InvalidInputException(List<FieldError> fieldErrors) {
			super(ErrorCode.BAD_USER_INPUT);
			this.fieldErrors = List.copyOf(fieldErrors);
		}

		public InvalidInputException(String message, List<FieldError> fieldErrors) {
			super(ErrorCode.BAD_USER_INPUT, message);
			this.fieldErrors = List.copyOf(fieldErrors);
		}

		/** 단일 필드 에러를 위한 편의 생성자 */
		public InvalidInputException(String field, String message, FieldErrorCode fieldErrorCode) {
			this(message, List.of(FieldError.of(field, message, fieldErrorCode)));
		}
	}

	/**
	 * 권한이 없는 경우 (HTTP 403 대응)
	 * 스키마의 ForbiddenError 타입과 매핑
	 */
	public static class ForbiddenException extends BusinessException {
		public ForbiddenException(String message) {
			super(ErrorCode.FORBIDDEN, message);
		}
	}

	/**
	 * 리소스를 찾을 수 없는 경우 (HTTP 404 대응)
	 * 스키마의 NotFoundError 타입과 매핑
	 */
	@Getter
	public static class NotFoundException extends BusinessException {
		private final String resourceType;

		public NotFoundException(String resourceType, String id) {
			super(ErrorCode.NOT_FOUND, String.format("%s (ID: %s)을(를) 찾을 수 없습니다.", resourceType, id));
			this.resourceType = resourceType;
		}
	}
}
