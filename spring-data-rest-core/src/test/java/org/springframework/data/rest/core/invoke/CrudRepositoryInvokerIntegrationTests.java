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
package org.springframework.data.rest.core.invoke;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.AbstractIntegrationTests;
import org.springframework.data.rest.core.domain.jpa.Order;
import org.springframework.data.rest.core.domain.jpa.OrderRepository;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.domain.jpa.PersonRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Intgration tests for {@link CrudRepositoryInvoker}.
 * 
 * @author Oliver Gierke
 */
public class CrudRepositoryInvokerIntegrationTests extends AbstractIntegrationTests {

	@Autowired ApplicationContext context;
	@Autowired PersonRepository personRepository;
	@Autowired OrderRepository orderRepository;

	/**
	 * @see DATAREST-216
	 */
	@Test
	public void invokesRedeclaredSave() {

		RepositoryInvoker invoker = getInvokerFor(orderRepository, OrderRepository.class);

		Person person = personRepository.findOne(1L);
		invoker.invokeSave(new Order(person));
	}

	/**
	 * @see DATAREST-216
	 */
	@Test
	public void invokesRedeclaredFindOne() {

		Person person = personRepository.findOne(1L);
		Order order = orderRepository.save(new Order(person));

		RepositoryInvoker invoker = getInvokerFor(orderRepository, OrderRepository.class);
		invoker.invokeFindOne(order.getId());
	}

	/**
	 * @see DATAREST-216
	 */
	@Test
	public void invokesDeleteOnCrudRepository() {

		Person person = personRepository.findOne(1L);
		Order order = orderRepository.save(new Order(person));

		RepositoryInvoker invoker = getInvokerFor(orderRepository, CrudRepository.class);
		invoker.invokeDelete(order.getId());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private RepositoryInvoker getInvokerFor(Object repository, Class<?> expectedType) {

		Object proxy = getVerifyingProxy(repository, expectedType);
		Repositories repositories = new Repositories(context);
		ConversionService conversionService = new DefaultFormattingConversionService();

		return new CrudRepositoryInvoker((CrudRepository) proxy, repositories.getRepositoryInformationFor(Order.class),
				conversionService);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getVerifyingProxy(T target, Class<?> expectedType) {

		ProxyFactory factory = new ProxyFactory();
		factory.setInterfaces(target.getClass().getInterfaces());
		factory.setTarget(target);
		factory.addAdvice(new VerifyingMethodInterceptor(expectedType));

		return (T) factory.getProxy();
	}

	/**
	 * {@link MethodInterceptor} to verifiy the invocation was triggered on the given type.
	 * 
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("rawtypes")
	private static final class VerifyingMethodInterceptor implements MethodInterceptor {

		private final Class expectedInvocationTarget;

		public VerifyingMethodInterceptor(Class<?> expectedInvocationTarget) {
			this.expectedInvocationTarget = expectedInvocationTarget;
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Class<?> type = invocation.getMethod().getDeclaringClass();

			assertThat("Expected method invocation on " + expectedInvocationTarget + " but was invoked on " + type + "!",
					type, is(equalTo(expectedInvocationTarget)));

			return invocation.proceed();
		}
	}
}
