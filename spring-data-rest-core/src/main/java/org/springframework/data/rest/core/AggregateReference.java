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

import org.jspecify.annotations.Nullable;
import org.springframework.web.util.UriComponents;

/**
 * Represents a reference to an aggregate backed by a URI. It can be resolved into an aggregate identifier or the
 * aggregate instance itself.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 */
public interface AggregateReference<T, ID> {

	/**
	 * Returns the source {@link URI}.
	 *
	 * @return will never be {@literal null}.
	 */
	URI getUri();

	/**
	 * Creates a new {@link AggregateReference} resolving the identifier source value from the given
	 * {@link UriComponents}.
	 *
	 * @param extractor must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	AggregateReference<T, ID> withIdSource(Function<UriComponents, Object> extractor);

	/**
	 * Resolves the underlying URI into a full aggregate, potentially applying the configured identifier extractor.
	 *
	 * @return can be {@literal null}.
	 * @see #withIdSource(Function)
	 */
	@Nullable
	T resolveAggregate();

	/**
	 * Resolves the underlying URI into an aggregate identifier, potentially applying the registered identifier extractor.
	 *
	 * @return can be {@literal null}.
	 * @see #withIdSource(Function)
	 */
	@Nullable
	ID resolveId();

	/**
	 * Resolves the underlying URI into a full aggregate, potentially applying the configured identifier extractor.
	 *
	 * @return will never be {@literal null}.
	 * @throws IllegalStateException in case the value resolved is {@literal null}.
	 */
	default T resolveRequiredAggregate() {

		T result = resolveAggregate();

		if (result == null) {
			throw new IllegalStateException("Resolving the aggregate resulted in null");
		}

		return result;
	}

	/**
	 * Resolves the underlying URI into an aggregate identifier, potentially applying the registered identifier extractor.
	 *
	 * @return will never be {@literal null}.
	 * @throws IllegalStateException in case the value resolved is {@literal null}.
	 */
	default ID resolveRequiredId() {

		ID result = resolveId();

		if (result == null) {
			throw new IllegalStateException("Resolving the aggregate identifier resulted in null");
		}

		return result;
	}
}
