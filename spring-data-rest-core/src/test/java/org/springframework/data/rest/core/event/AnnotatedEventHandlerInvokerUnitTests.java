/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.rest.core.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.event.AnnotatedEventHandlerInvoker.EventHandlerMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AnnotatedEventHandlerInvoker}.
 *
 * @author Oliver Gierke
 * @author Fabian Trampusch
 * @author Joseph Valerio
 */
public class AnnotatedEventHandlerInvokerUnitTests {

	@Test // DATAREST-582
	@SuppressWarnings("unchecked")
	public void doesNotDiscoverMethodsOnProxyClasses() {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new Sample());
		factory.setProxyTargetClass(true);

		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(factory.getProxy(), "proxy");

		MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod> methods = (MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod>) ReflectionTestUtils
				.getField(invoker, "handlerMethods");

		assertThat(methods.get(BeforeCreateEvent.class)).hasSize(1);
	}

	@Test // DATAREST-606
	public void invokesPrivateEventHandlerMethods() {

		SampleWithPrivateHandler sampleHandler = new SampleWithPrivateHandler();

		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(sampleHandler, "sampleHandler");

		invoker.onApplicationEvent(new BeforeCreateEvent(new Person("Dave", "Matthews")));

		assertThat(sampleHandler.wasCalled).isTrue();
	}

	@Test // DATAREST-970
	public void invokesEventHandlerInOrderMethods() {

		SampleOrderEventHandler1 orderHandler1 = new SampleOrderEventHandler1();
		SampleOrderEventHandler2 orderHandler2 = new SampleOrderEventHandler2();

		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(orderHandler1, "orderHandler1");
		invoker.postProcessAfterInitialization(orderHandler2, "orderHandler2");

		invoker.onApplicationEvent(new BeforeCreateEvent(new Person("Dave", "Matthews")));

		assertThat(orderHandler1.wasCalled).isTrue();
		assertThat(orderHandler2.wasCalled).isTrue();

		assertThat(orderHandler1.timestamp).isGreaterThan(orderHandler2.timestamp);
	}

	@Test // DATAREST-983
	public void invokesEventHandlerOnParentClass() {

		FirstEventHandler firstHandler = new FirstEventHandler();
		SecondEventHandler secondHandler = new SecondEventHandler();

		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(firstHandler, "firstHandler");
		invoker.postProcessAfterInitialization(secondHandler, "secondHandler");

		invoker.onApplicationEvent(new BeforeCreateEvent(new FirstEntity()));
		invoker.onApplicationEvent(new BeforeCreateEvent(new SecondEntity()));

		assertThat(firstHandler.callCount).isEqualTo(1);
		assertThat(secondHandler.callCount).isEqualTo(1);
	}

	@Test // DATAREST-1075
	public void doesInvokeMethodOnlyOnceForMockitoSpy() {

		EventHandler handler = spy(new EventHandler());
		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(handler, "handler");

		Payload payload = new Payload();
		invoker.onApplicationEvent(new AfterCreateEvent(payload));

		verify(handler, times(1)).doAfterCreate(payload);
	}

	@Test // DATAREST-582
	public void invocesInterceptorForProxiedListener() {

		SampleInterceptor interceptor = new SampleInterceptor();

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new Sample());
		factory.addAdvice(interceptor);
		factory.setProxyTargetClass(true);

		AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
		invoker.postProcessAfterInitialization(factory.getProxy(), "proxy");

		invoker.onApplicationEvent(new BeforeCreateEvent(new Sample()));

		Method method = ReflectionUtils.findMethod(Sample.class, "method", Sample.class);

		assertThat(interceptor.invocations.get(method)).isEqualTo(1);
	}

	@RepositoryEventHandler
	static class Sample {

		@HandleBeforeCreate
		public void method(Sample sample) {}
	}

	@RepositoryEventHandler
	static class SampleWithPrivateHandler {

		boolean wasCalled = false;

		@HandleBeforeCreate
		private void method(Person sample) {
			wasCalled = true;
		}
	}

	@RepositoryEventHandler
	static class SampleOrderEventHandler1 {

		boolean wasCalled = false;
		long timestamp;

		@Order(2)
		@HandleBeforeCreate
		private void method(Person sample) {
			wasCalled = true;
			timestamp = System.nanoTime();
		}
	}

	@RepositoryEventHandler
	static class SampleOrderEventHandler2 {

		boolean wasCalled = false;
		long timestamp;

		@Order(1)
		@HandleBeforeCreate
		private void method(Person sample) {
			wasCalled = true;
			timestamp = System.nanoTime();
		}
	}

	// DATAREST-983

	static class AbstractBaseEntityEventHandler<T extends BaseEntity> {
		int callCount = 0;

		@HandleBeforeCreate
		private void method(T entity) {
			callCount += 1;
		}
	}

	@RepositoryEventHandler
	static class FirstEventHandler extends AbstractBaseEntityEventHandler<FirstEntity> {}

	@RepositoryEventHandler
	static class SecondEventHandler extends AbstractBaseEntityEventHandler<SecondEntity> {}

	static abstract class BaseEntity {}

	static class FirstEntity extends BaseEntity {}

	static class SecondEntity extends BaseEntity {}

	@RepositoryEventHandler
	static class EventHandler {

		@HandleAfterCreate
		public void doAfterCreate(Payload bar) {}
	}

	static class Payload {}

	static class SampleInterceptor implements MethodInterceptor {

		Map<Method, Integer> invocations = new HashMap<>();

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			invocations.compute(invocation.getMethod(), (method, value) -> value == null ? 1 : value++);

			return invocation.proceed();
		}
	}
}
