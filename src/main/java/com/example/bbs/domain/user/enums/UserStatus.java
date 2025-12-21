package com.example.bbs.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 상태를 표현하는 값 객체
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus {

	/** 활성 상태: 정상 이용 가능 */
	ACTIVE("활성"),

	/** 일시 정지 상태: 제한적 또는 일시적 이용 제한 */
	SUSPENDED("정지"),

	/** 영구 차단 상태: 접근 불가 */
	BANNED("차단"),

	/** 탈퇴 처리 상태: 더 이상 서비스 이용 불가 */
	DELETED("탈퇴");

	private final String description;
}
