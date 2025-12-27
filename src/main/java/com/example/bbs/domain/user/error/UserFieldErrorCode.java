package com.example.bbs.domain.user.error;

import org.jspecify.annotations.NullMarked;

import com.example.bbs.global.error.FieldErrorCode;

@NullMarked
public enum UserFieldErrorCode implements FieldErrorCode {
	// 아이디 및 닉네임 관련
	INVALID_USERNAME_FORMAT("INVALID_USERNAME_FORMAT"),
	RESERVED_USERNAME("RESERVED_USERNAME"),
	INVALID_NICKNAME_FORMAT("INVALID_NICKNAME_FORMAT"),
	RESERVED_NICKNAME("RESERVED_NICKNAME"),

	// 비밀번호 관련 (추가)
	INVALID_PASSWORD_FORMAT("INVALID_PASSWORD_FORMAT"), // 8자 미만 또는 영문/숫자 미조합
	PASSWORD_CONTAINS_USERNAME("PASSWORD_CONTAINS_USERNAME"), // 비밀번호에 아이디 포함
	WEAK_PASSWORD("WEAK_PASSWORD"), // 연속/반복 문자 등 보안 취약

	// 공통
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
