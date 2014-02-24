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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Unit tests for {@link SpelEvaluatingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SpelEvaluatingMethodInterceptorUnitTests {

	@Mock MethodInterceptor delegate;
	@Mock MethodInvocation invocation;

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void invokesMethodOnTarget() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("propertyFromTarget"));

		MethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(), null);

		assertThat(interceptor.invoke(invocation), is((Object) "property"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void invokesMethodOnBean() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("invokeBean"));

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("someBean", new SomeBean());

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(), factory);

		assertThat(interceptor.invoke(invocation), is((Object) "value"));
	}

	interface Projection {

		@Value("#{target.property}")
		String propertyFromTarget();

		@Value("#{@someBean.value}")
		String invokeBean();
	}

	static class Target {

		public String getProperty() {
			return "property";
		}
	}

	static class SomeBean {

		public String getValue() {
			return "value";
		}
	}
}
