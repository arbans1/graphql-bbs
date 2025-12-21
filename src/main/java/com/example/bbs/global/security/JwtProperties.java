package com.example.bbs.global.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@NullMarked
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	/** 액세스 토큰 비밀 키. */
	private final String accessSecret;

	/** 리프레시 토큰 비밀 키. */
	private final String refreshSecret;

	/** 액세스 토큰 만료 시간 (밀리초). */
	private final long accessExpiration;

	/** 리프레시 토큰 만료 시간 (밀리초). */
	private final long refreshExpiration;

}
