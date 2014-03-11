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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Method interceptor to forward a delegation to bean property accessor methods to the property of a given target.
 * 
 * @author Oliver Gierke
 */
class PropertyAccessingMethodInterceptor implements MethodInterceptor {

	private final BeanWrapper target;

	/**
	 * Creates a new {@link PropertyAccessingMethodInterceptor} for the given target object.
	 * 
	 * @param target must not be {@literal null}.
	 */
	public PropertyAccessingMethodInterceptor(Object target) {

		Assert.notNull(target, "Proxy target must not be null!");
		this.target = new DirectFieldAccessFallbackBeanWrapper(target);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (ReflectionUtils.isObjectMethod(method)) {
			return invocation.proceed();
		}

		PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);

		if (descriptor == null) {
			throw new IllegalStateException("Invoked method is not a property accessor!");
		}

		return target.getPropertyValue(descriptor.getName());
	}
}
