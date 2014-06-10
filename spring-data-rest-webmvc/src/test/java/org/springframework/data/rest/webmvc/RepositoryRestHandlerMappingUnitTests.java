/*
 * Copyright 2013-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * Unit tests for {@link RepositoryRestHandlerMapping}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryRestHandlerMappingUnitTests {

	static final ApplicationContext CONTEXT = new AnnotationConfigApplicationContext(RepositoryRestMvcConfiguration.class);

	@Mock ResourceMappings mappings;

	RepositoryRestConfiguration configuration;
	RepositoryRestHandlerMapping handlerMapping;
	MockHttpServletRequest mockRequest;
	Method listEntitiesMethod, rootHandlerMethod;

	@Before
	public void setUp() throws Exception {

		configuration = new RepositoryRestConfiguration();

		handlerMapping = new RepositoryRestHandlerMapping(mappings, configuration);
		handlerMapping.setApplicationContext(CONTEXT);
		handlerMapping.afterPropertiesSet();

		mockRequest = new MockHttpServletRequest();

		listEntitiesMethod = RepositoryEntityController.class.getMethod("getCollectionResource",
				RootResourceInformation.class, DefaultedPageable.class, Sort.class, PersistentEntityResourceAssembler.class);
		rootHandlerMethod = RepositoryController.class.getMethod("listRepositories");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappings() {
		new RepositoryRestHandlerMapping(null, configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConfiguration() {
		new RepositoryRestHandlerMapping(mappings, null);
	}

	/**
	 * @see DATAREST-111
	 */
	@Test
	public void returnsNullForUriNotMapped() throws Exception {
		assertThat(handlerMapping.lookupHandlerMethod("/foo", mockRequest), is(nullValue()));
	}

	/**
	 * @see DATAREST-111
	 */
	@Test
	public void looksUpRepositoryEntityControllerMethodCorrectly() throws Exception {

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/people");

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/people", mockRequest);

		assertThat(method, is(notNullValue()));
		assertThat(method.getMethod(), is(listEntitiesMethod));
	}

	/**
	 * @see DATAREST-292
	 */
	@Test
	public void returnsRepositoryHandlerMethodWithBaseUriConfigured() throws Exception {

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/base/people");

		configuration.setBaseUri(URI.create("base"));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people", mockRequest);

		assertThat(method, is(notNullValue()));
		assertThat(method.getMethod(), is(listEntitiesMethod));
	}

	/**
	 * @see DATAREST-292
	 */
	@Test
	public void returnsRootHandlerMethodWithBaseUriConfigured() throws Exception {

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/base");

		configuration.setBaseUri(URI.create("base"));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base", mockRequest);

		assertThat(method, is(notNullValue()));
		assertThat(method.getMethod(), is(rootHandlerMethod));
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void returnsRepositoryHandlerMethodForAbsoluteBaseUri() throws Exception {

		String baseUri = "http://localhost/base";

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", baseUri.concat("/people/"));

		configuration.setBaseUri(URI.create(baseUri));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people/", mockRequest);

		assertThat(method, is(notNullValue()));
		assertThat(method.getMethod(), is(listEntitiesMethod));
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void returnsRepositoryHandlerMethodForAbsoluteBaseUriWithServletMapping() throws Exception {

		String baseUri = "http://localhost/base";
		String uri = baseUri.concat("/people/");

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		configuration.setBaseUri(URI.create(baseUri));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people/", mockRequest);

		assertThat(method, is(notNullValue()));
		assertThat(method.getMethod(), is(listEntitiesMethod));
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void refrainsFromMappingIfTheRequestDoesNotPointIntoAbsolutelyDefinedUriSpace() throws Exception {

		String baseUri = "http://localhost/base";
		String uri = baseUri.concat("/people/");

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		configuration.setBaseUri(URI.create(baseUri));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/people", mockRequest);

		assertThat(method, is(nullValue()));
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void refrainsFromMappingWhenUrisDontMatch() throws Exception {

		String baseUri = "foo";
		String uri = baseUri.concat("/people");

		when(mappings.exportsTopLevelResourceFor("people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		configuration.setBaseUri(URI.create(baseUri));

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/people", mockRequest);

		assertThat(method, is(nullValue()));
	}
}
