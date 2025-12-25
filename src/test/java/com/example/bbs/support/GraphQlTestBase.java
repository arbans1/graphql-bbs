package com.example.bbs.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import com.example.bbs.global.aop.GqlMutationAspect;
import com.example.bbs.global.config.GraphqlConfig;

@ActiveProfiles("test")
@EnableAspectJAutoProxy
@Import({GqlMutationAspect.class, GraphqlConfig.class})
@TestConstructor(autowireMode = AutowireMode.ALL)
public abstract class GraphQlTestBase {

	@Autowired
	protected GraphQlTester graphQlTester;
}
