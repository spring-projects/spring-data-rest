/*
 * Copyright 2018-2022 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link HttpMethods} that expose methods to create different {@link ConfigurableHttpMethods}.
 *
 * @author Oliver Gierke
 * @since 3.1
 */
public class ConfigurableHttpMethods implements HttpMethods {

	public static final ConfigurableHttpMethods NONE = ConfigurableHttpMethods.of();
	public static final ConfigurableHttpMethods ALL = ConfigurableHttpMethods.of(HttpMethod.values());

	private final Collection<HttpMethod> methods;

	private ConfigurableHttpMethods(Collection<HttpMethod> methods) {

		Assert.notNull(methods, "HttpMethods must not be null");

		this.methods = methods;
	}

	static ConfigurableHttpMethods of(Collection<HttpMethod> methods) {
		return new ConfigurableHttpMethods(methods);
	}

	/**
	 * Creates a new {@link ConfigurableHttpMethods} of the given {@link HttpMethod}s.
	 *
	 * @param methods must not be {@literal null}.
	 * @return
	 */
	static ConfigurableHttpMethods of(HttpMethod... methods) {

		Assert.notNull(methods, "HttpMethods must not be null");

		return new ConfigurableHttpMethods(Arrays.stream(methods).collect(Collectors.toSet()));
	}

	/**
	 * Creates a new {@link ConfigurableHttpMethods} of the given {@link HttpMethods}.
	 *
	 * @param methods must not be {@literal null}.
	 * @return
	 */
	static ConfigurableHttpMethods of(HttpMethods methods) {

		Assert.notNull(methods, "HttpMethods must not be null");

		if (ConfigurableHttpMethods.class.isInstance(methods)) {
			return ConfigurableHttpMethods.class.cast(methods);
		}

		return new ConfigurableHttpMethods(methods.stream().collect(Collectors.toSet()));
	}

	/**
	 * Disables the given {@link HttpMethod}s.
	 *
	 * @param methods must not be {@literal null}.
	 * @return
	 */
	public ConfigurableHttpMethods disable(HttpMethod... methods) {

		Assert.notNull(methods, "HttpMethods must not be null");

		List<HttpMethod> toRemove = Arrays.asList(methods);

		return new ConfigurableHttpMethods(this.methods.stream() //
				.filter(it -> !toRemove.contains(it)) //
				.collect(Collectors.toSet()));
	}

	/**
	 * Enables the given {@link HttpMethod}s.
	 *
	 * @param methods must not be {@literal null}.
	 * @return
	 */
	public ConfigurableHttpMethods enable(HttpMethod... methods) {

		Assert.notNull(methods, "HttpMethods must not be null");

		List<HttpMethod> toAdd = Arrays.asList(methods);

		if (this.methods.containsAll(toAdd)) {
			return this;
		}

		return ConfigurableHttpMethods.of(Stream.concat(this.methods.stream(), toAdd.stream()).collect(Collectors.toSet()));
	}

	@Override
	public boolean contains(HttpMethod method) {

		Assert.notNull(method, "HTTP method must not be null");

		return methods.contains(method);
	}

	@Override
	public Iterator<HttpMethod> iterator() {
		return methods.iterator();
	}
}
