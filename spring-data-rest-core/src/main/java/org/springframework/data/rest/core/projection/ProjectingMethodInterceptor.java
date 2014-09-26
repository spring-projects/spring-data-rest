/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.core.projection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.CollectionFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link MethodInterceptor} to delegate the invocation to a different {@link MethodInterceptor} but creating a
 * projecting proxy in case the returned value is not of the return type of the invoked method.
 * 
 * @author Oliver Gierke
 */
class ProjectingMethodInterceptor implements MethodInterceptor {

	private final ProjectionFactory factory;
	private final MethodInterceptor delegate;

	/**
	 * Creates a new {@link ProjectingMethodInterceptor} using the given {@link ProjectionFactory} and delegate
	 * {@link MethodInterceptor}.
	 * 
	 * @param factory the {@link ProjectionFactory} to use to create projections if types do not match.
	 * @param delegate the {@link MethodInterceptor} to trigger to create the source value.
	 */
	public ProjectingMethodInterceptor(ProjectionFactory factory, MethodInterceptor delegate) {

		Assert.notNull(factory, "ProjectionFactory must not be null!");
		Assert.notNull(delegate, "Delegate MethodInterceptor must not be null!");

		this.factory = factory;
		this.delegate = delegate;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Object result = delegate.invoke(invocation);

		if (result == null) {
			return null;
		}

		TypeInformation<?> type = ClassTypeInformation.fromReturnTypeOf(invocation.getMethod());

		if (type.isCollectionLike()) {
			return projectCollectionElements(asCollection(result), type);
		} else if (type.isMap()) {
			return projectMapValues((Map<?, ?>) result, type);
		} else {
			return getProjection(result, type.getType());
		}
	}

	/**
	 * Creates projections of the given {@link Collection}'s elements if necessary and returns a new collection containing
	 * the projection results.
	 * 
	 * @param sources must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private Collection<Object> projectCollectionElements(Collection<?> sources, TypeInformation<?> type) {

		Collection<Object> result = CollectionFactory.createCollection(type.getType(), sources.size());

		for (Object source : sources) {
			result.add(getProjection(source, type.getComponentType().getType()));
		}

		return result;
	}

	/**
	 * Creates projections of the given {@link Map}'s values if necessary and returns an new {@link Map} with the handled
	 * values.
	 * 
	 * @param sources must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private Map<Object, Object> projectMapValues(Map<?, ?> sources, TypeInformation<?> type) {

		Map<Object, Object> result = CollectionFactory.createMap(type.getType(), sources.size());

		for (Entry<?, ?> source : sources.entrySet()) {
			result.put(source.getKey(), getProjection(source.getValue(), type.getMapValueType().getType()));
		}

		return result;
	}

	private Object getProjection(Object result, Class<?> returnType) {
		return ClassUtils.isAssignable(returnType, result.getClass()) ? result : factory.createProjection(result,
				returnType);
	}

	/**
	 * Turns the given value into a {@link Collection}. Will create an empty {@link Collection} for {@literal null}, turn
	 * an array iinto a collection an wrap all other values into a single-element collection.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	private static Collection<?> asCollection(Object source) {

		if (source == null) {
			return Collections.emptySet();
		} else if (source instanceof Collection) {
			return (Collection<?>) source;
		} else if (source.getClass().isArray()) {
			return Arrays.asList((Object[]) source);
		} else {
			return Collections.singleton(source);
		}
	}
}
