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

import java.util.Arrays;
import java.util.Map;

import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslRepositoryInvokerAdapter;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.mysema.query.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link RootResourceInformation} for injection into Spring MVC
 * controller methods.
 * 
 * @author Oliver Gierke
 * @since 2.4
 */
public class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver
		extends RootResourceInformationHandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final QuerydslPredicateBuilder predicateBuilder;
	private final EntityPathResolver entityPathResolver;

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
			QuerydslPredicateBuilder predicateBuilder) {

		super(repositories, invokerFactory, resourceMetadataResolver);

		this.repositories = repositories;
		this.predicateBuilder = predicateBuilder;
		this.entityPathResolver = SimpleEntityPathResolver.INSTANCE;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver#postProcess(org.springframework.data.repository.support.RepositoryInvoker, java.lang.Class, java.util.Map)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected RepositoryInvoker postProcess(RepositoryInvoker invoker, Class<?> domainType,
			Map<String, String[]> parameters) {

		Object repository = repositories.getRepositoryFor(domainType);

		if (!QueryDslPredicateExecutor.class.isInstance(repository)) {
			return invoker;
		}

		QuerydslBindings bindings = new QuerydslBindings();

		if (QuerydslBinderCustomizer.class.isInstance(repository)) {
			((QuerydslBinderCustomizer) repository).customize(bindings, entityPathResolver.createPath(domainType));
		}

		Predicate predicate = predicateBuilder.getPredicate(ClassTypeInformation.from(domainType),
				toMultiValueMap(parameters), bindings);

		return new QuerydslRepositoryInvokerAdapter(invoker, (QueryDslPredicateExecutor<Object>) repository, predicate);
	}

	/**
	 * Converts the given Map into a {@link MultiValueMap}.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static MultiValueMap<String, String> toMultiValueMap(Map<String, String[]> source) {

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();

		for (String key : source.keySet()) {
			result.put(key, Arrays.asList(source.get(key)));
		}

		return result;
	}
}
