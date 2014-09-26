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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
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

		Class<?> returnType = invocation.getMethod().getReturnType();

		return ClassUtils.isAssignable(returnType, result.getClass()) ? result : factory.createProjection(result,
				returnType);
	}
}
