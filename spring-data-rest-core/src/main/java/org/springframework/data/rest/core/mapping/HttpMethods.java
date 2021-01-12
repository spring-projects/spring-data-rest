/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * A collection of {@link HttpMethod}s with some convenience methods to create alternate sets of those.
 *
 * @author Oliver Gierke
 */
public interface HttpMethods extends Streamable<HttpMethod> {

	public static HttpMethods none() {
		return ConfigurableHttpMethods.NONE;
	}

	/**
	 * Returns a new {@link HttpMethods} with the given {@link HttpMethod}s.
	 *
	 * @param methods must not be {@literal null}.
	 * @return
	 */
	public static HttpMethods of(Collection<HttpMethod> methods) {

		Assert.notNull(methods, "HTTP methods must not be null!");

		return ConfigurableHttpMethods.of(methods);
	}

	/**
	 * Returns whether the given {@link HttpMethod} is contained in the current {@link HttpMethods}.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	boolean contains(HttpMethod method);

	/**
	 * Returns an unmodifiable {@link Set} of all underlying {@link HttpMethod}s.
	 *
	 * @return
	 */
	default Set<HttpMethod> toSet() {
		return stream().collect(StreamUtils.toUnmodifiableSet());
	}

	/**
	 * Returns a new {@link HttpMethods} with the given {@link HttpMethod}s added.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	default HttpMethods and(HttpMethod... method) {
		return of(Stream.concat(stream(), Arrays.stream(method)).collect(Collectors.toSet()));
	}

	/**
	 * Returns a new {@link HttpMethods} instance with the given {@link HttpMethod}s removed.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	default HttpMethods butWithout(HttpMethod... method) {

		List<HttpMethod> toRemove = Arrays.asList(method);

		return of(stream().filter(it -> !toRemove.contains(it)).collect(Collectors.toSet()));
	}
}
