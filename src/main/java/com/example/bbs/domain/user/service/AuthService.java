package com.example.bbs.domain.user.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.error.UserFieldErrorCode;
import com.example.bbs.domain.user.mapper.UserMapper;
import com.example.bbs.domain.user.policy.PasswordPolicy;
import com.example.bbs.domain.user.policy.UserNamePolicy;
import com.example.bbs.domain.user.repository.UserDuplicateView;
import com.example.bbs.domain.user.repository.UserRepository;
import com.example.bbs.global.error.BusinessException;
import com.example.bbs.global.error.FieldError;
import com.example.bbs.global.security.JwtProperties;
import com.example.bbs.global.security.JwtTokenProvider;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final JwtProperties jwtProperties;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public AuthPayload register(RegisterInput input) {

		String username = input.getUsername().trim();
		String nickname = input.getNickname().trim();
		String email = input.getEmail().trim();
		String password = input.getPassword();

		validateUserNamePolicy(username, nickname);
		validatePasswordPolicy(username, password);
		validateDuplication(username, email, nickname);

		UserEntity newUser = UserEntity.builder()
			.loginId(username)
			.hashedPassword(passwordEncoder.encode(password))
			.nickname(nickname)
			.email(email)
			.build();

		UserEntity saved = userRepository.save(newUser);

		return createAuthPayload(saved);
	}

	@Transactional
	public AuthPayload login(LoginInput input) {

		String username = input.getUsername().trim();
		String password = input.getPassword();

		// 사용자 조회
		UserEntity user = userRepository.findByLoginId(username)
			.orElseThrow(() -> {
				throw new BusinessException.AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다.");
			});

		// 비밀번호 검증
		if (!passwordEncoder.matches(password, user.getHashedPassword())) {
			throw new BusinessException.AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다.");
		}

		// 계정 상태 확인
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException.ForbiddenException("로그인할 수 없는 계정입니다.");
		}

		// 로그인 성공
		user.updateLastLoginAt();

		return createAuthPayload(user);
	}

	/**
	 * JWT 토큰 발급 및 응답 페이로드 생성
	 */
	private AuthPayload createAuthPayload(UserEntity user) {
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		return AuthPayload.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.expiresIn(jwtProperties.getAccessExpiration())
			.user(userMapper.toDto(user))
			.build();
	}

	/**
	 * 사용자 이름 정책 검증 (아이디 및 닉네임 형식 및 예약어 체크)
	 */
	private void validateUserNamePolicy(String username, String nickname) {
		List<FieldError> fieldErrors = new ArrayList<>();

		if (!UserNamePolicy.isUsernameFormatValid(username)) {
			fieldErrors
				.add(FieldError.of("username", "아이디 형식이 허용되지 않습니다.", UserFieldErrorCode.INVALID_USERNAME_FORMAT));
		}
		if (UserNamePolicy.isReservedUsername(username)) {
			fieldErrors
				.add(FieldError.of("username", "아이디에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_USERNAME));
		}
		if (!UserNamePolicy.isNicknameFormatValid(nickname)) {
			fieldErrors
				.add(FieldError.of("nickname", "닉네임 형식이 허용되지 않습니다.", UserFieldErrorCode.INVALID_NICKNAME_FORMAT));
		}
		if (UserNamePolicy.isReservedNickname(nickname)) {
			fieldErrors
				.add(FieldError.of("nickname", "닉네임에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_NICKNAME));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}
	}

	private void validatePasswordPolicy(String username, String password) {
		List<FieldError> fieldErrors = new ArrayList<>();

		if (!PasswordPolicy.isFormatValid(password)) {
			fieldErrors.add(FieldError.of("password", "비밀번호는 영어와 숫자를 포함하여 8자 이상이어야 합니다.",
				UserFieldErrorCode.INVALID_PASSWORD_FORMAT));
		}

		if (PasswordPolicy.containsLoginId(password, username)) {
			fieldErrors.add(
				FieldError.of("password", "비밀번호에 아이디를 포함할 수 없습니다.", UserFieldErrorCode.PASSWORD_CONTAINS_USERNAME));
		}

		if (PasswordPolicy.hasRepeatedChars(password)) {
			fieldErrors.add(FieldError.of("password", "연속으로 반복되는 문자는 사용할 수 없습니다.", UserFieldErrorCode.WEAK_PASSWORD));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}
	}

	/**
	 * 사용자 정보 중복 검증 (아이디, 이메일, 닉네임)
	 */
	private void validateDuplication(String username, String email, String nickname) {
		List<FieldError> fieldErrors = new ArrayList<>();

		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(username, email,
			nickname);

		for (UserDuplicateView user : duplicates) {
			if (user.getLoginId().equals(username)) {
				fieldErrors.add(FieldError.of("username", "이미 사용 중인 아이디입니다.", UserFieldErrorCode.DUPLICATE));
			}
			if (user.getEmail().equals(email)) {
				fieldErrors.add(FieldError.of("email", "이미 사용 중인 이메일입니다.", UserFieldErrorCode.DUPLICATE));
			}
			if (user.getNickname().equals(nickname)) {
				fieldErrors.add(FieldError.of("nickname", "이미 사용 중인 닉네임입니다.", UserFieldErrorCode.DUPLICATE));
			}
		}
		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}
	}

}
