package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AuthTokens(
	AuthPayload payload,
	String refreshToken) {
}
