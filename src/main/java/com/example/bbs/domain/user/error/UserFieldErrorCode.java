package com.example.bbs.domain.user.error;

import org.jspecify.annotations.NullMarked;

import com.example.bbs.global.error.FieldErrorCode;

@NullMarked
public enum UserFieldErrorCode implements FieldErrorCode {
	INVALID_USERNAME_FORMAT("INVALID_USERNAME_FORMAT"),
	RESERVED_USERNAME("RESERVED_USERNAME"),
	INVALID_NICKNAME_FORMAT("INVALID_NICKNAME_FORMAT"),
	RESERVED_NICKNAME("RESERVED_NICKNAME"),
	DUPLICATE("DUPLICATE");

	private final String code;

	UserFieldErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String code() {
		return code;
	}
}
