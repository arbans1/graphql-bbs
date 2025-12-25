package com.example.bbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.error.UserFieldErrorCode;
import com.example.bbs.domain.user.service.AuthService;
import com.example.bbs.global.error.BusinessException;
import com.example.bbs.global.error.FieldError;
import com.example.bbs.support.GraphQlTestBase;

@GraphQlTest(AuthMutationController.class)
class AuthMutationControllerTest extends GraphQlTestBase {

	@MockitoBean
	private AuthService authService;

	@Nested
	@DisplayName("회원가입(register) 테스트")
	class RegisterTest {

		@Test
		@DisplayName("회원가입 성공 - 올바른 입력값")
		void register_Success() {
			// given
			AuthPayload mockPayload = AuthPayload.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.expiresIn(3600L)
				.user(User.builder()
					.id("user-id")
					.email("test@test.com")
					.role(UserRole.MEMBER)
					.status(UserStatus.ACTIVE)
					.build())
				.build();

			given(authService.register(any(RegisterInput.class))).willReturn(mockPayload);

			String document = """
				mutation Register($input: RegisterInput!) {
					auth {
						register(input: $input) {
							... on AuthPayload {
								accessToken
								refreshToken
								expiresIn
								user {
									id
									email
									role
									status
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "testuser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.register.accessToken").entity(String.class).isEqualTo("access-token")
				.path("auth.register.refreshToken").entity(String.class).isEqualTo("refresh-token")
				.path("auth.register.expiresIn").entity(Long.class).isEqualTo(3600L)
				.path("auth.register.user.id").entity(String.class).isEqualTo("user-id")
				.path("auth.register.user.email").entity(String.class).isEqualTo("test@test.com")
				.path("auth.register.user.role").entity(String.class).isEqualTo("MEMBER")
				.path("auth.register.user.status").entity(String.class).isEqualTo("ACTIVE");
		}

		@Test
		@DisplayName("회원가입 실패 - 비밀번호 형식 오류")
		void register_Fail_InvalidPassword() {
			// given
			List<FieldError> fieldErrors = List.of(
				FieldError.of("password", "비밀번호는 영어와 숫자를 포함하여 8자 이상이어야 합니다.",
					UserFieldErrorCode.INVALID_PASSWORD_FORMAT));

			willThrow(new BusinessException.InvalidInputException(fieldErrors))
				.given(authService).register(any(RegisterInput.class));

			String document = """
				mutation Register($input: RegisterInput!) {
					auth {
						register(input: $input) {
							... on UserInputError {
								message
								code
								fieldErrors {
									field
									message
									code
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "testuser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "short"))
				.execute()
				.errors().verify()
				.path("auth.register.code").entity(String.class).isEqualTo("BAD_USER_INPUT")
				.path("auth.register.fieldErrors[0].field").entity(String.class).isEqualTo("password")
				.path("auth.register.fieldErrors[0].code").entity(String.class)
				.isEqualTo("INVALID_PASSWORD_FORMAT");
		}

		@Test
		@DisplayName("회원가입 실패 - 아이디 중복")
		void register_Fail_DuplicateUsername() {
			// given
			List<FieldError> fieldErrors = List.of(
				FieldError.of("username", "이미 사용 중인 아이디입니다.", UserFieldErrorCode.DUPLICATE));

			willThrow(new BusinessException.InvalidInputException(fieldErrors))
				.given(authService).register(any(RegisterInput.class));

			String document = """
				mutation Register($input: RegisterInput!) {
					auth {
						register(input: $input) {
							... on UserInputError {
								message
								code
								fieldErrors {
									field
									message
									code
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "existinguser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.register.fieldErrors[0].field").entity(String.class).isEqualTo("username")
				.path("auth.register.fieldErrors[0].code").entity(String.class).isEqualTo("DUPLICATE");
		}

		@Test
		@DisplayName("회원가입 실패 - 예약된 아이디 사용")
		void register_Fail_ReservedUsername() {
			// given
			List<FieldError> fieldErrors = List.of(
				FieldError.of("username", "아이디에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_USERNAME));

			willThrow(new BusinessException.InvalidInputException(fieldErrors))
				.given(authService).register(any(RegisterInput.class));

			String document = """
				mutation Register($input: RegisterInput!) {
					auth {
						register(input: $input) {
							... on UserInputError {
								fieldErrors {
									field
									code
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "admin",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.register.fieldErrors[0].field").entity(String.class).isEqualTo("username")
				.path("auth.register.fieldErrors[0].code").entity(String.class)
				.isEqualTo("RESERVED_USERNAME");
		}

		@Test
		@DisplayName("회원가입 실패 - 여러 검증 오류 동시 발생")
		void register_Fail_MultipleErrors() {
			// given
			List<FieldError> fieldErrors = List.of(
				FieldError.of("username", "아이디에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_USERNAME),
				FieldError.of("nickname", "닉네임에 예약어를 사용할 수 없습니다.", UserFieldErrorCode.RESERVED_NICKNAME));

			willThrow(new BusinessException.InvalidInputException(fieldErrors))
				.given(authService).register(any(RegisterInput.class));

			String document = """
				mutation Register($input: RegisterInput!) {
					auth {
						register(input: $input) {
							... on UserInputError {
								fieldErrors {
									field
									code
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "admin",
					"nickname", "관리자",
					"email", "test@test.com",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.register.fieldErrors").entityList(Object.class).hasSize(2);
		}
	}

	@Nested
	@DisplayName("로그인(login) 테스트")
	class LoginTest {

		@Test
		@DisplayName("로그인 성공 - 올바른 아이디와 비밀번호")
		void login_Success() {
			// given
			AuthPayload mockPayload = AuthPayload.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.expiresIn(3600L)
				.user(User.builder()
					.id("user-id")
					.email("test@test.com")
					.role(UserRole.MEMBER)
					.status(UserStatus.ACTIVE)
					.build())
				.build();

			given(authService.login(any(LoginInput.class))).willReturn(mockPayload);

			String document = """
				mutation Login($input: LoginInput!) {
					auth {
						login(input: $input) {
							... on AuthPayload {
								accessToken
								refreshToken
								expiresIn
								user {
									id
									email
									role
									status
								}
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "tester",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.login.accessToken").entity(String.class).isEqualTo("access-token")
				.path("auth.login.refreshToken").entity(String.class).isEqualTo("refresh-token")
				.path("auth.login.user.id").entity(String.class).isEqualTo("user-id")
				.path("auth.login.user.email").entity(String.class).isEqualTo("test@test.com");
		}

		@Test
		@DisplayName("로그인 실패 - 존재하지 않는 사용자")
		void login_Fail_UserNotFound() {
			// given
			willThrow(new BusinessException.AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다."))
				.given(authService).login(any(LoginInput.class));

			String document = """
				mutation Login($input: LoginInput!) {
					auth {
						login(input: $input) {
							... on AuthenticationError {
								message
								code
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "nonexistent",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.login.code").entity(String.class).isEqualTo("UNAUTHENTICATED")
				.path("auth.login.message").entity(String.class)
				.isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
		}

		@Test
		@DisplayName("로그인 실패 - 비밀번호 불일치")
		void login_Fail_WrongPassword() {
			// given
			willThrow(new BusinessException.AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다."))
				.given(authService).login(any(LoginInput.class));

			String document = """
				mutation Login($input: LoginInput!) {
					auth {
						login(input: $input) {
							... on AuthenticationError {
								message
								code
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "testuser",
					"password", "wrongpassword"))
				.execute()
				.errors().verify()
				.path("auth.login.code").entity(String.class).isEqualTo("UNAUTHENTICATED")
				.path("auth.login.message").entity(String.class)
				.isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
		}

		@Test
		@DisplayName("로그인 실패 - 비활성화된 계정")
		void login_Fail_InactiveAccount() {
			// given
			willThrow(new BusinessException.ForbiddenException("로그인할 수 없는 계정입니다."))
				.given(authService).login(any(LoginInput.class));

			String document = """
				mutation Login($input: LoginInput!) {
					auth {
						login(input: $input) {
							... on ForbiddenError {
								message
								code
							}
						}
					}
				}
				""";

			// when & then
			graphQlTester.document(document)
				.variable("input", Map.of(
					"username", "suspended",
					"password", "password123"))
				.execute()
				.errors().verify()
				.path("auth.login.code").entity(String.class).isEqualTo("FORBIDDEN")
				.path("auth.login.message").entity(String.class).isEqualTo("로그인할 수 없는 계정입니다.");
		}
	}
}
