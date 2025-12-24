package com.example.bbs.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String> {

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);
}
