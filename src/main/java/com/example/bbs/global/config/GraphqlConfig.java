package com.example.bbs.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;
import lombok.RequiredArgsConstructor;

import com.example.bbs.global.security.AuthDirectiveWiring;

@Configuration
@RequiredArgsConstructor
public class GraphqlConfig {

	private final AuthDirectiveWiring authDirectiveWiring;

	@Bean
	public RuntimeWiringConfigurer runtimeWiringConfigurer() {
		return wiringBuilder -> {
			wiringBuilder
				.scalar(ExtendedScalars.DateTime)
				.directive("auth", authDirectiveWiring);
		};
	}

}
