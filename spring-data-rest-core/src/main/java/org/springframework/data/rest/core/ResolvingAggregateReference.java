/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.data.rest.core;

import java.net.URI;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * An {@link AggregateReference} implementation that resolves the source URI given a {@link Function} or into a fixed
 * value.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 */
public class ResolvingAggregateReference<T, ID> implements AggregateReference<T, ID> {

	private static final Function<URI, UriComponents> STARTER = it -> UriComponentsBuilder.fromUri(it).build();

	private final URI source;
	private final Function<URI, ? extends Object> extractor;
	private final Function<Object, ? extends T> aggregateResolver;
	private final Function<Object, ? extends ID> identifierResolver;

	/**
	 * Creates a new {@link ResolvingAggregateReference} for the given {@link URI} to eventually resolve the final value
	 * against the given resolver function.
	 *
	 * @param source must not be {@literal null}.
	 * @param aggregateResolver must not be {@literal null}.
	 * @param identifierResolver must not be {@literal null}.
	 */
	public ResolvingAggregateReference(URI source, Function<Object, ? extends T> aggregateResolver,
			Function<Object, ? extends ID> identifierResolver) {

		this(source, aggregateResolver, identifierResolver, it -> it);
	}

	protected ResolvingAggregateReference(URI source, Function<Object, ? extends T> aggregateResolver,
			Function<Object, ? extends ID> identifierResolver, Function<URI, ? extends Object> extractor) {

		Assert.notNull(source, "Source URI must not be null!");
		Assert.notNull(aggregateResolver, "Aggregate resolver must not be null!");
		Assert.notNull(identifierResolver, "Identifier resolver must not be null!");

		this.source = source;
		this.aggregateResolver = aggregateResolver;
		this.identifierResolver = identifierResolver;
		this.extractor = extractor;
	}

	/**
	 * Creates a new {@link ResolvingAggregateReference} for the given {@link URI} resolving in the given fixed value.
	 * Primarily for testing purposes.
	 *
	 * @param source must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @param identifier must not be {@literal null}.
	 */
	public ResolvingAggregateReference(URI source, @Nullable T value, ID identifier) {
		this(source, __ -> value, __ -> identifier, it -> it);
	}

	@Override
	public URI getUri() {
		return source;
	}

	@Override
	public ID resolveId() {
		return extractor.andThen(identifierResolver).apply(source);
	}

	@Override
	public T resolveAggregate() {
		return extractor.andThen(aggregateResolver).apply(source);
	}

	@Override
	public AggregateReference<T, ID> withIdSource(Function<UriComponents, Object> extractor) {
		return new ResolvingAggregateReference<>(source, aggregateResolver, identifierResolver, STARTER.andThen(extractor));
	}
}
