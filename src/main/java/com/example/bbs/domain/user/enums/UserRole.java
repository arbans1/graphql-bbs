package com.example.bbs.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 내에서 사용자의 권한을 정의한다
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

	/** 운영자 권한. 시스템 전역 관리 기능에 접근 가능 */
	ADMIN("운영자"),

	/** 일반 회원 권한. 기본 CRUD 범위 내에서 활동 */
	MEMBER("회원"),

	/** 비회원/게스트 권한. 제한된 읽기 전용 접근만 허용 */
	GUEST("비회원");

	private final String description;
}
