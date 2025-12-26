package com.example.bbs.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성 및 검증 컴포넌트
 * <p>
 * Access Token과 Refresh Token을 각각 별도 시크릿 키로 서명하여 관리
 * </p>
 */
@Component
@NullMarked
@Slf4j
public class JwtTokenProvider {

	/** Access Token 서명용 HMAC-SHA 비밀 키 */
	private final SecretKey accessKey;

	/** Refresh Token 서명용 HMAC-SHA 비밀 키 */
	private final SecretKey refreshKey;

	/** Access Token 만료 시간 (밀리초) */
	private final long accessExpiration;

	/** Refresh Token 만료 시간 (밀리초) */
	private final long refreshExpiration;

	/**
	 * @param properties JWT 시크릿 키 및 만료 시간 설정
	 */
	public JwtTokenProvider(JwtProperties properties) {
		this.accessKey = Keys.hmacShaKeyFor(properties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
		this.refreshKey = Keys.hmacShaKeyFor(properties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
		this.accessExpiration = properties.getAccessExpiration();
		this.refreshExpiration = properties.getRefreshExpiration();
	}

	/**
	 * 사용자 ID와 권한을 포함한 Access Token 생성
	 *
	 * @param userId 사용자 식별자 (TSID)
	 * @param role 사용자 권한 (ADMIN, MEMBER, GUEST)
	 * @return 서명된 JWT 토큰
	 */
	public String createAccessToken(String userId, String role) {
		Instant now = Instant.now();
		Instant expiryDate = now.plusMillis(accessExpiration);

		return Jwts.builder()
			.subject(userId)
			.claim("role", role)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiryDate))
			.signWith(accessKey)
			.compact();
	}

	/**
	 * 사용자 ID만 포함한 Refresh Token 생성. Access Token 갱신 전용
	 *
	 * @param userId 사용자 식별자 (TSID)
	 * @return 서명된 JWT 토큰
	 */
	public String createRefreshToken(String userId) {
		Instant now = Instant.now();
		Instant expiryDate = now.plusMillis(refreshExpiration);

		return Jwts.builder()
			.subject(userId)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiryDate))
			.signWith(refreshKey)
			.compact();
	}

	/**
	 * JWT 토큰에서 사용자 ID 추출
	 *
	 * @param token JWT 토큰
	 * @param isAccessToken Access Token 여부
	 * @return subject 클레임의 사용자 ID
	 * @throws ExpiredJwtException 토큰 유효 시간이 만료된 경우
	 * @throws JwtException 토큰 서명이 틀리거나 형식이 잘못된 경우
	 * @throws IllegalArgumentException 토큰이 null이거나 비어있는 경우
	 */
	public String getUserIdFromToken(String token, boolean isAccessToken)
		throws ExpiredJwtException, JwtException, IllegalArgumentException {
		SecretKey key = isAccessToken ? accessKey : refreshKey;

		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();
	}

	/**
	 * JWT 토큰 유효성 검증. 서명, 만료 시간, 형식 확인
	 *
	 * @param token JWT 토큰
	 * @param isAccessToken Access Token 여부
	 * @return 유효 여부
	 */
	public boolean validateToken(String token, boolean isAccessToken) {
		SecretKey key = isAccessToken ? accessKey : refreshKey;
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.debug("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
			return false;
		}
	}

	/**
	* Access Token을 기반으로 Spring Security의 Authentication 객체 생성
	* @param accessToken 유효성이 검증된 Access Token
	* @return Authentication 객체
	*/
	public Authentication getAuthentication(String accessToken) {
		Claims claims = Jwts.parser()
			.verifyWith(accessKey)
			.build()
			.parseSignedClaims(accessToken)
			.getPayload();
		String userId = claims.getSubject();
		String role = claims.get("role", String.class); // 페이로드에 저장했던 role
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
		return new UsernamePasswordAuthenticationToken(userId, null, authorities);
	}

	/**
	 * 리프레시 토큰 쿠키 생성
	 *
	 * @param properties JWT 설정 정보
	 * @param refreshToken 리프레시 토큰 문자열
	 * @return ResponseCookie 객체
	 */
	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from(JwtProperties.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(Duration.ofMillis(this.refreshExpiration))
			.sameSite("Lax")
			.build();
	}

	/**
	 * 리프레시 토큰 쿠키 삭제용 쿠키 생성 (Max-Age=0)
	 * * @return 삭제 신호를 포함한 ResponseCookie
	 */
	public ResponseCookie createDeleteRefreshTokenCookie() {
		return ResponseCookie.from(JwtProperties.REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0) // 즉시 만료
			.sameSite("Lax")
			.build();
	}
}
