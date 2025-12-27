package com.example.bbs.controller;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.service.UserService;
import com.example.bbs.global.error.BusinessException;

@Controller
@NullMarked
@RequiredArgsConstructor
public class UserQueryController {
	private final UserService userService;

	/**
	 * 사용자 조회용 GraphQL Query 컨트롤러
	 *
	 * 현재 로그인 사용자 조회와 특정 ID 조회 기능 제공
	 */
	@QueryMapping
	public User me(@AuthenticationPrincipal String userId) {
		return userService.findById(userId);
	}

	/**
	 * ID로 사용자 프로필 조회
	 *
	 * 존재하지 않을 경우 null 반환 (NotFound 예외 내부 처리)
	 *
	 * @param id 조회할 사용자 ID
	 * @return 사용자 정보 또는 null
	 */
	@QueryMapping
	@Nullable
	public User user(@Argument String id) {
		try {
			return userService.findById(id);
		} catch (BusinessException.NotFoundException e) {
			return null;
		}
	}
}
