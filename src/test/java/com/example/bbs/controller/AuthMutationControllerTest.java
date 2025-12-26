package com.example.bbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.LoginInput;
import com.example.bbs.domain.user.dto.LogoutSuccess;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.error.UserFieldErrorCode;
import com.example.bbs.domain.user.service.AuthService;
import com.example.bbs.global.error.BusinessException;
import com.example.bbs.global.error.FieldError;
import com.example.bbs.global.security.JwtProperties;
import com.example.bbs.global.security.JwtTokenProvider;
import com.example.bbs.support.GraphQlTestBase;

@SpringBootTest
class AuthMutationControllerTest extends GraphQlTestBase {

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtProperties jwtProperties;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Nested
	@DisplayName("회원가입(register) 테스트")
	class RegisterTest {

		@Test
		@DisplayName("회원가입 성공 - 올바른 입력값")
		void register_Success() {
			// given
			given(jwtProperties.getRefreshExpiration()).willReturn(3_600_000L);
			User mockUser = User.builder()
				.id("user-id")
				.email("test@test.com")
				.role(UserRole.MEMBER)
				.status(UserStatus.ACTIVE)
				.build();

			AuthPayload mockPayload = new AuthPayload("access-token", mockUser, 3600L);
			AuthTokens mockTokens = new AuthTokens(mockPayload, "refresh-token");

			ResponseCookie refreshCookie = ResponseCookie.from(JwtProperties.REFRESH_TOKEN_COOKIE_NAME, "refresh-token")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(3_600)
				.sameSite("Lax")
				.build();

			given(authService.register(any(RegisterInput.class))).willReturn(mockTokens);
			given(jwtTokenProvider.createRefreshTokenCookie("refresh-token")).willReturn(refreshCookie);

			// when: GraphQL 요청 실행
			List<String> cookieList = new ArrayList<>();
			HttpGraphQlTester mutatedTester = graphQlTester.mutate()
				.webTestClient(webClientBuilder -> {
					webClientBuilder.filter((request, next) -> next.exchange(request).doOnNext(res -> {
						List<String> cookies = res.headers().header(HttpHeaders.SET_COOKIE);
						cookieList.addAll(cookies);
					}));
				})
				.build();
			var response = mutatedTester.documentName("auth/register")
				.variable("input", Map.of(
					"username", "testuser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute();

			// then: 쿠키 및 응답 바디 검증
			assertThat(cookieList)
				.isNotNull()
				.anySatisfy(cookie -> assertThat(cookie)
					.contains(JwtProperties.REFRESH_TOKEN_COOKIE_NAME + "=refresh-token")
					.contains("Max-Age=3600")
					.contains("Path=/"));

			response.errors().verify();
			response.path("auth.register.accessToken").entity(String.class).isEqualTo("access-token");
			response.path("auth.register.expiresIn").entity(Long.class).isEqualTo(3600L);
			response.path("auth.register.user.id").entity(String.class).isEqualTo("user-id");
			response.path("auth.register.user.email").entity(String.class).isEqualTo("test@test.com");
			response.path("auth.register.user.role").entity(String.class).isEqualTo("MEMBER");
			response.path("auth.register.user.status").entity(String.class).isEqualTo("ACTIVE");
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/register")
				.variable("input", Map.of(
					"username", "testuser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "short"))
				.execute();

			// then: 에러 및 필드 검증
			response.errors().verify()
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/register")
				.variable("input", Map.of(
					"username", "existinguser",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute();

			// then: 에러 및 필드 검증
			response.errors().verify()
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/register")
				.variable("input", Map.of(
					"username", "admin",
					"nickname", "테스터",
					"email", "test@test.com",
					"password", "password123"))
				.execute();

			// then: 에러 및 필드 검증
			response.errors().verify()
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/register")
				.variable("input", Map.of(
					"username", "admin",
					"nickname", "관리자",
					"email", "test@test.com",
					"password", "password123"))
				.execute();

			// then: 에러 및 필드 검증
			response.errors().verify()
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
			given(jwtProperties.getRefreshExpiration()).willReturn(3_600_000L);
			User mockUser = User.builder()
				.id("user-id")
				.email("test@test.com")
				.role(UserRole.MEMBER)
				.status(UserStatus.ACTIVE)
				.build();

			AuthPayload mockPayload = new AuthPayload("access-token", mockUser, 3600L);
			AuthTokens mockTokens = new AuthTokens(mockPayload, "refresh-token");

			ResponseCookie refreshCookie = ResponseCookie.from(JwtProperties.REFRESH_TOKEN_COOKIE_NAME, "refresh-token")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(3_600)
				.sameSite("Lax")
				.build();

			given(authService.login(any(LoginInput.class))).willReturn(mockTokens);
			given(jwtTokenProvider.createRefreshTokenCookie("refresh-token")).willReturn(refreshCookie);

			// when: GraphQL 요청 실행
			List<String> cookieList = new ArrayList<>();
			HttpGraphQlTester mutatedTester = graphQlTester.mutate()
				.webTestClient(webClientBuilder -> {
					webClientBuilder.filter((request, next) -> next.exchange(request).doOnNext(res -> {
						List<String> cookies = res.headers().header(HttpHeaders.SET_COOKIE);
						cookieList.addAll(cookies);
					}));
				})
				.build();

			var response = mutatedTester.documentName("auth/login")
				.variable("input", Map.of("username", "tester", "password", "password123"))
				.execute();

			// then: 쿠키 및 응답 바디 검증
			assertThat(cookieList)
				.isNotNull()
				.anySatisfy(cookie -> assertThat(cookie)
					.contains(JwtProperties.REFRESH_TOKEN_COOKIE_NAME + "=refresh-token")
					.contains("Max-Age=3600")
					.contains("Path=/"));

			response.errors().verify();
			response.path("auth.login.accessToken").entity(String.class).isEqualTo("access-token");
			response.path("auth.login.user.id").entity(String.class).isEqualTo("user-id");
			response.path("auth.login.user.email").entity(String.class).isEqualTo("test@test.com");

		}

		@Test
		@DisplayName("로그인 실패 - 존재하지 않는 사용자")
		void login_Fail_UserNotFound() {
			// given
			willThrow(new BusinessException.AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다."))
				.given(authService).login(any(LoginInput.class));

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/login")
				.variable("input", Map.of(
					"username", "nonexistent",
					"password", "password123"))
				.execute();

			// then: 에러 및 메시지 검증
			response.errors().verify()
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/login")
				.variable("input", Map.of(
					"username", "testuser",
					"password", "wrongpassword"))
				.execute();

			// then: 에러 및 메시지 검증
			response.errors().verify()
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

			// when: GraphQL 요청 실행
			var response = graphQlTester.documentName("auth/login")
				.variable("input", Map.of(
					"username", "suspended",
					"password", "password123"))
				.execute();

			// then: 에러 및 메시지 검증
			response.errors().verify()
				.path("auth.login.code").entity(String.class).isEqualTo("FORBIDDEN")
				.path("auth.login.message").entity(String.class).isEqualTo("로그인할 수 없는 계정입니다.");
		}
	}

	@Nested
	@DisplayName("로그아웃(logout) 테스트")
	class LogoutTest {

		@Test
		@DisplayName("로그아웃 성공")
		void logout_Success() {
			// given
			given(authService.logout()).willReturn(new LogoutSuccess("로그아웃 되었습니다"));
			ResponseCookie deleteCookie = ResponseCookie.from(JwtProperties.REFRESH_TOKEN_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(0)
				.sameSite("Lax")
				.build();
			given(jwtTokenProvider.createDeleteRefreshTokenCookie()).willReturn(deleteCookie);

			// when: GraphQL 요청 실행
			List<String> cookieList = new ArrayList<>();

			HttpGraphQlTester mutatedTester = graphQlTester.mutate()
				.webTestClient(clientBuilder -> {
					clientBuilder.filter((request, next) -> next.exchange(request).doOnNext(response -> {
						List<String> cookies = response.headers().header(HttpHeaders.SET_COOKIE);
						cookieList.addAll(cookies);
					}));
				})
				.build();

			var response = mutatedTester.documentName("auth/logout").execute();

			// then: 응답 및 쿠키 검증
			response.errors().verify();
			response.path("auth.logout.message").entity(String.class).isEqualTo("로그아웃 되었습니다");

			assertThat(cookieList)
				.isNotNull()
				.anySatisfy(cookie -> assertThat(cookie)
					.contains(JwtProperties.REFRESH_TOKEN_COOKIE_NAME + "=")
					.contains("Max-Age=0")
					.contains("Path=/"));
		}
	}
}
