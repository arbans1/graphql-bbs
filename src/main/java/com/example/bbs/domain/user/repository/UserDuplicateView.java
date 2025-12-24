package com.example.bbs.domain.user.repository;

/**
 * 중복 체크 시 필요한 필드만 조회하기 위한 프로젝션
 */
public interface UserDuplicateView {
	String getLoginId();

	String getEmail();

	String getNickname();
}
