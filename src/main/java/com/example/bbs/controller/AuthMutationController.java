package com.example.bbs.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.RegisterInput;
import com.example.bbs.domain.user.service.AuthService;
import com.example.bbs.global.aop.GqlMutation;
import com.example.bbs.global.common.MutationResult;

@Slf4j
@NullMarked
@Controller
@RequiredArgsConstructor
public class AuthMutationController {

	private final AuthService authService;

	@MutationMapping
	public AuthMutation auth() {
		log.debug("Auth mutation called");
		return new AuthMutation();
	}

	@SchemaMapping(typeName = "AuthMutation", field = "register")
	@GqlMutation
	public MutationResult register(@Argument RegisterInput input) {
		log.info("Register mutation called for username: {}", input.getUsername());
		AuthPayload response = authService.register(input);
		log.info("Register successful for username: {}", input.getUsername());
		return response;
	}

	public static class AuthMutation {
	}
}
