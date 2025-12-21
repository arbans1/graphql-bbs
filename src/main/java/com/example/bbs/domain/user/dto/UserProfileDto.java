package com.example.bbs.domain.user.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@NullMarked
public class UserProfileDto {
	private String nickname;
	private @Nullable String imageUrl;

}
