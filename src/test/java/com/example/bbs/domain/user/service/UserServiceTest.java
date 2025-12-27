package com.example.bbs.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.dto.UserProfile;
import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;
import com.example.bbs.domain.user.mapper.UserMapper;
import com.example.bbs.domain.user.repository.UserRepository;
import com.example.bbs.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private UserService userService;

	@Nested
	@DisplayName("findById 메서드")
	class FindByIdTest {

		@Test
		@DisplayName("존재하는 사용자 ID로 조회 시 사용자 정보를 반환함")
		void findById_whenUserExists_returnsUser() {
			// given
			String userId = "test-user-id-123";
			UserEntity userEntity = createUserEntity(userId, "testuser", "test@example.com");
			User expectedUser = createUser(userId, "test@example.com");

			when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
			when(userMapper.toDto(userEntity)).thenReturn(expectedUser);

			// when
			User actualUser = userService.findById(userId);

			// then
			assertThat(actualUser).isNotNull();
			assertThat(actualUser.getId()).isEqualTo(userId);
			assertThat(actualUser.getEmail()).isEqualTo("test@example.com");
			verify(userRepository).findById(userId);
			verify(userMapper).toDto(userEntity);
		}

		@Test
		@DisplayName("존재하지 않는 사용자 ID로 조회 시 NotFoundException 발생")
		void findById_whenUserNotExists_throwsNotFoundException() {
			// given
			String userId = "non-existent-id";

			when(userRepository.findById(userId)).thenReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userService.findById(userId))
				.isInstanceOf(BusinessException.NotFoundException.class)
				.hasMessageContaining("사용자")
				.hasMessageContaining(userId);

			verify(userRepository).findById(userId);
		}

	}

	// ========== [Private Helper Methods] ==========

	/**
	 * 테스트용 UserEntity 생성
	 */
	private UserEntity createUserEntity(String id, String loginId, String email) {
		UserEntity userEntity = UserEntity.builder()
			.loginId(loginId)
			.hashedPassword("hashed-password")
			.nickname("테스트사용자")
			.email(email)
			.build();
		ReflectionTestUtils.setField(userEntity, "id", id);
		return userEntity;
	}

	/**
	 * 테스트용 User DTO 생성
	 */
	private User createUser(String id, String email) {
		UserProfile profile = UserProfile.builder()
			.nickname("테스트사용자")
			.imageUrl(null)
			.build();

		return User.builder()
			.id(id)
			.email(email)
			.profile(profile)
			.role(UserRole.MEMBER)
			.status(UserStatus.ACTIVE)
			.createdAt(OffsetDateTime.now())
			.lastLoginAt(null)
			.build();
	}
}
