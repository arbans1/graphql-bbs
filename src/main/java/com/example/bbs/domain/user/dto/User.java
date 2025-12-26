package com.example.bbs.domain.user.dto;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.global.common.Ownable;

@Getter
@Builder
@ToString(exclude = "profile")
@NullMarked
public class User implements Ownable {
	private String id;
	private String email;
	private UserProfile profile;
	private UserRole role;
	private OffsetDateTime createdAt;
	private @Nullable OffsetDateTime lastLoginAt;
	private UserStatus status;

	@Override
	public String getOwnerId() {
		return this.id;
	}
}
