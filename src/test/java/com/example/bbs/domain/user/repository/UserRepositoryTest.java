package com.example.bbs.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.bbs.domain.user.entity.UserEntity;
import com.example.bbs.support.RepositoryTestBase;

class UserRepositoryTest extends RepositoryTestBase {

	private final UserRepository userRepository;

	public UserRepositoryTest(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Test
	@DisplayName("아이디, 이메일, 닉네임 중 하나라도 중복되면 프로젝션으로 조회된다")
	void findAllByLoginIdOrEmailOrNickname_Success() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("tester")
			.hashedPassword("hashed_pwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();
		userRepository.save(user);

		// when: 아이디만 겹치는 경우 조회 요청
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"tester", "other@example.com", "다른닉네임");

		// then
		assertThat(duplicates).hasSize(1);
		assertThat(duplicates.get(0).getLoginId()).isEqualTo("tester");
		assertThat(duplicates.get(0).getNickname()).isEqualTo("테스터");
	}

	@Test
	@DisplayName("이메일만 중복되는 경우 조회됨")
	void findAllByLoginIdOrEmailOrNickname_EmailDuplicate() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("originalUser")
			.hashedPassword("hashed_pwd")
			.nickname("원본유저")
			.email("test@example.com")
			.build();
		userRepository.save(user);

		// when: 이메일만 겹치는 경우
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"newUser", "test@example.com", "새유저");

		// then
		assertThat(duplicates).hasSize(1);
		assertThat(duplicates.get(0).getEmail()).isEqualTo("test@example.com");
	}

	@Test
	@DisplayName("닉네임만 중복되는 경우 조회됨")
	void findAllByLoginIdOrEmailOrNickname_NicknameDuplicate() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("originalUser")
			.hashedPassword("hashed_pwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();
		userRepository.save(user);

		// when: 닉네임만 겹치는 경우
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"newUser", "new@example.com", "테스터");

		// then
		assertThat(duplicates).hasSize(1);
		assertThat(duplicates.get(0).getNickname()).isEqualTo("테스터");
	}

	@Test
	@DisplayName("여러 필드가 중복되는 경우 한 건만 조회됨")
	void findAllByLoginIdOrEmailOrNickname_MultipleDuplicates() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("tester")
			.hashedPassword("hashed_pwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();
		userRepository.save(user);

		// when: 아이디와 이메일이 모두 겹치는 경우
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"tester", "test@example.com", "다른닉네임");

		// then: 동일한 유저이므로 한 건만 조회됨
		assertThat(duplicates).hasSize(1);
		assertThat(duplicates.get(0).getLoginId()).isEqualTo("tester");
		assertThat(duplicates.get(0).getEmail()).isEqualTo("test@example.com");
	}

	@Test
	@DisplayName("아무것도 중복되지 않으면 빈 리스트 반환")
	void findAllByLoginIdOrEmailOrNickname_NoDuplicate() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("existingUser")
			.hashedPassword("hashed_pwd")
			.nickname("기존유저")
			.email("existing@example.com")
			.build();
		userRepository.save(user);

		// when: 모든 값이 다른 경우
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"newUser", "new@example.com", "새유저");

		// then
		assertThat(duplicates).isEmpty();
	}

	@Test
	@DisplayName("여러 사용자가 있을 때 각각 다른 필드가 중복되면 모두 조회됨")
	void findAllByLoginIdOrEmailOrNickname_MultipleUsers() {
		// given
		UserEntity user1 = UserEntity.builder()
			.loginId("user1")
			.hashedPassword("hashed_pwd")
			.nickname("유저1")
			.email("user1@example.com")
			.build();

		UserEntity user2 = UserEntity.builder()
			.loginId("user2")
			.hashedPassword("hashed_pwd")
			.nickname("유저2")
			.email("user2@example.com")
			.build();

		userRepository.save(user1);
		userRepository.save(user2);

		// when: user1의 아이디와 user2의 이메일이 각각 겹침
		List<UserDuplicateView> duplicates = userRepository.findAllByLoginIdOrEmailOrNickname(
			"user1", "user2@example.com", "새유저");

		// then: 두 명의 사용자가 조회됨
		assertThat(duplicates).hasSize(2);
	}

	@Test
	@DisplayName("아이디로 사용자 조회 - 존재하는 경우")
	void findByLoginId_Success() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("testuser")
			.hashedPassword("hashed_pwd")
			.nickname("테스터")
			.email("test@example.com")
			.build();
		userRepository.save(user);

		// when
		var result = userRepository.findByLoginId("testuser");

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getLoginId()).isEqualTo("testuser");
		assertThat(result.get().getNickname()).isEqualTo("테스터");
		assertThat(result.get().getEmail()).isEqualTo("test@example.com");
	}

	@Test
	@DisplayName("아이디로 사용자 조회 - 존재하지 않는 경우")
	void findByLoginId_NotFound() {
		// given: 데이터 없음

		// when
		var result = userRepository.findByLoginId("nonexistent");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("사용자 저장 후 ID가 자동 생성됨")
	void save_GeneratesId() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("newuser")
			.hashedPassword("hashed_pwd")
			.nickname("신규유저")
			.email("new@example.com")
			.build();

		// when
		UserEntity saved = userRepository.save(user);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getId()).isNotEmpty();
	}

	@Test
	@DisplayName("사용자 저장 시 기본값이 올바르게 설정됨")
	void save_DefaultValues() {
		// given
		UserEntity user = UserEntity.builder()
			.loginId("newuser")
			.hashedPassword("hashed_pwd")
			.nickname("신규유저")
			.email("new@example.com")
			.build();

		// when
		UserEntity saved = userRepository.save(user);

		// then
		assertThat(saved.getRole()).isEqualTo(com.example.bbs.domain.user.enums.UserRole.MEMBER);
		assertThat(saved.getStatus()).isEqualTo(com.example.bbs.domain.user.enums.UserStatus.ACTIVE);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getLastLoginAt()).isNull();
	}
}
