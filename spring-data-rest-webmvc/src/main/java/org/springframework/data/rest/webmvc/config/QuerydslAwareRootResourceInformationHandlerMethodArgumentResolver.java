/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslRepositoryInvokerAdapter;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.mysema.commons.lang.Pair;
import com.querydsl.core.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link RootResourceInformation} for injection into Spring MVC
 * controller methods.
 *
 * @author Oliver Gierke
 * @since 2.4
 */
class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver
		extends RootResourceInformationHandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final QuerydslPredicateBuilder predicateBuilder;
	private final QuerydslBindingsFactory factory;

	/**
	 * Creates a new {@link QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver} using the given
	 * {@link Repositories}, {@link RepositoryInvokerFactory} and {@link ResourceMetadataHandlerMethodArgumentResolver}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param resourceMetadataResolver must not be {@literal null}.
	 */
	public QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(Repositories repositories,
			RepositoryInvokerFactory invokerFactory, ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver,
			QuerydslPredicateBuilder predicateBuilder, QuerydslBindingsFactory factory) {

		super(repositories, invokerFactory, resourceMetadataResolver);

		this.repositories = repositories;
		this.predicateBuilder = predicateBuilder;
		this.factory = factory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver#postProcess(org.springframework.data.repository.support.RepositoryInvoker, java.lang.Class, java.util.Map)
	 */
	@Override
	protected RepositoryInvoker postProcess(MethodParameter parameter, RepositoryInvoker invoker, Class<?> domainType,
			Map<String, String[]> parameters) {

		if (!parameter.hasParameterAnnotation(QuerydslPredicate.class)) {
			return invoker;
		}

		return repositories.getRepositoryFor(domainType)//
				.filter(it -> QuerydslPredicateExecutor.class.isInstance(it))//
				.map(it -> QuerydslPredicateExecutor.class.cast(it))//
				.flatMap(it -> getRepositoryAndPredicate(it, domainType, parameters))//
				.map(it -> getQuerydslAdapter(invoker, it.getFirst(), it.getSecond()))//
				.orElse(invoker);
	}

	private Optional<Pair<QuerydslPredicateExecutor<?>, Predicate>> getRepositoryAndPredicate(
			QuerydslPredicateExecutor<?> repository, Class<?> domainType, Map<String, String[]> parameters) {

		ClassTypeInformation<?> type = ClassTypeInformation.from(domainType);

		QuerydslBindings bindings = factory.createBindingsFor(type);
		Predicate predicate = predicateBuilder.getPredicate(type, toMultiValueMap(parameters), bindings);

		return Optional.ofNullable(predicate).map(it -> Pair.of(repository, it));
	}

	@SuppressWarnings("unchecked")
	private static RepositoryInvoker getQuerydslAdapter(RepositoryInvoker invoker,
			QuerydslPredicateExecutor<?> repository, Predicate predicate) {
		return new QuerydslRepositoryInvokerAdapter(invoker, (QuerydslPredicateExecutor<Object>) repository, predicate);
	}

	/**
	 * Converts the given Map into a {@link MultiValueMap}.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static MultiValueMap<String, String> toMultiValueMap(Map<String, String[]> source) {

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();

		for (Entry<String, String[]> entry : source.entrySet()) {
			result.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}

		return result;
	}
}
