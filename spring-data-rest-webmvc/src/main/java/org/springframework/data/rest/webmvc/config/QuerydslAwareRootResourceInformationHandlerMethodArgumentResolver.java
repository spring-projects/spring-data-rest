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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.PersistentProperty;
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
import org.springframework.data.rest.webmvc.json.MappedJacksonProperties;
import org.springframework.data.util.Pair;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.querydsl.core.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link RootResourceInformation} for injection into Spring MVC
 * controller methods.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 2.4
 */
class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver
		extends RootResourceInformationHandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final QuerydslPredicateBuilder predicateBuilder;
	private final QuerydslBindingsFactory factory;
	private final Function<Class<?>, @Nullable MappedJacksonProperties> jacksonPropertiesLookup;

	/**
	 * Creates a new {@link QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver} using the given
	 * {@link Repositories}, {@link RepositoryInvokerFactory} and {@link ResourceMetadataHandlerMethodArgumentResolver}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param resourceMetadataResolver must not be {@literal null}.
	 * @param jacksonPropertiesLookup must not be {@literal null}. May return {@literal null} for domain types that are
	 *          not managed as persistent entities, in which case the request parameter map is forwarded to Querydsl
	 *          unfiltered.
	 */
	public QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(Repositories repositories,
			RepositoryInvokerFactory invokerFactory, ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver,
			QuerydslPredicateBuilder predicateBuilder, QuerydslBindingsFactory factory,
			Function<Class<?>, @Nullable MappedJacksonProperties> jacksonPropertiesLookup) {

		super(repositories, invokerFactory, resourceMetadataResolver);

		this.repositories = repositories;
		this.predicateBuilder = predicateBuilder;
		this.factory = factory;
		this.jacksonPropertiesLookup = jacksonPropertiesLookup;
	}

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

		Map<String, String[]> filteredParameters = filterByJacksonVisibility(domainType, parameters);

		TypeInformation<?> type = TypeInformation.of(domainType);

		QuerydslBindings bindings = factory.createBindingsFor(type);
		Predicate predicate = predicateBuilder.getPredicate(type, toMultiValueMap(filteredParameters), bindings);

		return Optional.ofNullable(predicate).map(it -> Pair.of(repository, it));
	}

	/**
	 * Reduces the request parameter map to entries that map to properties Jackson would expose in serialized responses
	 * for {@code domainType}, translating Jackson field names to the underlying persistent property names that Querydsl
	 * operates on. Without this gate, properties hidden from serialization (e.g. via {@code @JsonIgnore}) would still
	 * be available as server-side filter keys via Querydsl's default permit-all bindings, and Jackson-renamed
	 * properties (e.g. {@code @JsonProperty("renamed")}) would not be addressable under their public alias.
	 */
	private Map<String, String[]> filterByJacksonVisibility(Class<?> domainType, Map<String, String[]> parameters) {

		MappedJacksonProperties properties = jacksonPropertiesLookup.apply(domainType);

		if (properties == null) {
			return parameters;
		}

		Map<String, String[]> filtered = new LinkedHashMap<>(parameters.size());

		for (Entry<String, String[]> entry : parameters.entrySet()) {

			PersistentProperty<?> property = properties.getPersistentProperty(entry.getKey());

			if (property != null) {
				filtered.put(property.getName(), entry.getValue());
			}
		}

		return filtered;
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
