package com.example.bbs.global.error;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
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
		if (ex instanceof InvalidInputException invalidInputEx) {
			return buildInvalidInputError(invalidInputEx, env);
		}

		if (ex instanceof NotFoundException notFoundEx) {
			return buildNotFoundError(notFoundEx, env);
		}

		if (ex instanceof BusinessException businessEx) {
			return buildBusinessError(businessEx, env);
		}

		// 처리되지 않은 예외는 null 반환하여 기본 처리기로 위임
		return null;
	}

	private GraphQLError buildInvalidInputError(InvalidInputException ex, DataFetchingEnvironment env) {
		Map<String, Object> extensions = new HashMap<>();
		extensions.put("code", ex.getCode());
		extensions.put("fieldErrors", ex.getFieldErrors());

		return GraphqlErrorBuilder.newError(env)
			.message(ex.getMessage())
			.errorType(ErrorType.BAD_REQUEST)
			.extensions(extensions)
			.build();
	}

	private GraphQLError buildNotFoundError(NotFoundException ex, DataFetchingEnvironment env) {
		Map<String, Object> extensions = new HashMap<>();
		extensions.put("code", ex.getCode());
		if (ex.getResourceType() != null) {
			extensions.put("resourceType", ex.getResourceType());
		}

		return GraphqlErrorBuilder.newError(env)
			.message(ex.getMessage())
			.errorType(ErrorType.NOT_FOUND)
			.extensions(extensions)
			.build();
	}

	private GraphQLError buildBusinessError(BusinessException ex, DataFetchingEnvironment env) {
		ErrorType errorType = mapToErrorType(ex.getErrorCode());

		Map<String, Object> extensions = new HashMap<>();
		extensions.put("code", ex.getCode());

		return GraphqlErrorBuilder.newError(env)
			.message(ex.getMessage())
			.errorType(errorType)
			.extensions(extensions)
			.build();
	}

	private ErrorType mapToErrorType(ErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_USER_INPUT -> ErrorType.BAD_REQUEST;
			case UNAUTHENTICATED -> ErrorType.UNAUTHORIZED;
			case FORBIDDEN -> ErrorType.FORBIDDEN;
			case NOT_FOUND -> ErrorType.NOT_FOUND;
			case INTERNAL_SERVER_ERROR -> ErrorType.INTERNAL_ERROR;
		};
	}
}
