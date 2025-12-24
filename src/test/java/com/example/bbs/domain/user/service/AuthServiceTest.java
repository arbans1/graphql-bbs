package com.example.bbs.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.example.bbs.domain.user.dto.AuthPayload;
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

	// ========== 회원가입(register) 테스트 ==========

	@Test
	@DisplayName("회원가입 성공 - 모든 조건이 올바른 경우")
	void register_Success() {
		// given
		RegisterInput input = new RegisterInput("validuser", "유효한유저", "valid@test.com", "password123");

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of());
		when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("access_token");
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");
		when(jwtProperties.getAccessExpiration()).thenReturn(3600L);

		User mockUser = User.builder()
			.id("user_id")
			.email("valid@test.com")
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.build();
		when(userMapper.toDto(any(UserEntity.class))).thenReturn(mockUser);

		// when
		AuthPayload result = authService.register(input);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getAccessToken()).isEqualTo("access_token");
		assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
		assertThat(result.getExpiresIn()).isEqualTo(3600L);
		assertThat(result.getUser()).isNotNull();
		verify(userRepository).save(any(UserEntity.class));
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호 형식 오류 (8자 미만)")
	void register_Fail_PasswordTooShort() {
		// given
		RegisterInput input = new RegisterInput("user123", "테스터", "test@test.com", "short1");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.hasMessageContaining("비밀번호");
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호에 영어 미포함")
	void register_Fail_PasswordNoAlphabet() {
		// given
		RegisterInput input = new RegisterInput("user123", "테스터", "test@test.com", "12345678");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class);
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호에 숫자 미포함")
	void register_Fail_PasswordNoDigit() {
		// given
		RegisterInput input = new RegisterInput("user123", "테스터", "test@test.com", "password");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class);
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호에 아이디 포함")
	void register_Fail_PasswordContainsUsername() {
		// given
		RegisterInput input = new RegisterInput("testuser", "테스터", "test@test.com", "testuser123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("password");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.PASSWORD_CONTAINS_USERNAME.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호에 연속 반복 문자 포함")
	void register_Fail_PasswordRepeatedChars() {
		// given
		RegisterInput input = new RegisterInput("testuser", "테스터", "test@test.com", "pass1111word");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("password");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.WEAK_PASSWORD.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 아이디 형식 오류 (한글 포함)")
	void register_Fail_UsernameInvalidFormat() {
		// given
		RegisterInput input = new RegisterInput("유저123", "테스터", "test@test.com", "password123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("username");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.INVALID_USERNAME_FORMAT.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 예약된 아이디 사용")
	void register_Fail_ReservedUsername() {
		// given
		RegisterInput input = new RegisterInput("admin", "테스터", "test@test.com", "password123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("username");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.RESERVED_USERNAME.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 닉네임 형식 오류 (특수문자 포함)")
	void register_Fail_NicknameInvalidFormat() {
		// given
		RegisterInput input = new RegisterInput("testuser", "테스터@#", "test@test.com", "password123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("nickname");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.INVALID_NICKNAME_FORMAT.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 예약된 닉네임 사용")
	void register_Fail_ReservedNickname() {
		// given
		RegisterInput input = new RegisterInput("testuser", "관리자", "test@test.com", "password123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("nickname");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.RESERVED_NICKNAME.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 아이디 중복")
	void register_Fail_DuplicateUsername() {
		// given
		RegisterInput input = new RegisterInput("existinguser", "테스터", "test@test.com", "password123");

		UserDuplicateView duplicate = new UserDuplicateView() {
			@Override
			public String getLoginId() {
				return "existinguser";
			}

			@Override
			public String getEmail() {
				return "other@test.com";
			}

			@Override
			public String getNickname() {
				return "다른유저";
			}
		};

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of(duplicate));

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("username");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.DUPLICATE.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	void register_Fail_DuplicateEmail() {
		// given
		RegisterInput input = new RegisterInput("newuser", "테스터", "existing@test.com", "password123");

		UserDuplicateView duplicate = new UserDuplicateView() {
			@Override
			public String getLoginId() {
				return "otheruser";
			}

			@Override
			public String getEmail() {
				return "existing@test.com";
			}

			@Override
			public String getNickname() {
				return "다른유저";
			}
		};

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of(duplicate));

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("email");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.DUPLICATE.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 닉네임 중복")
	void register_Fail_DuplicateNickname() {
		// given
		RegisterInput input = new RegisterInput("newuser", "기존닉네임", "new@test.com", "password123");

		UserDuplicateView duplicate = new UserDuplicateView() {
			@Override
			public String getLoginId() {
				return "otheruser";
			}

			@Override
			public String getEmail() {
				return "other@test.com";
			}

			@Override
			public String getNickname() {
				return "기존닉네임";
			}
		};

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of(duplicate));

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).isNotEmpty();
				assertThat(errors.get(0).field()).isEqualTo("nickname");
				assertThat(errors.get(0).code()).isEqualTo(UserFieldErrorCode.DUPLICATE.code());
			});
	}

	@Test
	@DisplayName("회원가입 실패 - 여러 검증 오류 동시 발생")
	void register_Fail_MultipleErrors() {
		// given: 아이디도 예약어이고, 닉네임도 예약어인 경우
		RegisterInput input = new RegisterInput("admin", "관리자", "test@test.com", "password123");

		// when & then
		assertThatThrownBy(() -> authService.register(input))
			.isInstanceOf(BusinessException.InvalidInputException.class)
			.extracting(ex -> ((BusinessException.InvalidInputException)ex).getFieldErrors())
			.satisfies(errors -> {
				assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
			});
	}

	@Test
	@DisplayName("회원가입 성공 - 입력값 앞뒤 공백 제거 처리")
	void register_Success_TrimInput() {
		// given
		RegisterInput input = new RegisterInput("  validuser  ", "  유효한유저  ", "  valid@test.com  ",
			"password123");

		when(userRepository.findAllByLoginIdOrEmailOrNickname(anyString(), anyString(), anyString()))
			.thenReturn(List.of());
		when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("access_token");
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");
		when(jwtProperties.getAccessExpiration()).thenReturn(3600L);

		User mockUser = User.builder().id("user_id").build();
		when(userMapper.toDto(any(UserEntity.class))).thenReturn(mockUser);

		// when
		AuthPayload result = authService.register(input);

		// then
		assertThat(result).isNotNull();
		verify(userRepository).save(any(UserEntity.class));
	}

	// ========== 로그인(login) 테스트 ==========

	@Test
	@DisplayName("로그인 성공 - 올바른 아이디와 비밀번호")
	void login_Success() {
		// given
		LoginInput input = new LoginInput("testuser", "password123");

		UserEntity user = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashed_password")
			.nickname("테스터")
			.email("test@test.com")
			.build();

		when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("access_token");
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");
		when(jwtProperties.getAccessExpiration()).thenReturn(3600L);

		User mockUser = User.builder()
			.id("user_id")
			.email("test@test.com")
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.build();
		when(userMapper.toDto(any(UserEntity.class))).thenReturn(mockUser);

		// when
		AuthPayload result = authService.login(input);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getAccessToken()).isEqualTo("access_token");
		assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
		assertThat(result.getUser()).isNotNull();
		verify(userRepository).findByLoginId("testuser");
	}

	@Test
	@DisplayName("로그인 실패 - 존재하지 않는 아이디")
	void login_Fail_UserNotFound() {
		// given
		LoginInput input = new LoginInput("nonexistent", "password123");

		when(userRepository.findByLoginId("nonexistent")).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.login(input))
			.isInstanceOf(BusinessException.AuthenticationException.class)
			.hasMessageContaining("아이디 또는 비밀번호가 일치하지 않습니다");
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 불일치")
	void login_Fail_WrongPassword() {
		// given
		LoginInput input = new LoginInput("testuser", "wrongpassword");

		UserEntity user = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashed_password")
			.nickname("테스터")
			.email("test@test.com")
			.build();

		when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.login(input))
			.isInstanceOf(BusinessException.AuthenticationException.class)
			.hasMessageContaining("아이디 또는 비밀번호가 일치하지 않습니다");
	}

	@Test
	@DisplayName("로그인 성공 - 마지막 로그인 시간 업데이트")
	void login_Success_UpdatesLastLoginAt() {
		// given
		LoginInput input = new LoginInput("testuser", "password123");

		UserEntity user = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashed_password")
			.nickname("테스터")
			.email("test@test.com")
			.build();

		when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("access_token");
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");
		when(jwtProperties.getAccessExpiration()).thenReturn(3600L);

		User mockUser = User.builder().id("user_id").build();
		when(userMapper.toDto(any(UserEntity.class))).thenReturn(mockUser);

		// when
		authService.login(input);

		// then
		assertThat(user.getLastLoginAt()).isNotNull();
	}

	@Test
	@DisplayName("로그인 성공 - 입력값 앞뒤 공백 제거 처리")
	void login_Success_TrimInput() {
		// given
		LoginInput input = new LoginInput("  testuser  ", "password123");

		UserEntity user = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashed_password")
			.nickname("테스터")
			.email("test@test.com")
			.build();

		when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("access_token");
		when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");
		when(jwtProperties.getAccessExpiration()).thenReturn(3600L);

		User mockUser = User.builder().id("user_id").build();
		when(userMapper.toDto(any(UserEntity.class))).thenReturn(mockUser);

		// when
		AuthPayload result = authService.login(input);

		// then
		assertThat(result).isNotNull();
		verify(userRepository).findByLoginId("testuser");
	}

	@Test
	@DisplayName("비밀번호 정책에 위반되면 InvalidInputException이 발생한다")
	void register_ThrowsException_WhenPasswordInvalid() {
		// given: 정책에 어긋나는 짧은 비밀번호
		RegisterInput input = new RegisterInput("user123", "테스터", "test@test.com", "short");

		// when & then
		assertThrows(BusinessException.InvalidInputException.class, () -> {
			authService.register(input);
		});
	}
}
