package com.example.bbs.global.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
import com.example.bbs.global.error.BusinessException;

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
			String containerName = fieldsContainer.getName();
			// 루트(Query/Mutation) 필드에서는 권한 부족 시 예외를 던져 바로 실패
			boolean isRootField = "Query".equals(containerName) || "Mutation".equals(containerName);

			if (!checkAccess(requiredRole, auth, dfe.getSource())) {
				if (isRootField) {
					throw new BusinessException.ForbiddenException("접근 권한이 없습니다.");
				}
				return null;
			}
			return originalDataFetcher.get(dfe);
		};

		env.getCodeRegistry().dataFetcher(
			(GraphQLObjectType)fieldsContainer,
			env.getFieldDefinition(),
			authDataFetcher);

		return env.getElement();
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
