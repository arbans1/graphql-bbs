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

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.LogoutSuccess;
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
import com.example.bbs.global.error.ErrorCode;
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
		lenient().when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn(accessToken);
		lenient().when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn(refreshToken);
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

		lenient().when(userMapper.toAuthPayload(any(), anyString(), anyLong())).thenAnswer(invocation -> {
			String token = invocation.getArgument(1);
			Long expiresIn = invocation.getArgument(2);
			return new AuthPayload(token, mockUser, expiresIn);
		});

		lenient().when(userMapper.toAuthTokens(any(AuthPayload.class), anyString())).thenAnswer(invocation -> {
			AuthPayload payload = invocation.getArgument(0);
			String refreshToken = invocation.getArgument(1);
			return new AuthTokens(payload, refreshToken);
		});
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

	// ========== 로그아웃(logout) 테스트 ==========

	@Test
	@DisplayName("로그아웃 성공")
	void logout_Success() {
		// when
		LogoutSuccess result = authService.logout();

		// then
		assertThat(result).isNotNull();
		assertThat(result.message()).isEqualTo("로그아웃 되었습니다");
	}

	// ========== 토큰 갱신(refreshToken) 테스트 ==========

	@Test
	@DisplayName("토큰 갱신 성공 - 유효한 리프레시 토큰으로 새 액세스 토큰 발급")
	void refreshToken_Success() {
		// given
		String refreshToken = "valid_refresh_token";
		String userId = "user-123";
		UserEntity user = UserEntity.builder()
			.loginId("myuser")
			.hashedPassword("hashed_pw")
			.build();
		ReflectionTestUtils.setField(user, "id", userId);

		when(jwtTokenProvider.getUserIdFromToken(refreshToken, false)).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		givenJwtSettings("new_access_token", "refresh_token", 3600L);
		givenUserMapperReturnsAuthTokens(userId, "test@test.com");

		// when
		AuthPayload result = authService.refreshToken(refreshToken);

		// then
		assertThat(result.accessToken()).isEqualTo("new_access_token");
		assertThat(result.user().getId()).isEqualTo(userId);
		verify(jwtTokenProvider).getUserIdFromToken(refreshToken, false);
		verify(userRepository).findById(userId);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - null 리프레시 토큰")
	void refreshToken_Fail_NullToken() {
		// when & then
		assertThatThrownBy(() -> authService.refreshToken(null))
			.isInstanceOf(BusinessException.AuthenticationException.class);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - 빈 문자열 리프레시 토큰")
	void refreshToken_Fail_BlankToken() {
		// when & then
		assertThatThrownBy(() -> authService.refreshToken("   "))
			.isInstanceOf(BusinessException.AuthenticationException.class);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - 만료된 토큰")
	void refreshToken_Fail_ExpiredToken() {
		// given
		String refreshToken = "expired_refresh_token";

		when(jwtTokenProvider.getUserIdFromToken(refreshToken, false))
			.thenThrow(new ExpiredJwtException(null, null, "토큰이 만료됨"));

		// when & then
		assertThatThrownBy(() -> authService.refreshToken(refreshToken))
			.isInstanceOf(BusinessException.AuthenticationException.class)
			.extracting(ex -> ((BusinessException.AuthenticationException)ex).getErrorCode())
			.isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
	void refreshToken_Fail_InvalidToken() {
		// given
		String refreshToken = "invalid_refresh_token";

		when(jwtTokenProvider.getUserIdFromToken(refreshToken, false))
			.thenThrow(new JwtException("토큰이 유효하지 않음"));

		// when & then
		assertThatThrownBy(() -> authService.refreshToken(refreshToken))
			.isInstanceOf(BusinessException.AuthenticationException.class);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - 사용자 존재하지 않음")
	void refreshToken_Fail_UserNotFound() {
		// given
		String refreshToken = "valid_refresh_token";
		String userId = "nonexistent-user";

		when(jwtTokenProvider.getUserIdFromToken(refreshToken, false)).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.refreshToken(refreshToken))
			.isInstanceOf(BusinessException.AuthenticationException.class);
	}

	@Test
	@DisplayName("토큰 갱신 실패 - 사용자 계정 비활성화")
	void refreshToken_Fail_UserInactive() {
		// given
		String refreshToken = "valid_refresh_token";
		String userId = "user-123";
		UserEntity user = UserEntity.builder()
			.loginId("myuser")
			.hashedPassword("hashed_pw")
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
		ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);

		when(jwtTokenProvider.getUserIdFromToken(refreshToken, false)).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> authService.refreshToken(refreshToken))
			.isInstanceOf(BusinessException.ForbiddenException.class);
	}
}
