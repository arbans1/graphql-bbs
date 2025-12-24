package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@NullMarked
public class LoginInput {
	private final String username;
	private final String password;
}
