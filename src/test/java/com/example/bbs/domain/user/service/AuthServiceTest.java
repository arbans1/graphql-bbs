package com.example.bbs.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.error.UserFieldErrorCode;
import com.example.bbs.domain.user.mapper.UserMapper;
import com.example.bbs.domain.user.repository.UserDuplicateView;
import com.example.bbs.domain.user.repository.UserRepository;
import com.example.bbs.global.error.BusinessException;
import com.example.bbs.global.security.JwtProperties;
import com.example.bbs.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private UserMapper userMapper;
	@Mock
	private JwtProperties jwtProperties;
	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private AuthService authService;

	// ========== [Private Helpers] ==========

	/** DB 저장 시 ID를 강제로 주입하는 공통 로직 */
	private void givenUserSaveReturnsWithId(String id) {
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
			UserEntity user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", id);
			return user;
		});
	}

	/** JWT 관련 Mock 설정 */
	private void givenJwtSettings(String accessToken, String refreshToken, long expiresIn) {
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn(accessToken);
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn(refreshToken);
		lenient().when(jwtProperties.getAccessExpiration()).thenReturn(expiresIn);
	}

	/** 중복 검사 시 "중복 없음"을 보장하는 설정 */
	private void givenNoDuplicateUser() {
		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of());
	}

	/** JWT 토큰 생성 시 가짜 토큰 반환 설정 */
	private void givenUserMapperReturnsAuthTokens(String userId, String email) {
		User mockUser = User.builder()
			.id(userId)
			.email(email)
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.build();

		AuthPayload mockPayload = new AuthPayload("access_token", mockUser, 3600L);
		AuthTokens mockTokens = new AuthTokens(mockPayload, "refresh_token");

		when(userMapper.toAuthPayload(any(), anyString(), anyLong())).thenReturn(mockPayload);
		when(userMapper.toAuthTokens(any(), anyString())).thenReturn(mockTokens);
	}

	// ========== 회원가입(register) 테스트 ==========

	@Test
	@DisplayName("회원가입 성공 - 모든 조건이 올바른 경우")
	void register_Success() {
		// given
		RegisterInput input = new RegisterInput("myuser", "password123", "nick1", "valid@test.com");

		givenNoDuplicateUser();
		when(passwordEncoder.encode(anyString())).thenReturn("encoded_pw");

		givenUserSaveReturnsWithId("user-123");
		givenJwtSettings("access_token", "refresh_token", 3600L);
		givenUserMapperReturnsAuthTokens("user-123", "valid@test.com");

		// when
		AuthTokens tokens = authService.register(input);
		AuthPayload result = tokens.payload();

		// then
		assertThat(result.accessToken()).isEqualTo("access_token");
		assertThat(result.user().getId()).isEqualTo("user-123");
		verify(userRepository).save(any(UserEntity.class));
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호 형식 오류 (8자 미만)")
	void register_Fail_PasswordTooShort() {
		// given
		RegisterInput input = new RegisterInput("user123", "short1", "nick1", "test@test.com");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("password");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.INVALID_PASSWORD_FORMAT.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호에 아이디 포함")
	void register_Fail_PasswordContainsUsername() {
		RegisterInput input = new RegisterInput("myuser", "myuser123", "nick1", "test@test.com");

		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.PASSWORD_CONTAINS_USERNAME.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 아이디 중복")
	void register_Fail_DuplicateUsername() {
		RegisterInput input = new RegisterInput("existinguser", "password123", "nick1", "test@test.com");

		UserDuplicateView duplicate = new UserDuplicateView() {
			@Override
			public String getLoginId() {
				return "existinguser";
			} // 중복 발생 지점

			@Override
			public String getEmail() {
				return "other@test.com";
			}

			@Override
			public String getNickname() {
				return "otherNick";
			}
		};

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of(duplicate));

		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors.get(0).field()).isEqualTo("username");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.DUPLICATE.code());
			});
	}

	@Test
	@DisplayName("회원가입 성공 - 입력값 앞뒤 공백 제거 처리")
	void register_Success_TrimInput() {
		RegisterInput input = new RegisterInput("  myuser  ", "password123", "  nick1  ", "  valid@test.com  ");

		givenNoDuplicateUser();
		when(passwordEncoder.encode(anyString())).thenReturn("encoded_pw");
		givenUserSaveReturnsWithId("user-123");
		givenJwtSettings("access_token", "refresh_token", 3600L);
		givenUserMapperReturnsAuthTokens("user-123", "valid@test.com");

		authService.register(input);

		verify(userRepository)
			.save(argThat(user -> user.getLoginId().equals("myuser") && user.getNickname().equals("nick1")));
	}

	// ========== 로그인(login) 테스트 ==========

	@Test
	@DisplayName("로그인 성공 - 올바른 아이디와 비밀번호")
	void login_Success() {
		// given
		LoginInput input = new LoginInput("myuser", "password123");
		UserEntity user = UserEntity.builder().loginId("myuser").hashedPassword("hashed_pw").build();
		ReflectionTestUtils.setField(user, "id", "user-123");

		when(userRepository.findByLoginId("myuser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed_pw")).thenReturn(true);

		givenJwtSettings("access_token", "refresh_token", 3600L);
		givenUserMapperReturnsAuthTokens("user-123", "test@test.com");

		// when
		AuthTokens tokens = authService.login(input);
		AuthPayload result = tokens.payload();

		// then
		assertThat(result.accessToken()).isEqualTo("access_token");
		assertThat(user.getLastLoginAt()).isNotNull();
	}

	@Test
	@DisplayName("로그인 실패 - 존재하지 않는 아이디")
	void login_Fail_UserNotFound() {
		LoginInput input = new LoginInput("nonexistent", "password123");
		when(userRepository.findByLoginId("nonexistent")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(input))
			.isInstanceOf(BusinessException.AuthenticationException.class);
	}

	@Test
	@DisplayName("비밀번호 정책 위반 테스트")
	void register_Fail_InvalidPassword() {
		RegisterInput input = new RegisterInput("user123", "short", "nick1", "test@test.com");
		assertThrows(BusinessException.InvalidInputException.class, () -> authService.register(input));
	}
}
