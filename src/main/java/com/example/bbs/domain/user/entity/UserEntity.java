package com.example.bbs.domain.user.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Comment;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Comment("서비스 사용자 정보 및 권한 관리 테이블")
public class UserEntity {

	@Id
	@Tsid
	@Column(name = "id", length = 20, nullable = false, updatable = false)
	@Comment("사용자 고유 식별자 (TSID)")
	private String id;

	@Column(name = "login_id", length = 50, nullable = false, unique = true)
	@Comment("로그인 시 사용하는 아이디")
	private String loginId;

	@Column(name = "hashed_password", length = 255, nullable = false)
	@Comment("BCrypt로 암호화된 비밀번호")
	private String hashedPassword;

	@Column(name = "nickname", length = 50, nullable = false, unique = true)
	@Comment("사용자 닉네임")
	private String nickname;

	@Column(name = "email", length = 255, nullable = false, unique = true)
	@Comment("이메일")
	private String email;

	@Nullable
	@Column(name = "profile_image_url", length = 500)
	@Comment("프로필 이미지")
	private String profileImageUrl;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	@Comment("권한: ADMIN(운영자), MEMBER(회원), GUEST(비회원)")
	private UserRole role = UserRole.MEMBER;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	@Comment("상태: ACTIVE(활성), SUSPENDED(정지), BANNED(차단), DELETED(탈퇴)")
	private UserStatus status = UserStatus.ACTIVE;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	@Comment("사용자 생성 일시")
	private OffsetDateTime createdAt;

	@Nullable
	@Column(name = "last_login_at")
	@Comment("마지막 로그인 일시")
	private OffsetDateTime lastLoginAt;

}
