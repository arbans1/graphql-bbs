package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

import lombok.Builder;
import lombok.Getter;

import com.example.bbs.global.common.MutationResult;

@Getter
@Builder
@NullMarked
public class AuthPayload implements MutationResult {
	private String accessToken;
	private String refreshToken;
	private User user;
	private long expiresIn;
}
