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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.webmvc.ResourceType.*;
import static org.springframework.http.HttpMethod.*;

import org.atteo.evo.inflector.English;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Unit tests for {@link RootResourceInformation}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RootResourceInformationUnitTests {

	@Mock ResourceMetadata metadata;
	@Mock PersistentEntity<?, ?> entity;
	RepositoryInvoker invoker;

	RootResourceInformation information;

	@Before
	public void setUp() {

		when(metadata.isExported()).thenReturn(true);
		this.invoker = mock(RepositoryInvoker.class, new DefaultBooleanToTrue());
		this.information = new RootResourceInformation(metadata, entity, invoker);
	}

	/**
	 * @see DATAREST-217, DATAREST-330
	 */
	@Test
	public void defaultsSupportedHttpMethodsForItemResource() {

		assertThat(information.getSupportedMethods(ResourceType.ITEM), hasItems(GET, PUT, PATCH, DELETE, OPTIONS));
		assertThat(information.getSupportedMethods(ResourceType.ITEM), not(hasItems(POST)));

		assertThat(information.getSupportedMethods(COLLECTION), hasItems(GET, POST, OPTIONS));
		assertThat(information.getSupportedMethods(COLLECTION), not(hasItems(PUT, PATCH, DELETE)));

	}

	/**
	 * @see DATAREST-217
	 */
	@Test
	public void doesNotSupportGetOnItemResourceIfFindOneIsNotExported() {

		when(invoker.exposesFindOne()).thenReturn(false);
		assertThat(information.supports(GET, ITEM), is(false));
	}

	/**
	 * @see DATAREST-217
	 */
	@Test
	public void doesNotSupportDeleteOnItemResourceIfDeleteIsNotExported() {

		when(invoker.exposesDelete()).thenReturn(false);
		assertThat(information.supports(DELETE, ITEM), is(false));
	}

	/**
	 * @see DATAREST-217
	 */
	@Test
	public void doesNotSupportPutOnItemResourceIfSaveIsNotExported() {

		when(invoker.exposesSave()).thenReturn(false);
		assertThat(information.supports(POST, ITEM), is(false));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void supportsHeadIfFindAllIsExposed() {

		when(invoker.exposesFindAll()).thenReturn(true);
		assertThat(information.supports(HEAD, COLLECTION), is(true));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void doesNotSupportHeadIfFindAllIsNotExposed() {

		when(invoker.exposesFindAll()).thenReturn(false);
		assertThat(information.supports(HEAD, COLLECTION), is(false));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void supportsHeadIfFindOneIsExposed() {

		when(invoker.exposesFindOne()).thenReturn(true);
		assertThat(information.supports(HEAD, ITEM), is(true));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void doesNotSupportHeadIfFindOneIsNotExposed() {

		when(invoker.exposesFindOne()).thenReturn(false);
		assertThat(information.supports(HEAD, ITEM), is(false));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void throwsExceptionOnVerificationIfResourceIsNotExported() throws HttpRequestMethodNotSupportedException {

		when(metadata.isExported()).thenReturn(false);
		information.verifySupportedMethod(HEAD, COLLECTION);
	}

	/**
	 * Helper class to default boolean methods to return {@literal true} instead of {@literal false} by default.
	 * 
	 * @author Oliver Gierke
	 */
	static class DefaultBooleanToTrue implements Answer<Object> {

		private static final Answer<Object> DEFAULT = Mockito.RETURNS_DEFAULTS;

		/* 
		 * (non-Javadoc)
		 * @see org.mockito.stubbing.Answer#answer(org.mockito.invocation.InvocationOnMock)
		 */
		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {

			Class<?> returnType = invocation.getMethod().getReturnType();
			return returnType.equals(Boolean.class) || returnType.equals(boolean.class) ? true : DEFAULT.answer(invocation);
		}
	}
}
