package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

/**
 * GraphQL 요청 처리 중 발생한 예외를 GraphQL 에러 응답으로 변환하는 resolver.
 * BusinessException 및 하위 예외들을 적절한 에러 타입과 extensions로 매핑함.
 */
@NullMarked
@Component
public class GraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

	@Nullable
	@Override
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		// 비즈니스 예외는 AOP에서 이미 처리됨.
		// 여기서는 인증 실패(401)나 권한 없음(403) 같은 인프라 에러만 처리.
		if (ex instanceof AccessDeniedException) {
			return GraphqlErrorBuilder.newError(env)
				.message("접근 권한이 없습니다.")
				.errorType(ErrorType.FORBIDDEN)
				.build();
		}

		// 처리되지 않은 나머지는 null 반환 (기본 500 에러 배열로 처리)
		return null;
	}
}
