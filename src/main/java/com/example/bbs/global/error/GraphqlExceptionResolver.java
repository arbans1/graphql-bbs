package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL 요청 처리 중 발생한 예외를 GraphQL 에러 응답으로 변환하는 resolver.
 * BusinessException 및 하위 예외들을 적절한 에러 타입과 extensions로 매핑함.
 */
@Slf4j
@NullMarked
@Component
public class GraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

	@Override
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		// 비즈니스 예외는 AOP에서 이미 처리됨.
		// 여기서는 인증 실패(401)나 권한 없음(403) 같은 인프라 에러만 처리.

		@Nullable String message = ex.getMessage();

		// 인증 실패 (401)
		if (ex instanceof AuthenticationException) {
			String authMessage = (message != null) ? message : "인증이 필요한 서비스입니다.";
			return buildError(env, authMessage, ErrorType.UNAUTHORIZED);
		}

		// 인가 실패 (403)
		if (ex instanceof AccessDeniedException) {
			String denyMessage = (message != null) ? message : "접근 권한이 없습니다.";
			return buildError(env, denyMessage, ErrorType.FORBIDDEN);
		}

		// 처리되지 않은 나머지는 null 반환 (기본 500 에러 배열로 처리)
		log.error("알 수 없는 예외가 발생했습니다.", ex);
		return GraphqlErrorBuilder.newError(env)
			.message("내부 처리 오류가 발생했습니다.")
			.errorType(ErrorType.INTERNAL_ERROR)
			.build();
	}

	private GraphQLError buildError(DataFetchingEnvironment env, String message, ErrorType type) {
		return GraphqlErrorBuilder.newError(env)
			.message(message)
			.errorType(type)
			.build();
	}
}
