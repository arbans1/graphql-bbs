package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FieldError(
	String field,
	String message,
	String code) {

	public static FieldError of(String field, String message, FieldErrorCode errorCode) {
		return new FieldError(field, message, errorCode.code());
	}
}
