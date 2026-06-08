/*
 * Copyright 2015-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
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
import org.springframework.data.rest.tests.mongodb.Profile;
import org.springframework.data.rest.tests.mongodb.QUser;
import org.springframework.data.rest.tests.mongodb.Receipt;
import org.springframework.data.rest.tests.mongodb.ReceiptRepository;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.webmvc.json.MappedProperties;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests {

	static final Map<String, String[]> NO_PARAMETERS = Collections.emptyMap();

	@Mock Repositories repositories;
	@Mock RepositoryInvokerFactory invokerFactory;
	@Mock ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;
	@Mock(lenient = true) QuerydslPredicateBuilder builder;

	@Mock RepositoryInvoker invoker;
	@Mock MethodParameter parameter;

	QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver resolver;
	Function<Class<?>, MappedProperties> jacksonPropertiesLookup;

	@BeforeEach
	void setUp() {

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "repositories", Optional.of(repositories));

		MongoCustomConversions conversions = new MongoCustomConversions(Collections.emptyList());
		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		mappingContext.getPersistentEntity(User.class);
		mappingContext.getPersistentEntity(Receipt.class);
		mappingContext.getPersistentEntity(Profile.class);
		PersistentEntities entities = new PersistentEntities(List.of(mappingContext));

		ObjectMapper mapper = new ObjectMapper();
		this.jacksonPropertiesLookup = type -> entities.getPersistentEntity(type)
				.map(entity -> MappedProperties.forSerialization(entity, mapper)).orElse(null);

		this.resolver = new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(repositories, invokerFactory,
				resourceMetadataResolver, builder, factory, jacksonPropertiesLookup);

		when(builder.getPredicate(any(), any(), any())).thenReturn(mock(Predicate.class));
		when(parameter.hasParameterAnnotation(QuerydslPredicate.class)).thenReturn(true);
	}

	@Test // DATAREST-616
	void returnsInvokerIfRepositoryIsNotQuerydslAware() {

		ReceiptRepository repository = mock(ReceiptRepository.class);
		when(repositories.getRepositoryFor(Receipt.class)).thenReturn(Optional.of(repository));

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, Receipt.class, NO_PARAMETERS);

		assertThat(result).isEqualTo(invoker);
	}

	@Test // DATAREST-616
	void wrapsInvokerInQuerydslAdapter() {

		Object repository = mock(QuerydslUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(Optional.of(repository));

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, User.class, NO_PARAMETERS);

		assertThat(result).isInstanceOf(QuerydslRepositoryInvokerAdapter.class);
	}

	@Test // DATAREST-616
	void invokesCustomizationOnRepositoryIfItImplementsCustomizer() {

		QuerydslCustomizingUserRepository repository = mock(QuerydslCustomizingUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(Optional.of(repository));

		RepositoryInvoker result = resolver.postProcess(parameter, invoker, User.class, NO_PARAMETERS);

		assertThat(result).isInstanceOf(QuerydslRepositoryInvokerAdapter.class);
		verify(repository, times(1)).customize(Mockito.any(QuerydslBindings.class), Mockito.any(QUser.class));
	}

	@Test // GH-2572
	void doesNotExposeJsonIgnoredPropertiesAsFilterKeys() {

		Object repository = mock(QuerydslUserRepository.class);
		when(repositories.getRepositoryFor(User.class)).thenReturn(Optional.of(repository));

		Map<String, String[]> parameters = Map.of("ignored", new String[] { "candidate-value" });

		ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.captor();
		when(builder.getPredicate(any(), captor.capture(), any())).thenReturn(mock(Predicate.class));

		resolver.postProcess(parameter, invoker, User.class, parameters);

		// Querydsl never receives a parameter that maps to a @JsonIgnore-annotated property: it could otherwise be used
		// as a server-side filter key (and as an existence oracle) for a value the framework refuses to serialize.
		assertThat(captor.getValue()).doesNotContainKey("ignored");
	}

	@Test // GH-2572
	void translatesJacksonRenamedPropertyToPersistentPropertyName() {

		Object repository = mock(QuerydslProfileRepository.class);
		when(repositories.getRepositoryFor(Profile.class)).thenReturn(Optional.of(repository));

		// Profile.aliased is exposed as "renamed" via @JsonProperty("renamed"). A request that addresses the public alias
		// must reach Querydsl under the underlying domain property name; the bare Java field name must not be accepted.
		Map<String, String[]> parameters = Map.of(
				"renamed", new String[] { "value" },
				"aliased", new String[] { "ignored" });

		ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.captor();
		when(builder.getPredicate(any(), captor.capture(), any())).thenReturn(mock(Predicate.class));

		resolver.postProcess(parameter, invoker, Profile.class, parameters);

		MultiValueMap<String, String> forwarded = captor.getValue();
		assertThat(forwarded).doesNotContainKey("renamed");
		assertThat(forwarded.get("aliased")).containsExactly("value");
	}

	interface QuerydslUserRepository extends QuerydslPredicateExecutor<User> {}

	interface QuerydslProfileRepository extends QuerydslPredicateExecutor<Profile> {}

	interface QuerydslCustomizingUserRepository
			extends QuerydslPredicateExecutor<User>, QuerydslBinderCustomizer<QUser> {}
}
