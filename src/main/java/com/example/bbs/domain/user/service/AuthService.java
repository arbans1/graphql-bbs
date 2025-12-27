package com.example.bbs.domain.user.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.LogoutSuccess;
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
import com.example.bbs.global.error.ErrorCode;
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

	/**
	 * 회원가입 처리
	 *
	 * 입력값 검증 → 사용자 정보 저장 → JWT 토큰 발급의 순서로 진행.
	 *
	 * @param input 회원가입 요청 정보 (아이디, 닉네임, 이메일, 비밀번호)
	 * @return 발급된 액세스/리프레시 토큰
	 */
	@Transactional
	public AuthTokens register(RegisterInput input) {

		String username = input.getUsername().trim();
		String nickname = input.getNickname().trim();
		String email = input.getEmail().trim();
		String password = input.getPassword();

		// 아이디 및 닉네임 정책 검증
		validateUserNamePolicy(username, nickname);
		// 비밀번호 정책 검증
		validatePasswordPolicy(username, password);
		// 중복 여부 검증
		validateDuplication(username, email, nickname);

		// 새로운 사용자 엔티티 생성 및 비밀번호 암호화 저장
		UserEntity newUser = UserEntity.builder()
			.loginId(username)
			.hashedPassword(passwordEncoder.encode(password))
			.nickname(nickname)
			.email(email)
			.build();

		UserEntity saved = userRepository.save(newUser);

		return createAuthTokens(saved);
	}

	/**
	 * 로그인 처리
	 *
	 * 사용자 존재 여부 확인 → 비밀번호 검증 → 계정 상태 확인 → 마지막 로그인 시간 갱신 → JWT 토큰 발급.
	 *
	 * @param input 로그인 요청 정보 (아이디, 비밀번호)
	 * @return 발급된 액세스/리프레시 토큰
	 * @throws BusinessException.AuthenticationException 아이디 또는 비밀번호가 일치하지 않는 경우
	 * @throws BusinessException.ForbiddenException 계정 상태가 활성화되지 않은 경우
	 */
	@Transactional
	public AuthTokens login(LoginInput input) {

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

		// 계정 상태 확인 (활성화된 계정만 로그인 가능)
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException.ForbiddenException("로그인할 수 없는 계정입니다.");
		}

		// 로그인 성공 - 마지막 로그인 시간 갱신
		user.updateLastLoginAt();

		return createAuthTokens(user);
	}

	/**
	 * 로그아웃 처리
	 *
	 * 현재 인증 정보를 SecurityContext에서 제거하고 로그아웃 성공 메시지 반환.
	 * 클라이언트는 리프레시 토큰 쿠키를 별도로 삭제해야 합니다.
	 *
	 * @return 로그아웃 성공 응답
	 */
	public LogoutSuccess logout() {
		return new LogoutSuccess("로그아웃 되었습니다");
	}

	/**
	 * 토큰 갱신 처리
	 *
	 * 리프레시 토큰 검증 → 사용자 존재 여부 확인 → 계정 상태 확인 → 새로운 액세스 토큰 발급.
	 * 리프레시 토큰 갱신은 클라이언트에서 별도 처리.
	 *
	 * @param refreshToken 리프레시 토큰
	 * @return 새로운 액세스 토큰 정보
	 * @throws BusinessException.AuthenticationException 토큰이 만료되었거나 유효하지 않은 경우
	 * @throws BusinessException.ForbiddenException 사용자 계정이 활성화되지 않은 경우
	 */
	public AuthPayload refreshToken(@Nullable String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException.AuthenticationException("인증 정보가 유효하지 않습니다.");
		}
		// 리프레시 토큰 검증 및 사용자 ID 추출
		String userId;
		try {
			userId = jwtTokenProvider.getUserIdFromToken(refreshToken, false);
		} catch (ExpiredJwtException e) {
			throw new BusinessException.AuthenticationException(ErrorCode.TOKEN_EXPIRED, "세션이 만료되었습니다.");
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException.AuthenticationException("인증 정보가 유효하지 않습니다.");
		}

		// 사용자 조회
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException.AuthenticationException("존재하지 않는 사용자입니다."));

		// 계정 상태 확인
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException.ForbiddenException("로그인이 제한된 사용자입니다.");
		}

		// 새로운 액세스 토큰 발급
		return createAuthPayload(user);
	}

	/**
	 * 인증 페이로드 생성
	 *
	 * 사용자 정보를 기반으로 새로운 액세스 토큰을 생성하고 응답 페이로드 구성.
	 *
	 * @param user 사용자 엔티티
	 * @return 액세스 토큰과 만료 시간을 포함한 인증 페이로드
	 */
	private AuthPayload createAuthPayload(UserEntity user) {
		// 액세스 토큰 생성 (사용자 ID와 권한 정보 포함)
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
		long expiresIn = jwtProperties.getAccessExpiration();

		return userMapper.toAuthPayload(user, accessToken, expiresIn);
	}

	/**
	 * JWT 토큰 발급 및 응답 페이로드 생성
	 */
	private AuthTokens createAuthTokens(UserEntity user) {
		AuthPayload payload = createAuthPayload(user);
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
		return userMapper.toAuthTokens(payload, refreshToken);
	}

	/**
	 * 사용자명 정책 검증
	 *
	 * 아이디와 닉네임의 형식 및 예약어 여부를 검증.
	 * 검증 실패 시 모든 오류를 수집하여 한 번에 반환.
	 *
	 * @param username 사용자 아이디
	 * @param nickname 사용자 닉네임
	 * @throws BusinessException.InvalidInputException 정책 위반 시 발생
	 */
	private void validateUserNamePolicy(String username, String nickname) {
		List<FieldError> fieldErrors = new ArrayList<>();

		// 아이디 형식 검증
		if (!UserNamePolicy.isUsernameFormatValid(username)) {
			fieldErrors
				.add(FieldError.of("username", "아이디 형식이 허용되지 않습니다.", UserFieldErrorCode.INVALID_USERNAME_FORMAT));
		}
		// 아이디 예약어 검증
		if (UserNamePolicy.isReservedUsername(username)) {
			fieldErrors
				.add(FieldError.of("username", "아이디에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_USERNAME));
		}
		// 닉네임 형식 검증
		if (!UserNamePolicy.isNicknameFormatValid(nickname)) {
			fieldErrors
				.add(FieldError.of("nickname", "닉네임 형식이 허용되지 않습니다.", UserFieldErrorCode.INVALID_NICKNAME_FORMAT));
		}
		// 닉네임 예약어 검증
		if (UserNamePolicy.isReservedNickname(nickname)) {
			fieldErrors
				.add(FieldError.of("nickname", "닉네임에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_NICKNAME));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}
	}

	/**
	 * 비밀번호 정책 검증
	 *
	 * 비밀번호의 형식, 아이디 포함 여부, 반복 문자 등을 검증.
	 * 검증 실패 시 모든 오류를 수집하여 한 번에 반환.
	 *
	 * @param username 사용자 아이디 (비밀번호 포함 여부 검증용)
	 * @param password 검증할 비밀번호
	 * @throws BusinessException.InvalidInputException 정책 위반 시 발생
	 */
	private void validatePasswordPolicy(String username, String password) {
		List<FieldError> fieldErrors = new ArrayList<>();

		// 비밀번호 형식 검증 (영어, 숫자, 8자 이상)
		if (!PasswordPolicy.isFormatValid(password)) {
			fieldErrors.add(FieldError.of("password", "비밀번호는 영어와 숫자를 포함하여 8자 이상이어야 합니다.",
				UserFieldErrorCode.INVALID_PASSWORD_FORMAT));
		}

		// 비밀번호에 아이디 포함 여부 검증
		if (PasswordPolicy.containsLoginId(password, username)) {
			fieldErrors.add(
				FieldError.of("password", "비밀번호에 아이디를 포함할 수 없습니다.", UserFieldErrorCode.PASSWORD_CONTAINS_USERNAME));
		}

		// 연속 반복 문자 검증
		if (PasswordPolicy.hasRepeatedChars(password)) {
			fieldErrors.add(FieldError.of("password", "연속으로 반복되는 문자는 사용할 수 없습니다.", UserFieldErrorCode.WEAK_PASSWORD));
		}

		if (!fieldErrors.isEmpty()) {
			throw new BusinessException.InvalidInputException(fieldErrors);
		}
	}

	/**
	 * 사용자 정보 중복 검증
	 *
	 * 아이디, 이메일, 닉네임의 중복 여부를 데이터베이스에서 조회하여 검증.
	 * 중복된 필드가 여러 개일 경우 모두 수집하여 반환.
	 *
	 * @param username 검증할 사용자 아이디
	 * @param email 검증할 이메일
	 * @param nickname 검증할 닉네임
	 * @throws BusinessException.InvalidInputException 중복된 정보 존재 시 발생
	 */
	private void validateDuplication(String username, String email, String nickname) {
		List<FieldError> fieldErrors = new ArrayList<>();

		// 중복 데이터 조회
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(username, email,
			nickname);

		// 중복된 필드별로 오류 메시지 추가
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
