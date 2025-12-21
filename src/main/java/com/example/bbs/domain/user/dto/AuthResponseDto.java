package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@NullMarked
public class AuthResponseDto {
	private String accessToken;
	private String refreshToken;
	private UserDto user;
	private long expiresIn;
}
