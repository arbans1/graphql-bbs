package com.example.bbs.global.error;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * 입력값 검증 실패 시 발생하는 예외. (HTTP 400 대응)
 * 여러 필드의 에러를 한 번에 담을 수 있습니다.
 */
@NullMarked
public class InvalidInputException extends BusinessException {

	private final List<FieldErrorDto> fieldErrors;

	public InvalidInputException() {
		super(ErrorCode.BAD_USER_INPUT);
		this.fieldErrors = new ArrayList<>();
	}

	public InvalidInputException(String message) {
		super(ErrorCode.BAD_USER_INPUT, message);
		this.fieldErrors = new ArrayList<>();
	}

	public InvalidInputException(String field, String message) {
		super(ErrorCode.BAD_USER_INPUT, message);
		this.fieldErrors = List.of(new FieldErrorDto(field, message, null));
	}

	public InvalidInputException(List<FieldErrorDto> fieldErrors) {
		super(ErrorCode.BAD_USER_INPUT);
		this.fieldErrors = fieldErrors;
	}

	public List<FieldErrorDto> getFieldErrors() {
		return fieldErrors;
	}
}
