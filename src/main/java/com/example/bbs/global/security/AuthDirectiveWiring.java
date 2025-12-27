package com.example.bbs.global.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import lombok.RequiredArgsConstructor;

import com.example.bbs.global.common.Ownable;

@Component
@NullMarked
@RequiredArgsConstructor
public class AuthDirectiveWiring implements SchemaDirectiveWiring {

	@Override
	@Nullable
	public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
		GraphQLAppliedDirective appliedDirective = env.getAppliedDirective("auth");

		// @auth(requires: ROLE) 인자값이 없으면 기본 회원 권한으로 처리
		final String requiredRole = (appliedDirective.getArgument("requires") != null)
			? appliedDirective.getArgument("requires").getValue().toString()
			: "MEMBER";

		GraphQLFieldsContainer fieldsContainer = env.getFieldsContainer();

		DataFetcher<?> originalDataFetcher = env.getCodeRegistry().getDataFetcher(
			(GraphQLObjectType)fieldsContainer,
			env.getFieldDefinition());

		// GraphQL 필드 실행 앞단에 권한 검증을 삽입
		DataFetcher<?> authDataFetcher = dfe -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			// 권한 체크 통과 시 즉시 원래 로직 실행
			if (checkAccess(requiredRole, auth, dfe.getSource())) {
				return originalDataFetcher.get(dfe);
			}

			// 권한이 없는데 루트 필드가 아니면 null 반환
			if (!isRootField(fieldsContainer)) {
				return null;
			}

			// 권한이 없는 루트 필드라면 상황에 맞는 예외 투척
			throw createAuthException(auth);
		};

		env.getCodeRegistry().dataFetcher(
			(GraphQLObjectType)fieldsContainer,
			env.getFieldDefinition(),
			authDataFetcher);

		return env.getElement();
	}

	private boolean isRootField(GraphQLFieldsContainer container) {
		String name = container.getName();
		return "Query".equals(name) || "Mutation".equals(name);
	}

	private RuntimeException createAuthException(@Nullable Authentication auth) {
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			return new InsufficientAuthenticationException("인증이 필요한 서비스입니다.");
		}
		return new AccessDeniedException("접근 권한이 없습니다.");
	}

	private boolean checkAccess(String requiredRole, Authentication auth, Object source) {
		// 익명 여부와 요구 역할로 접근 가능성을 계산
		boolean isAnonymous = auth == null || auth instanceof AnonymousAuthenticationToken;

		if ("GUEST".equals(requiredRole)) {
			return true;
		}
		if (isAnonymous) {
			return false;
		}
		boolean isAdmin = auth.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		String currentUserId = auth.getName();

		// SELF 계열은 소유자 비교로 제한하고, 기타 역할은 명시된 권한만 통과
		return switch (requiredRole) {
			case "MEMBER" -> true;
			case "ADMIN" -> isAdmin;
			case "SELF" -> currentUserId.equals(extractUserId(source));
			case "SELF_OR_ADMIN" -> isAdmin || currentUserId.equals(extractUserId(source));
			default -> false;
		};
	}

	@Nullable
	private String extractUserId(Object source) {
		// Ownable 구현체에서만 소유자 식별자를 추출
		if (source instanceof Ownable ownable) {
			return ownable.getOwnerId();
		}
		return null;
	}
}
