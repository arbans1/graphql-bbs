package com.example.bbs.domain.user.policy;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UserNamePolicy {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
	private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣._-]+$");

	private static final Set<String> RESERVED_USERNAMES = Set.of(
		"admin",
		"administrator",
		"root",
		"system",
		"support",
		"help",
		"owner",
		"service",
		"operator",
		"moderator",
		"staff",
		"manager",
		"webmaster",
		"security",
		"info",
		"contact",
		"cs",
		"customer",
		"guest",
		"anonymous",
		"noreply",
		"mailer",
		"robot",
		"null",
		"undefined",
		"test",
		"master",
		"superuser",
		"superadmin",
		"host",
		"api",
		"graphql",
		"bbs");

	private static final Set<String> RESERVED_NICKNAMES = Set.of(
		"관리자",
		"운영자",
		"어드민",
		"시스템",
		"스탭",
		"스태프",
		"모더레이터",
		"관리팀",
		"게스트",
		"손님",
		"익명",
		"admin",
		"guest",
		"anonymous",
		"operator",
		"staff",
		"moderator",
		"manager");

	private UserNamePolicy() {
	}

	public static boolean isUsernameFormatValid(String username) {
		return USERNAME_PATTERN.matcher(username).matches();
	}

	public static boolean isNicknameFormatValid(String nickname) {
		return NICKNAME_PATTERN.matcher(nickname).matches();
	}

	public static boolean isReservedUsername(String username) {
		return RESERVED_USERNAMES.contains(normalize(username));
	}

	public static boolean isReservedNickname(String nickname) {
		return RESERVED_NICKNAMES.contains(normalize(nickname));
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
