package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

import com.example.bbs.global.common.MutationResult;

@NullMarked
public record AuthPayload(
	String accessToken,
	User user,
	long expiresIn) implements MutationResult {
}
