/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslRepositoryInvokerAdapter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.mongodb.QUser;
import org.springframework.data.rest.webmvc.mongodb.User;
import org.springframework.data.rest.webmvc.mongodb.UserRepository;
import org.springframework.data.web.querydsl.QuerydslBinderCustomizer;
import org.springframework.data.web.querydsl.QuerydslBindings;
import org.springframework.data.web.querydsl.QuerydslPredicateBuilder;

/**
 * Unit tests for {@link QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests {

	static final Map<String, String[]> NO_PARAMETERS = Collections.emptyMap();

	@Mock Repositories repositories;
	@Mock RepositoryInvokerFactory factory;
	@Mock ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;

	@Mock RepositoryInvoker invoker;

	QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver resolver;

	@Before
	public void setUp() {

		QuerydslPredicateBuilder builder = new QuerydslPredicateBuilder(new DefaultConversionService());
		this.resolver = new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(repositories, factory,
				resourceMetadataResolver, builder);
	}

	/**
	 * @see DATAREST-616
	 */
	@Test
	public void returnsInvokerIfRepositoryIsNotQuerydslAware() {

		UserRepository repository = mock(UserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(invoker, User.class, NO_PARAMETERS);

		assertThat(result, is(invoker));
	}

	/**
	 * @see DATAREST-616
	 */
	@Test
	public void wrapsInvokerInQuerydslAdapter() {

		Object repository = mock(QuerydslUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(invoker, User.class, NO_PARAMETERS);

		assertThat(result, is(instanceOf(QuerydslRepositoryInvokerAdapter.class)));
	}

	/**
	 * @see DATAREST-616
	 */
	@Test
	public void invokesCustomizationOnRepositoryIfItImplementsCustomizer() {

		QuerydslCustomizingUserRepository repository = mock(QuerydslCustomizingUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(repository);

		RepositoryInvoker result = resolver.postProcess(invoker, User.class, NO_PARAMETERS);

		assertThat(result, is(instanceOf(QuerydslRepositoryInvokerAdapter.class)));
		verify(repository, times(1)).customize(Mockito.any(QuerydslBindings.class), Mockito.any(QUser.class));
	}

	interface QuerydslUserRepository extends QueryDslPredicateExecutor<User> {}

	interface QuerydslCustomizingUserRepository
			extends QueryDslPredicateExecutor<User>, QuerydslBinderCustomizer<QUser> {}
}
