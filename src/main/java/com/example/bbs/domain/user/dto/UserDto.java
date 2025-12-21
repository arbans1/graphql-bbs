package com.example.bbs.domain.user.dto;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Getter;

import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;

@Getter
@Builder
@NullMarked
public class UserDto {
	private String id;
	private String email;
	private UserProfileDto profile;
	private UserRole role;
	private OffsetDateTime createdAt;
	private @Nullable OffsetDateTime lastLoginAt;
	private UserStatus status;

}
