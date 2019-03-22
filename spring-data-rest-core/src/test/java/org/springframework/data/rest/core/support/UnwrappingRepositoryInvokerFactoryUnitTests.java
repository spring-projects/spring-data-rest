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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Unit tests for {@link UnwrappingRepositoryInvokerFactory}.
 * 
 * @author Oliver Gierke
 */
@RunWith(Parameterized.class)
public class UnwrappingRepositoryInvokerFactoryUnitTests {

	static final Object REFERENCE = new Object();

	RepositoryInvokerFactory delegate = mock(RepositoryInvokerFactory.class);
	RepositoryInvoker invoker = mock(RepositoryInvoker.class);

	RepositoryInvokerFactory factory;
	Method method;

	public @Parameter(value = 0) Object source;
	public @Parameter(value = 1) Matcher<Object> value;

	@Before
	public void setUp() throws Exception {

		when(delegate.getInvokerFor(Object.class)).thenReturn(invoker);

		this.factory = new UnwrappingRepositoryInvokerFactory(delegate);
		this.method = Object.class.getMethod("toString");
	}

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { //
				{ Optional.empty(), is(nullValue()) }, //
						{ Optional.of(REFERENCE), is(REFERENCE) }, //
						{ com.google.common.base.Optional.absent(), is(nullValue()) }, //
						{ com.google.common.base.Optional.of(REFERENCE), is(REFERENCE) } //
				});
	}

	/**
	 * @see DATAREST-511
	 */
	@Test
	public void unwrapsValuesForFindOne() {
		assertFindOneValueForSource(source, value);
	}

	/**
	 * @see DATAREST-511
	 */
	@Test
	public void unwrapsValuesForQuery() {
		assertQueryValueForSource(source, value);
	}

	private void assertFindOneValueForSource(Object source, Matcher<Object> value) {

		when(invoker.invokeFindOne(1L)).thenReturn(source);
		assertThat(factory.getInvokerFor(Object.class).invokeFindOne(1L), value);
	}

	private void assertQueryValueForSource(Object source, Matcher<Object> value) {

		when(invoker.invokeQueryMethod(method, new LinkedMultiValueMap<String, Object>(), null, null)).thenReturn(source);
		assertThat(
				factory.getInvokerFor(Object.class).invokeQueryMethod(method, new LinkedMultiValueMap<String, Object>(), null,
						null), value);
	}
}
