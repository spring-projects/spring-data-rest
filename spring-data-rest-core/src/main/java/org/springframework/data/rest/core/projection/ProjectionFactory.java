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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oliver Gierke
 */
public class ProjectionFactory {

	public <T> T createProjectionProxy(Object source, Class<T> projectionType) {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(source);
		factory.setTargetClass(projectionType);

		if (projectionType.isInterface()) {
			factory.setInterfaces(projectionType);
		}

		factory.addAdvice(new PropertyInvocationDelegationMethodInterceptor(source));

		return (T) factory.getProxy();
	}

	private class PropertyInvocationDelegationMethodInterceptor implements MethodInterceptor {

		private final BeanWrapper target;

		/**
		 * @param target
		 */
		private PropertyInvocationDelegationMethodInterceptor(Object target) {
			this.target = new DirectFieldAccessFallbackBeanWrapper(target);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);

			Class<?> propertyType = getPropertyTypeFor(descriptor.getName());

			if (descriptor.getPropertyType().equals(propertyType)) {
				return target.getPropertyValue(descriptor.getName());
			}

			Object result = target.getPropertyValue(descriptor.getName());
			return ProjectionFactory.this.createProjectionProxy(result, descriptor.getPropertyType());
		}

		private Class<?> getPropertyTypeFor(String property) {

			try {
				return target.getPropertyDescriptor(property).getPropertyType();
			} catch (InvalidPropertyException o_O) {

				Field field = ReflectionUtils.findField(target.getWrappedInstance().getClass(), property);

				if (field == null) {
					throw new IllegalArgumentException("No property named " + property + " found on "
							+ target.getWrappedClass().getName() + "!");
				}

				return field.getType();
			}
		}
	}

	/**
	 * Custom extension of {@link BeanWrapperImpl} that falls back to direct field access in case the object or type being
	 * wrapped does not use accessor methods.
	 * 
	 * @author Oliver Gierke
	 */
	private static class DirectFieldAccessFallbackBeanWrapper extends BeanWrapperImpl {

		public DirectFieldAccessFallbackBeanWrapper(Object entity) {
			super(entity);
		}

		public DirectFieldAccessFallbackBeanWrapper(Class<?> type) {
			super(type);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.BeanWrapperImpl#getPropertyValue(java.lang.String)
		 */
		@Override
		public Object getPropertyValue(String propertyName) {
			try {
				return super.getPropertyValue(propertyName);
			} catch (NotReadablePropertyException e) {
				Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
				ReflectionUtils.makeAccessible(field);
				return ReflectionUtils.getField(field, getWrappedInstance());
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.BeanWrapperImpl#setPropertyValue(java.lang.String, java.lang.Object)
		 */
		@Override
		public void setPropertyValue(String propertyName, Object value) {
			try {
				super.setPropertyValue(propertyName, value);
			} catch (NotWritablePropertyException e) {
				Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, getWrappedInstance(), value);
			}
		}
	}
}
