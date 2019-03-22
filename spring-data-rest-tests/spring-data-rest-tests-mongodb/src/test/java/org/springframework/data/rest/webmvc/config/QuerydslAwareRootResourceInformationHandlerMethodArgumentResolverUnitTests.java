/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslRepositoryInvokerAdapter;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.tests.mongodb.QUser;
import org.springframework.data.rest.tests.mongodb.Receipt;
import org.springframework.data.rest.tests.mongodb.ReceiptRepository;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests {

	static final Map<String, String[]> NO_PARAMETERS = Collections.emptyMap();

	@Mock Repositories repositories;
	@Mock RepositoryInvokerFactory invokerFactory;
	@Mock ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;

	@Mock RepositoryInvoker invoker;
	@Mock MethodParameter parameter;

	QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver resolver;

	@Before
	public void setUp() {

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "repositories", repositories);
		QuerydslPredicateBuilder builder = new QuerydslPredicateBuilder(new DefaultConversionService(),
				factory.getEntityPathResolver());

		this.resolver = new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(repositories, invokerFactory,
				resourceMetadataResolver, builder, factory);

		when(parameter.hasParameterAnnotation(QuerydslPredicate.class)).thenReturn(true);
	}

	@Test // DATAREST-616
	public void returnsInvokerIfRepositoryIsNotQuerydslAware() {

		ReceiptRepository repository = mock(ReceiptRepository.class);
		when(repositories.getRepositoryFor(Receipt.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, Receipt.class, NO_PARAMETERS);

		assertThat(result, is(invoker));
	}

	@Test // DATAREST-616
	public void wrapsInvokerInQuerydslAdapter() {

		Object repository = mock(QuerydslUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, User.class, NO_PARAMETERS);

		assertThat(result, is(instanceOf(QuerydslRepositoryInvokerAdapter.class)));
	}

	@Test // DATAREST-616
	public void invokesCustomizationOnRepositoryIfItImplementsCustomizer() {

		QuerydslCustomizingUserRepository repository = mock(QuerydslCustomizingUserRepository.class);
		when(repositories.hasRepositoryFor(User.class)).thenReturn(true);
		when(repositories.getRepositoryFor(User.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, User.class, NO_PARAMETERS);

		assertThat(result, is(instanceOf(QuerydslRepositoryInvokerAdapter.class)));
		verify(repository, times(1)).customize(Mockito.any(QuerydslBindings.class), Mockito.any(QUser.class));
	}

	interface QuerydslUserRepository extends QueryDslPredicateExecutor<User> {}

	interface QuerydslCustomizingUserRepository
			extends QueryDslPredicateExecutor<User>, QuerydslBinderCustomizer<QUser> {}
}
