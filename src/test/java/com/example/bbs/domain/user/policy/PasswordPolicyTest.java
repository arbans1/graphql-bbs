package com.example.bbs.domain.user.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

	@Test
	@DisplayName("유효한 비밀번호 형식 검증 - 영어와 숫자 포함 8자 이상")
	void isFormatValid_Success() {
		// given
		String validPassword = "password123";

		// when
		boolean result = PasswordPolicy.isFormatValid(validPassword);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("비밀번호 형식 검증 실패 - 8자 미만")
	void isFormatValid_Fail_TooShort() {
		// given
		String shortPassword = "pass12";

		// when
		boolean result = PasswordPolicy.isFormatValid(shortPassword);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("비밀번호 형식 검증 실패 - 영어만 포함")
	void isFormatValid_Fail_NoDigit() {
		// given
		String noDigitPassword = "password";

		// when
		boolean result = PasswordPolicy.isFormatValid(noDigitPassword);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("비밀번호 형식 검증 실패 - 숫자만 포함")
	void isFormatValid_Fail_NoAlphabet() {
		// given
		String noAlphabetPassword = "12345678";

		// when
		boolean result = PasswordPolicy.isFormatValid(noAlphabetPassword);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("비밀번호 형식 검증 성공 - 특수문자 포함 가능")
	void isFormatValid_Success_WithSpecialChars() {
		// given
		String passwordWithSpecial = "pass!@#123";

		// when
		boolean result = PasswordPolicy.isFormatValid(passwordWithSpecial);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("비밀번호에 아이디 포함 여부 확인 - 포함됨")
	void containsLoginId_True() {
		// given
		String password = "myuser12345";
		String loginId = "myuser";

		// when
		boolean result = PasswordPolicy.containsLoginId(password, loginId);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("비밀번호에 아이디 포함 여부 확인 - 포함되지 않음")
	void containsLoginId_False() {
		// given
		String password = "password123";
		String loginId = "testuser";

		// when
		boolean result = PasswordPolicy.containsLoginId(password, loginId);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("반복되는 문자 체크 - 4회 이상 반복")
	void hasRepeatedChars_True_FourTimes() {
		// given
		String password = "pass1111word";

		// when
		boolean result = PasswordPolicy.hasRepeatedChars(password);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("반복되는 문자 체크 - 알파벳 반복")
	void hasRepeatedChars_True_Alphabet() {
		// given
		String password = "aaaaword123";

		// when
		boolean result = PasswordPolicy.hasRepeatedChars(password);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("반복되는 문자 체크 - 3회 이하 반복은 허용")
	void hasRepeatedChars_False_ThreeTimes() {
		// given
		String password = "pass111word";

		// when
		boolean result = PasswordPolicy.hasRepeatedChars(password);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("반복되는 문자 체크 - 반복 없음")
	void hasRepeatedChars_False_NoRepeat() {
		// given
		String password = "password123";

		// when
		boolean result = PasswordPolicy.hasRepeatedChars(password);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("반복되는 문자 체크 - 끝부분 반복")
	void hasRepeatedChars_True_AtEnd() {
		// given
		String password = "password1111";

		// when
		boolean result = PasswordPolicy.hasRepeatedChars(password);

		// then
		assertThat(result).isTrue();
	}
}
