package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RegisterInput(
	// login Id
	String username,
	String password,
	String nickname,
	String email) {
}
