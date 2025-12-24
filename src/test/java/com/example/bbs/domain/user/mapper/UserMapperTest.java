package com.example.bbs.domain.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;

class UserMapperTest {

	private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

	@Test
	@DisplayName("UserEntity를 User DTO로 변환")
	void toDto_Success() {
		// given
		UserEntity entity = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashedpwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();

		// when
		User dto = userMapper.toDto(entity);

		// then
		assertThat(dto).isNotNull();
		assertThat(dto.getEmail()).isEqualTo("test@example.com");
		assertThat(dto.getProfile()).isNotNull();
		assertThat(dto.getProfile().getNickname()).isEqualTo("테스터");
		assertThat(dto.getRole()).isEqualTo(UserRole.MEMBER);
		assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("UserEntity를 User DTO로 변환 - 프로필 이미지 없음")
	void toDto_Success_WithoutProfileImage() {
		// given
		UserEntity entity = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashedpwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();

		// when
		User dto = userMapper.toDto(entity);

		// then
		assertThat(dto).isNotNull();
		assertThat(dto.getProfile().getImageUrl()).isNull();
	}

	@Test
	@DisplayName("여러 UserEntity를 User DTO 리스트로 변환")
	void toDtoList_Success() {
		// given
		UserEntity entity1 = UserEntity.builder()
			.loginId("user1")
			.hashedPassword("hashedpwd1")
			.nickname("유저1")
			.email("user1@example.com")
			.build();

		UserEntity entity2 = UserEntity.builder()
			.loginId("user2")
			.hashedPassword("hashedpwd2")
			.nickname("유저2")
			.email("user2@example.com")
			.build();

		List<UserEntity> entities = List.of(entity1, entity2);

		// when
		List<User> dtos = userMapper.toDtoList(entities);

		// then
		assertThat(dtos).hasSize(2);
		assertThat(dtos.get(0).getEmail()).isEqualTo("user1@example.com");
		assertThat(dtos.get(0).getProfile().getNickname()).isEqualTo("유저1");
		assertThat(dtos.get(1).getEmail()).isEqualTo("user2@example.com");
		assertThat(dtos.get(1).getProfile().getNickname()).isEqualTo("유저2");
	}

	@Test
	@DisplayName("빈 리스트를 User DTO 리스트로 변환")
	void toDtoList_EmptyList() {
		// given
		List<UserEntity> entities = List.of();

		// when
		List<User> dtos = userMapper.toDtoList(entities);

		// then
		assertThat(dtos).isEmpty();
	}
}
