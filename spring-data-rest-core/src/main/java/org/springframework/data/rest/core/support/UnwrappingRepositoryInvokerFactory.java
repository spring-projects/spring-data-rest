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
package org.springframework.data.rest.core.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link RepositoryInvokerFactory} that wraps the {@link RepositoryInvokerFactory} returned by the delegate with one
 * that automatically unwraps JDK 8 {@link Optional} and Guava {@link com.google.common.base.Optional}s.
 *
 * @author Oliver Gierke
 */
public class UnwrappingRepositoryInvokerFactory implements RepositoryInvokerFactory {

	private final RepositoryInvokerFactory delegate;
	private final PluginRegistry<EntityLookup<?>, Class<?>> lookups;

	/**
	 * @param delegate must not be {@literal null}.
	 * @param lookups must not be {@literal null}.
	 */
	public UnwrappingRepositoryInvokerFactory(RepositoryInvokerFactory delegate,
			List<? extends EntityLookup<?>> lookups) {

		Assert.notNull(delegate, "Delegate RepositoryInvokerFactory must not be null!");
		Assert.notNull(lookups, "EntityLookups must not be null!");

		this.delegate = delegate;
		this.lookups = PluginRegistry.of(lookups);
	}

	@Override
	public RepositoryInvoker getInvokerFor(Class<?> domainType) {

		Optional<EntityLookup<?>> lookup = lookups.getPluginFor(domainType);

		return new UnwrappingRepositoryInvoker(delegate.getInvokerFor(domainType), lookup);
	}

	/**
	 * {@link RepositoryInvoker} that post-processes invocations of {@link RepositoryInvoker#invokeFindOne(Serializable)}
	 * and {@link #invokeQueryMethod(Method, MultiValueMap, Pageable, Sort)} using the given {@link Converter}s.
	 *
	 * @author Oliver Gierke
	 */
	private static class UnwrappingRepositoryInvoker implements RepositoryInvoker {

		private final RepositoryInvoker delegate;
		private final Optional<EntityLookup<?>> lookup;

		public UnwrappingRepositoryInvoker(RepositoryInvoker delegate, Optional<EntityLookup<?>> lookup) {

			Assert.notNull(delegate, "Delegate RepositoryInvoker must not be null!");
			Assert.notNull(lookup, "EntityLookup must not be null!");

			this.delegate = delegate;
			this.lookup = lookup;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Optional<T> invokeFindById(Object id) {

			return lookup.isPresent() //
					? (Optional<T>) lookup.flatMap(it -> it.lookupEntity(id)) //
					: delegate.invokeFindById(id);
		}

		@Override
		public Optional<Object> invokeQueryMethod(Method method, MultiValueMap<String, ? extends Object> parameters,
				Pageable pageable, Sort sort) {
			return delegate.invokeQueryMethod(method, parameters, pageable, sort);
		}

		@Override
		public boolean hasDeleteMethod() {
			return delegate.hasDeleteMethod();
		}

		@Override
		public boolean hasFindAllMethod() {
			return delegate.hasFindAllMethod();
		}

		@Override
		public boolean hasFindOneMethod() {
			return delegate.hasFindOneMethod();
		}

		@Override
		public boolean hasSaveMethod() {
			return delegate.hasSaveMethod();
		}

		@Override
		public void invokeDeleteById(Object id) {
			delegate.invokeDeleteById(id);
		}

		@Override
		public Iterable<Object> invokeFindAll(Pageable pageable) {
			return delegate.invokeFindAll(pageable);
		}

		@Override
		public Iterable<Object> invokeFindAll(Sort sort) {
			return delegate.invokeFindAll(sort);
		}

		@Override
		public <T> T invokeSave(T object) {
			return delegate.invokeSave(object);
		}
	}
}
