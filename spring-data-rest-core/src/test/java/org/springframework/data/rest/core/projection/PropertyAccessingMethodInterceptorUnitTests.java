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
import org.springframework.beans.NotReadablePropertyException;

/**
 * Unit tests for {@link PropertyAccessingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyAccessingMethodInterceptorUnitTests {

	@Mock MethodInvocation invocation;

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void triggersPropertyAccessOnTarget() throws Throwable {

		Source source = new Source();
		source.firstname = "Dave";

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("getFirstname"));
		MethodInterceptor interceptor = new PropertyAccessingMethodInterceptor(source);

		assertThat(interceptor.invoke(invocation), is((Object) "Dave"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = NotReadablePropertyException.class)
	public void throwsAppropriateExceptionIfThePropertyCannotBeFound() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("getLastname"));
		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void forwardsObjectMethodInvocation() throws Throwable {

		when(invocation.getMethod()).thenReturn(Object.class.getMethod("toString"));
		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	static class Source {

		String firstname;
	}

	interface Projection {

		String getFirstname();

		String getLastname();
	}
}
