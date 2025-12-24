package com.example.bbs.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullMarked;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@NullMarked
@RequiredArgsConstructor
public class RegisterInput {

	@NotBlank(message = "아이디는 필수 입력 항목입니다.")
	@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "아이디에는 영문, 숫자, ., _, -만 사용할 수 있습니다.")
	@Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하로 입력해주세요.")
	private final String username;

	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
	@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
	private final String password;

	@NotBlank(message = "닉네임은 필수 입력 항목입니다.")
	@Pattern(regexp = "^[a-zA-Z0-9가-힣._-]+$", message = "닉네임에는 한글, 영문, 숫자, ., _, -만 사용할 수 있습니다.")
	@Size(max = 50, message = "닉네임은 50자 이하로 입력해주세요.")
	private final String nickname;

	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	@Email(message = "유효한 이메일 주소를 입력해주세요.")
	@Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
	private final String email;

}
