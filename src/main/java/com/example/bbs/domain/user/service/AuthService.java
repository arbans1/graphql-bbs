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
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.error.UserFieldErrorCode;
import com.example.bbs.domain.user.mapper.UserMapper;
import com.example.bbs.domain.user.policy.UserNamePolicy;
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
		log.info("Registering new user: {}", input.getUsername());
		String username = input.getUsername().trim();
		String nickname = input.getNickname().trim();
		String email = input.getEmail().trim();
		String password = input.getPassword();

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

		if (userRepository.existsByLoginId(username)) {
			fieldErrors.add(FieldError.of("username", "이미 사용 중인 아이디입니다.", UserFieldErrorCode.DUPLICATE));
		}
		if (userRepository.existsByEmail(email)) {
			fieldErrors.add(FieldError.of("email", "이미 사용 중인 이메일입니다.", UserFieldErrorCode.DUPLICATE));
		}
		if (userRepository.existsByNickname(nickname)) {
			fieldErrors.add(FieldError.of("nickname", "이미 사용 중인 닉네임입니다.", UserFieldErrorCode.DUPLICATE));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}

		UserEntity newUser = UserEntity.builder()
			.loginId(username)
			.hashedPassword(passwordEncoder.encode(password))
			.nickname(nickname)
			.email(email)
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.build();

		UserEntity saved = userRepository.save(newUser);

		String accessToken = jwtTokenProvider.createAccessToken(saved.getId(), saved.getRole().name());
		String refreshToken = jwtTokenProvider.createRefreshToken(saved.getId());

		return AuthPayload.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.expiresIn(jwtProperties.getAccessExpiration())
			.user(userMapper.toDto(saved))
			.build();
	}

}
