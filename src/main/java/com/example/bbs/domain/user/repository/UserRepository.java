package com.example.bbs.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String> {

	List<UserDuplicateView> findAllByLoginIdOrEmailOrNickname(String loginId, String email, String nickname);

	Optional<UserEntity> findByLoginId(String loginId);
}
