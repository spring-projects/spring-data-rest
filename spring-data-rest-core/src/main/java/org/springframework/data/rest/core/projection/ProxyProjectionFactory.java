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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.AnnotationDetectionMethodCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A {@link ProjectionFactory} to create JDK proxies to back interfaces and handle method invocations on them. By
 * default two different kinds of methods are supported:
 * <ol>
 * <li>Bean property accessor methods - invocations will be delegated into a property lookup on the target instance.</li>
 * <li>Arbitrary methods annotated with {@link Value} to contain a SpEL expression, which will be evaluated on
 * invocation. The expressions can use {@code target} to refer to the proxy target.</li>
 * </ol>
 * In case the dlegating lookups result in an object of different type that the projection interface method's return
 * type, another projection will be created to transparently mitigate between the types.
 * 
 * @author Oliver Gierke
 */
public class ProxyProjectionFactory implements ProjectionFactory {

	private final Map<Class<?>, Boolean> typeCache = new HashMap<Class<?>, Boolean>();

	private BeanFactory beanFactory;

	/**
	 * Creates a new {@link ProxyProjectionFactory} using the given {@link BeanFactory}.
	 * 
	 * @param beanFactory can be {@literal null}. If {@literal null}, SpEL expressions at projection interfaces cannot use
	 *          bean references.
	 */
	public ProxyProjectionFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.projection.ProjectionFactory#createProjection(java.lang.Object, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T createProjection(Object source, Class<T> projectionType) {

		Assert.isTrue(projectionType.isInterface(), "Projection type must be an interface!");

		if (source == null) {
			return null;
		}

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(source);
		factory.setOpaque(true);
		factory.setInterfaces(projectionType, TargetClassAware.class);

		factory.addAdvice(new TargetClassAwareMethodInterceptor(source.getClass()));
		factory.addAdvice(getMethodInterceptor(source, projectionType));

		return (T) factory.getProxy();
	}

	/**
	 * Returns the {@link MethodInterceptor} to add to the proxy.
	 * 
	 * @param source must not be {@literal null}.
	 * @param projectionType must not be {@literal null}.
	 * @return
	 */
	private MethodInterceptor getMethodInterceptor(Object source, Class<?> projectionType) {

		MethodInterceptor propertyInvocationInterceptor = new PropertyAccessingMethodInterceptor(source);
		return new ProjectingMethodInterceptor(this, getSpelMethodInterceptorIfNecessary(source, projectionType,
				propertyInvocationInterceptor));
	}

	/**
	 * Inspects the given target type for methods with {@link Value} annotations and caches the result. Will create a
	 * {@link SpelEvaluatingMethodInterceptor} if an annotation was found or return the delegate as is if not.
	 * 
	 * @param source The backing source object.
	 * @param projectionType the proxy target type.
	 * @param delegate the root {@link MethodInterceptor}.
	 * @return
	 */
	private MethodInterceptor getSpelMethodInterceptorIfNecessary(Object source, Class<?> projectionType,
			MethodInterceptor delegate) {

		if (!typeCache.containsKey(projectionType)) {

			AnnotationDetectionMethodCallback<Value> callback = new AnnotationDetectionMethodCallback<Value>(Value.class);
			ReflectionUtils.doWithMethods(projectionType, callback);

			typeCache.put(projectionType, callback.hasFoundAnnotation());
		}

		return typeCache.get(projectionType) ? new SpelEvaluatingMethodInterceptor(delegate, source, beanFactory)
				: delegate;
	}

	/**
	 * Extension of {@link org.springframework.aop.TargetClassAware} to be able to ignore the getter on JSON rendering.
	 * 
	 * @author Oliver Gierke
	 */
	public static interface TargetClassAware extends org.springframework.aop.TargetClassAware {

		@JsonIgnore
		Class<?> getTargetClass();
	}

	/**
	 * Custom {@link MethodInterceptor} to expose the proxy target class even if we set
	 * {@link ProxyFactory#setOpaque(boolean)} to true to prevent properties on {@link Advised} to be rendered.
	 * 
	 * @author Oliver Gierke
	 */
	private static class TargetClassAwareMethodInterceptor implements MethodInterceptor {

		private static final Method GET_TARGET_CLASS_METHOD;
		private final Class<?> targetClass;

		static {
			try {
				GET_TARGET_CLASS_METHOD = TargetClassAware.class.getMethod("getTargetClass");
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * Creates a new {@link TargetClassAwareMethodInterceptor} with the given target class.
		 * 
		 * @param targetClass must not be {@literal null}.
		 */
		public TargetClassAwareMethodInterceptor(Class<?> targetClass) {

			Assert.notNull(targetClass, "Target class must not be null!");
			this.targetClass = targetClass;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			if (invocation.getMethod().equals(GET_TARGET_CLASS_METHOD)) {
				return targetClass;
			}

			return invocation.proceed();
		}
	}
}
