package com.example.bbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.dto.UserProfile;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.service.UserService;
import com.example.bbs.global.error.BusinessException;
import com.example.bbs.global.security.JwtTokenProvider;
import com.example.bbs.support.GraphQlTestBase;

@SpringBootTest
class UserQueryControllerTest extends GraphQlTestBase {

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Nested
	@DisplayName("현재 사용자 조회(me) 테스트")
	class MeTest {

		@Test
		@DisplayName("현재 사용자 조회 실패 - MEMBER 권한 없음(익명) 시 401 에러")
		void me_Fail_Anonymous_Unauthorized() {
			// given

			// when
			var response = graphQlTester.documentName("user/me")
				.execute();

			// then
			response.errors().satisfy(errors -> {
				assertThat(errors).hasSize(1);
				ResponseError error = errors.get(0);
				assertThat(error.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
			});
		}

		@Test
		@DisplayName("현재 사용자 조회 성공 - 회원 본인(A) 요청 시 개인정보 필드 포함")
		void me_Success_Member_Self_Full() {
			// given
			String userId = "user-a";
			User mockUser = createUser(userId, "a@test.com");
			given(userService.findById(userId)).willReturn(mockUser);

			HttpGraphQlTester authedTester = authenticatedTester(userId, "ROLE_MEMBER");

			// when
			var response = authedTester.documentName("user/me")
				.execute();

			// then
			response.errors().verify();
			response.path("me.id").entity(String.class).isEqualTo(userId);
			response.path("me.email").entity(String.class).isEqualTo("a@test.com");
			response.path("me.profile.nickname").entity(String.class).isEqualTo("닉네임");
			response.path("me.profile.imageUrl").valueIsNull();
			response.path("me.role").entity(String.class).isEqualTo("MEMBER");
			response.path("me.status").entity(String.class).isEqualTo("ACTIVE");
			response.path("me.createdAt").entity(String.class)
				.satisfies(createdAt -> assertThat(createdAt).isNotBlank());
			response.path("me.lastLoginAt").entity(String.class)
				.satisfies(lastLoginAt -> assertThat(lastLoginAt).isNotBlank());
		}

	}

	@Nested
	@DisplayName("특정 사용자 조회(user) 테스트")
	class UserTest {

		@Test
		@DisplayName("특정 사용자 조회 성공 - 익명(GUEST)도 공개 정보는 보이고 개인정보는 null")
		void user_Success_Anonymous_Partial() {
			// given
			String targetUserId = "user-a";
			User mockUser = createUser(targetUserId, "a@test.com");
			given(userService.findById(targetUserId)).willReturn(mockUser);

			// when
			var response = graphQlTester.documentName("user/user")
				.variable("id", targetUserId)
				.execute();

			// then
			response.errors().verify();
			response.path("user.id").entity(String.class).isEqualTo(targetUserId);
			response.path("user.profile.nickname").entity(String.class).isEqualTo("닉네임");
			response.path("user.profile.imageUrl").valueIsNull();
			response.path("user.role").entity(String.class).isEqualTo("MEMBER");
			response.path("user.createdAt").entity(String.class)
				.satisfies(createdAt -> assertThat(createdAt).isNotBlank());

			response.path("user.email").valueIsNull();
			response.path("user.status").valueIsNull();
			response.path("user.lastLoginAt").valueIsNull();
		}

		@Test
		@DisplayName("특정 사용자 조회 성공 - 회원 본인(A) 요청 시 개인정보 필드 포함")
		void user_Success_Member_Self_Full() {
			// given
			String userId = "user-a";
			User mockUser = createUser(userId, "a@test.com");
			given(userService.findById(userId)).willReturn(mockUser);

			HttpGraphQlTester authedTester = authenticatedTester(userId, "ROLE_MEMBER");

			// when
			var response = authedTester.documentName("user/user")
				.variable("id", userId)
				.execute();

			// then
			response.errors().verify();
			response.path("user.id").entity(String.class).isEqualTo(userId);
			response.path("user.email").entity(String.class).isEqualTo("a@test.com");
			response.path("user.status").entity(String.class).isEqualTo("ACTIVE");
			response.path("user.lastLoginAt").entity(String.class)
				.satisfies(lastLoginAt -> assertThat(lastLoginAt).isNotBlank());
		}

		@Test
		@DisplayName("특정 사용자 조회 성공 - 관리자(ADMIN) 요청 시 타인 개인정보 필드 포함")
		void user_Success_Admin_Full() {
			// given
			String adminId = "admin";
			String targetUserId = "user-a";
			User mockUser = createUser(targetUserId, "a@test.com");
			given(userService.findById(targetUserId)).willReturn(mockUser);

			HttpGraphQlTester authedTester = authenticatedTester(adminId, "ROLE_ADMIN");

			// when
			var response = authedTester.documentName("user/user")
				.variable("id", targetUserId)
				.execute();

			// then
			response.errors().verify();
			response.path("user.id").entity(String.class).isEqualTo(targetUserId);
			response.path("user.email").entity(String.class).isEqualTo("a@test.com");
			response.path("user.status").entity(String.class).isEqualTo("ACTIVE");
			response.path("user.lastLoginAt").entity(String.class)
				.satisfies(lastLoginAt -> assertThat(lastLoginAt).isNotBlank());
		}

		@Test
		@DisplayName("특정 사용자 조회 성공 - 존재하지 않는 사용자면 null 반환")
		void user_Success_NotFound_ReturnsNull() {
			// given
			String targetUserId = "missing-user";
			given(userService.findById(targetUserId))
				.willThrow(new BusinessException.NotFoundException("사용자", targetUserId));

			// when
			var response = graphQlTester.documentName("user/user")
				.variable("id", targetUserId)
				.execute();

			// then
			response.errors().verify();
			response.path("user").valueIsNull();
		}

	}

	private HttpGraphQlTester authenticatedTester(String userId, String role) {
		String accessToken = userId + "-access-token";
		Authentication auth = new UsernamePasswordAuthenticationToken(
			userId,
			null,
			List.of(new SimpleGrantedAuthority(role)));
		given(jwtTokenProvider.validateToken(accessToken, true)).willReturn(true);
		given(jwtTokenProvider.getAuthentication(accessToken)).willReturn(auth);

		return graphQlTester.mutate()
			.header("Authorization", "Bearer " + accessToken)
			.build();
	}

	private User createUser(String userId, String email) {
		return User.builder()
			.id(userId)
			.email(email)
			.profile(UserProfile.builder()
				.nickname("닉네임")
				.imageUrl(null)
				.build())
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.createdAt(OffsetDateTime.parse("2025-01-01T00:00:00Z"))
			.lastLoginAt(OffsetDateTime.parse("2025-01-02T00:00:00Z"))
			.build();
	}
}
