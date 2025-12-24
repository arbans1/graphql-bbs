package com.example.bbs.domain.user.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.bbs.domain.user.enums.UserRole;
import com.example.bbs.domain.user.enums.UserStatus;

@Getter
@Entity
@Table(name = "users")
@SuppressWarnings("NullAway.Init")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Comment("서비스 사용자 정보 및 권한 관리 테이블")
public class UserEntity {

	@Id
	@Tsid
	@Column(name = "id", length = 20, nullable = false, updatable = false)
	private String id;

	@Column(name = "login_id", length = 50, nullable = false, unique = true, updatable = false)
	private String loginId;

	@Column(name = "hashed_password", length = 255, nullable = false)
	private String hashedPassword;

	@Column(name = "nickname", length = 50, nullable = false, unique = true)
	private String nickname;

	@Column(name = "email", length = 255, nullable = false, unique = true)
	private String email;

	@Nullable
	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private UserStatus status;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Nullable
	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	/**
	 * 회원가입에 필요한 필드만 받는 빌더 생성자
	 */
	@SuppressWarnings("NullAway.Init")
	@Builder
	private UserEntity(String loginId, String hashedPassword, String nickname, String email) {
		this.loginId = loginId;
		this.hashedPassword = hashedPassword;
		this.nickname = nickname;
		this.email = email;

		this.role = UserRole.MEMBER;
		this.status = UserStatus.ACTIVE;
	}

	/**
	 * 마지막 로그인 일시 갱신
	 */
	public void updateLastLoginAt() {
		this.lastLoginAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
	}
}
