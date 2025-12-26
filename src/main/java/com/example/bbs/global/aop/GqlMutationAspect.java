package com.example.bbs.global.aop;

import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.example.bbs.global.common.AuthenticationError;
import com.example.bbs.global.common.ForbiddenError;
import com.example.bbs.global.common.UserInputError;
import com.example.bbs.global.error.BusinessException;

@Aspect
@Component
@Slf4j
public class GqlMutationAspect {

	@Around("@annotation(com.example.bbs.global.aop.GqlMutation)")
	public Object handleMutation(ProceedingJoinPoint joinPoint) throws Throwable {
		try {
			return joinPoint.proceed();
		} catch (BusinessException.InvalidInputException e) {
			// 유효성 검사 실패 시
			return new UserInputError(getDefaultMessage(e), e.getErrorCode().getCode(), e.getFieldErrors());
		} catch (BusinessException.AuthenticationException e) {
			// 인증 실패 시
			return new AuthenticationError(getDefaultMessage(e), e.getErrorCode().getCode());
		} catch (BusinessException.ForbiddenException e) {
			// 권한 없음 시
			return new ForbiddenError(getDefaultMessage(e), e.getErrorCode().getCode());
		}
		// 그 외(System Exception)는 던져서 GraphqlExceptionResolver가 처리하도록 함
	}

	private String getDefaultMessage(BusinessException exc) {
		return Objects.requireNonNullElse(exc.getMessage(), exc.getErrorCode().getDefaultMessage());
	}
}
