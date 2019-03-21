/*
 * Copyright 2015 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link RepositoryInvokerFactory} that wraps the {@link RepositoryInvokerFactory} returned by the delegate with one
 * that automatically unwraps JDK 8 {@link Optional} and Guava {@link com.google.common.base.Optional}s.
 * 
 * @author Oliver Gierke
 */
public class UnwrappingRepositoryInvokerFactory implements RepositoryInvokerFactory {

	private static final List<Converter<Object, Object>> CONVERTERS;

	static {

		List<Converter<Object, Object>> converters = new ArrayList<Converter<Object, Object>>();
		ClassLoader classLoader = UnwrappingRepositoryInvokerFactory.class.getClassLoader();

		// Add unwrapper for Java 8 Optional

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {
			converters.add(new Converter<Object, Object>() {
				@Override
				public Object convert(Object source) {
					return source instanceof Optional ? ((Optional<?>) source).orElse(null) : source;
				}
			});
		}

		// Add unwrapper for Guava Optional

		if (ClassUtils.isPresent("com.google.common.base.Optional", classLoader)) {

			converters.add(new Converter<Object, Object>() {
				@Override
				public Object convert(Object source) {
					return source instanceof com.google.common.base.Optional
							? ((com.google.common.base.Optional<?>) source).orNull() : source;
				}
			});
		}

		CONVERTERS = Collections.unmodifiableList(converters);
	}

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
		this.lookups = OrderAwarePluginRegistry.create(lookups);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInvokerFactory#getInvokerFor(java.lang.Class)
	 */
	@Override
	public RepositoryInvoker getInvokerFor(Class<?> domainType) {

		EntityLookup<?> lookup = lookups.getPluginFor(domainType);

		return new UnwrappingRepositoryInvoker(delegate.getInvokerFor(domainType), CONVERTERS, lookup);
	}

	/**
	 * {@link RepositoryInvoker} that post-processes invocations of {@link RepositoryInvoker#invokeFindOne(Serializable)}
	 * and {@link #invokeQueryMethod(Method, MultiValueMap, Pageable, Sort)} using the given {@link Converter}s.
	 * 
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static class UnwrappingRepositoryInvoker implements RepositoryInvoker {

		private final @NonNull RepositoryInvoker delegate;
		private final @NonNull Collection<Converter<Object, Object>> converters;
		private final EntityLookup<?> lookup;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeFindOne(java.io.Serializable)
		 */
		public <T> T invokeFindOne(Serializable id) {
			return postProcess(lookup != null ? lookup.lookupEntity(id) : delegate.invokeFindOne(id));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeQueryMethod(java.lang.reflect.Method, java.util.Map, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort)
		 */
		@Override
		@SuppressWarnings("deprecation")
		public Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort) {
			return postProcess(delegate.invokeQueryMethod(method, parameters, pageable, sort));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeQueryMethod(java.lang.reflect.Method, org.springframework.util.MultiValueMap, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort)
		 */
		@Override
		public Object invokeQueryMethod(Method method, MultiValueMap<String, ? extends Object> parameters,
				Pageable pageable, Sort sort) {
			return postProcess(delegate.invokeQueryMethod(method, parameters, pageable, sort));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvocationInformation#hasDeleteMethod()
		 */
		@Override
		public boolean hasDeleteMethod() {
			return delegate.hasDeleteMethod();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvocationInformation#hasFindAllMethod()
		 */
		@Override
		public boolean hasFindAllMethod() {
			return delegate.hasFindAllMethod();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvocationInformation#hasFindOneMethod()
		 */
		@Override
		public boolean hasFindOneMethod() {
			return delegate.hasFindOneMethod();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvocationInformation#hasSaveMethod()
		 */
		@Override
		public boolean hasSaveMethod() {
			return delegate.hasSaveMethod();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeDelete(java.io.Serializable)
		 */
		@Override
		public void invokeDelete(Serializable id) {
			delegate.invokeDelete(id);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Pageable)
		 */
		@Override
		public Iterable<Object> invokeFindAll(Pageable pageable) {
			return delegate.invokeFindAll(pageable);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Sort)
		 */
		@Override
		public Iterable<Object> invokeFindAll(Sort sort) {
			return delegate.invokeFindAll(sort);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.support.RepositoryInvoker#invokeSave(java.lang.Object)
		 */
		@Override
		public <T> T invokeSave(T object) {
			return delegate.invokeSave(object);
		}

		/**
		 * Invokes the configured converters for the given result.
		 * 
		 * @param result can be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private <T> T postProcess(Object result) {

			for (Converter<Object, Object> converter : converters) {
				result = converter.convert(result);
			}

			return (T) result;
		}
	}
}
