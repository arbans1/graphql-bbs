package com.example.bbs.global.aop;

import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

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
		}
		// 그 외(System Exception)는 던져서 GraphqlExceptionResolver가 처리하도록 함
	}

	private String getDefaultMessage(BusinessException exc) {
		return Objects.requireNonNullElse(exc.getMessage(), exc.getErrorCode().getDefaultMessage());
	}
}
