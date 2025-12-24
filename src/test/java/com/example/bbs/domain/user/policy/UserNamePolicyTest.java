package com.example.bbs.domain.user.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserNamePolicyTest {

	@Test
	@DisplayName("유효한 아이디 형식 검증 - 영문, 숫자, 특수문자(._-) 조합")
	void isUsernameFormatValid_Success() {
		// given
		String validUsername = "test_user-123";

		// when
		boolean result = UserNamePolicy.isUsernameFormatValid(validUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("아이디 형식 검증 실패 - 한글 포함")
	void isUsernameFormatValid_Fail_WithKorean() {
		// given
		String usernameWithKorean = "테스트user";

		// when
		boolean result = UserNamePolicy.isUsernameFormatValid(usernameWithKorean);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("아이디 형식 검증 실패 - 허용되지 않는 특수문자 포함")
	void isUsernameFormatValid_Fail_WithInvalidSpecialChar() {
		// given
		String usernameWithInvalidChar = "test@user";

		// when
		boolean result = UserNamePolicy.isUsernameFormatValid(usernameWithInvalidChar);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("아이디 형식 검증 실패 - 공백 포함")
	void isUsernameFormatValid_Fail_WithSpace() {
		// given
		String usernameWithSpace = "test user";

		// when
		boolean result = UserNamePolicy.isUsernameFormatValid(usernameWithSpace);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("유효한 닉네임 형식 검증 - 한글, 영문, 숫자, 특수문자(._-) 조합")
	void isNicknameFormatValid_Success_WithKorean() {
		// given
		String validNickname = "테스터_123";

		// when
		boolean result = UserNamePolicy.isNicknameFormatValid(validNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("유효한 닉네임 형식 검증 - 영문만")
	void isNicknameFormatValid_Success_EnglishOnly() {
		// given
		String validNickname = "TestUser";

		// when
		boolean result = UserNamePolicy.isNicknameFormatValid(validNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("닉네임 형식 검증 실패 - 허용되지 않는 특수문자")
	void isNicknameFormatValid_Fail_WithInvalidSpecialChar() {
		// given
		String nicknameWithInvalidChar = "테스터@123";

		// when
		boolean result = UserNamePolicy.isNicknameFormatValid(nicknameWithInvalidChar);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("닉네임 형식 검증 실패 - 공백 포함")
	void isNicknameFormatValid_Fail_WithSpace() {
		// given
		String nicknameWithSpace = "테스트 유저";

		// when
		boolean result = UserNamePolicy.isNicknameFormatValid(nicknameWithSpace);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - admin (소문자)")
	void isReservedUsername_True_Admin() {
		// given
		String reservedUsername = "admin";

		// when
		boolean result = UserNamePolicy.isReservedUsername(reservedUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - Admin (대소문자 무관)")
	void isReservedUsername_True_AdminUpperCase() {
		// given
		String reservedUsername = "Admin";

		// when
		boolean result = UserNamePolicy.isReservedUsername(reservedUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - ADMIN (대문자)")
	void isReservedUsername_True_AdminAllCaps() {
		// given
		String reservedUsername = "ADMIN";

		// when
		boolean result = UserNamePolicy.isReservedUsername(reservedUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - root")
	void isReservedUsername_True_Root() {
		// given
		String reservedUsername = "root";

		// when
		boolean result = UserNamePolicy.isReservedUsername(reservedUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - system")
	void isReservedUsername_True_System() {
		// given
		String reservedUsername = "system";

		// when
		boolean result = UserNamePolicy.isReservedUsername(reservedUsername);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - 예약어가 아닌 경우")
	void isReservedUsername_False() {
		// given
		String normalUsername = "testuser123";

		// when
		boolean result = UserNamePolicy.isReservedUsername(normalUsername);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("예약된 아이디 체크 - 공백 제거 후 체크")
	void isReservedUsername_True_WithSpaces() {
		// given
		String usernameWithSpaces = "  admin  ";

		// when
		boolean result = UserNamePolicy.isReservedUsername(usernameWithSpaces);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - 관리자")
	void isReservedNickname_True_Korean() {
		// given
		String reservedNickname = "관리자";

		// when
		boolean result = UserNamePolicy.isReservedNickname(reservedNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - 운영자")
	void isReservedNickname_True_Operator() {
		// given
		String reservedNickname = "운영자";

		// when
		boolean result = UserNamePolicy.isReservedNickname(reservedNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - admin (영문)")
	void isReservedNickname_True_Admin() {
		// given
		String reservedNickname = "admin";

		// when
		boolean result = UserNamePolicy.isReservedNickname(reservedNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - 대소문자 무관")
	void isReservedNickname_True_CaseInsensitive() {
		// given
		String reservedNickname = "ADMIN";

		// when
		boolean result = UserNamePolicy.isReservedNickname(reservedNickname);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - 예약어가 아닌 경우")
	void isReservedNickname_False() {
		// given
		String normalNickname = "일반사용자";

		// when
		boolean result = UserNamePolicy.isReservedNickname(normalNickname);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("예약된 닉네임 체크 - 공백 제거 후 체크")
	void isReservedNickname_True_WithSpaces() {
		// given
		String nicknameWithSpaces = "  관리자  ";

		// when
		boolean result = UserNamePolicy.isReservedNickname(nicknameWithSpaces);

		// then
		assertThat(result).isTrue();
	}
}
