package com.example.bbs.domain.user.policy;

import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PasswordPolicy {

	// 영어와 숫자가 적어도 하나씩 포함되어야 하며, 8자리 이상 (특수문자 허용)
	private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\W_]{8,}$";
	private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

	private PasswordPolicy() {
		// 인스턴스화 방지
	}

	/**
	 * 비밀번호 기본 형식 검증 (영어+숫자 조합, 8자 이상)
	 */
	public static boolean isFormatValid(String password) {
		return PASSWORD_PATTERN.matcher(password).matches();
	}

	/**
	 * 비밀번호에 아이디가 포함되어 있는지 확인 (보안 강화)
	 */
	public static boolean containsLoginId(String password, String loginId) {
		return password.contains(loginId);
	}

	/**
	 * 반복되는 문자 체크 (예: aaaa, 1111 등 4회 이상 반복 방지)
	 */
	public static boolean hasRepeatedChars(String password) {
		for (int i = 0; i <= password.length() - 4; i++) {
			if (password.charAt(i) == password.charAt(i + 1)
				&& password.charAt(i + 1) == password.charAt(i + 2)
				&& password.charAt(i + 2) == password.charAt(i + 3)) {
				return true;
			}
		}
		return false;
	}
}
