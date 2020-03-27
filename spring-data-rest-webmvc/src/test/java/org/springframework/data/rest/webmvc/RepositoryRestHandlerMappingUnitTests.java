/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Unit tests for {@link RepositoryRestHandlerMapping}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoryRestHandlerMappingUnitTests {

	static final AnnotationConfigWebApplicationContext CONTEXT = new AnnotationConfigWebApplicationContext();

	static {
		CONTEXT.register(RepositoryRestMvcConfiguration.class);
		CONTEXT.setServletContext(new MockServletContext());
		CONTEXT.refresh();
	}

	@Mock ResourceMappings mappings;
	@Mock ResourceMetadata resourceMetadata;
	@Mock Repositories repositories;

	RepositoryRestConfiguration configuration;
	RepositoryRestHandlerMapping handlerMapping;
	MockHttpServletRequest mockRequest;
	Method listEntitiesMethod, rootHandlerMethod;

	@Before
	public void setUp() throws Exception {

		configuration = new RepositoryRestConfiguration(new ProjectionDefinitionConfiguration(),
				new MetadataConfiguration(), mock(EnumTranslationConfiguration.class));

		handlerMapping = new RepositoryRestHandlerMapping(mappings, configuration, repositories);
		handlerMapping.setApplicationContext(CONTEXT);

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

	@Test // DATAREST-111
	public void returnsNullForUriNotMapped() throws Exception {

		handlerMapping.afterPropertiesSet();
		assertThat(handlerMapping.lookupHandlerMethod("/foo", mockRequest)).isNull();
	}

	@Test // DATAREST-111
	public void looksUpRepositoryEntityControllerMethodCorrectly() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/people");

		handlerMapping.afterPropertiesSet();
		HandlerMethod method = handlerMapping.lookupHandlerMethod("/people", mockRequest);

		assertThat(method).isNotNull();
		assertThat(method.getMethod()).isEqualTo(listEntitiesMethod);
	}

	@Test // DATAREST-292
	public void returnsRepositoryHandlerMethodWithBaseUriConfigured() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/base/people");

		configuration.setBasePath("/base");
		handlerMapping.afterPropertiesSet();

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people", mockRequest);

		assertThat(method).isNotNull();
		assertThat(method.getMethod()).isEqualTo(listEntitiesMethod);
	}

	@Test // DATAREST-292
	public void returnsRootHandlerMethodWithBaseUriConfigured() throws Exception {

		mockRequest = new MockHttpServletRequest("GET", "/base");

		configuration.setBasePath("/base");
		handlerMapping.afterPropertiesSet();

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base", mockRequest);

		assertThat(method).isNotNull();
		assertThat(method.getMethod()).isEqualTo(rootHandlerMethod);
	}

	@Test // DATAREST-276
	public void returnsRepositoryHandlerMethodForAbsoluteBaseUri() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/base/people/");

		configuration.setBasePath("/base");
		handlerMapping.afterPropertiesSet();

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people/", mockRequest);

		assertThat(method).isNotNull();
		assertThat(method.getMethod()).isEqualTo(listEntitiesMethod);
	}

	@Test // DATAREST-276
	public void returnsRepositoryHandlerMethodForAbsoluteBaseUriWithServletMapping() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", "/base/people");
		mockRequest.setServletPath("/base/people");

		configuration.setBasePath("/base");
		handlerMapping.afterPropertiesSet();

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/base/people", mockRequest);

		assertThat(method).isNotNull();
		assertThat(method.getMethod()).isEqualTo(listEntitiesMethod);
	}

	@Test // DATAREST-276
	public void refrainsFromMappingIfTheRequestDoesNotPointIntoAbsolutelyDefinedUriSpace() throws Exception {

		mockRequest = new MockHttpServletRequest("GET", "/servlet-path");
		mockRequest.setServletPath("/servlet-path");

		configuration.setBasePath("/base");

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/servlet-path", mockRequest);

		assertThat(method).isNull();
	}

	@Test // DATAREST-276
	public void refrainsFromMappingWhenUrisDontMatch() throws Exception {

		String baseUri = "foo";
		String uri = baseUri.concat("/people");

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		configuration.setBasePath(baseUri);

		HandlerMethod method = handlerMapping.lookupHandlerMethod("/people", mockRequest);

		assertThat(method).isNull();
	}

	@Test // DATAREST-609
	public void rejectsUnexpandedUriTemplateWithNotFound() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);

		mockRequest = new MockHttpServletRequest("GET", "/people{?projection}");

		assertThat(handlerMapping.getHandler(mockRequest)).isNull();
	}

	@Test // DATAREST-1019
	public void resolvesCorsConfigurationFromRequestUri() {

		String uri = "/people";

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		when(mappings.iterator()).thenReturn(Collections.singleton(resourceMetadata).iterator());
		when(resourceMetadata.getPath()).thenReturn(new Path("/people"));

		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		handlerMapping.getCorsConfiguration(uri, mockRequest);

		verify(mappings).exportsTopLevelResourceFor("/people");
	}

	@Test // DATAREST-1019
	public void stripsBaseUriForCorsConfigurationResolution() {

		String baseUri = "/foo";
		String uri = baseUri.concat("/people");

		configuration.setBasePath(baseUri);

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);
		when(mappings.iterator()).thenReturn(Collections.singleton(resourceMetadata).iterator());
		when(resourceMetadata.getPath()).thenReturn(new Path("/people"));

		mockRequest = new MockHttpServletRequest("GET", uri);
		mockRequest.setServletPath(uri);

		handlerMapping.getCorsConfiguration(uri, mockRequest);

		verify(mappings).exportsTopLevelResourceFor("/people");
	}

	@Test // DATAREST-994
	public void twoArgumentConstructorDoesNotThrowException() {
		new RepositoryRestHandlerMapping(mappings, configuration);
	}

	@Test // DATAREST-1132
	public void detectsAnnotationsOnProxies() {

		Class<?> type = createProxy(new SomeController());

		HandlerMappingStub mapping = new HandlerMappingStub(mock(ResourceMappings.class),
				mock(RepositoryRestConfiguration.class));

		assertThat(mapping.isHandler(type)).isTrue();
	}

	@Test // DATAREST-1294
	public void exposesEffectiveRepositoryLookupPathAsRequestAttribute() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/people/search/findByLastnameLike");

		handlerMapping.afterPropertiesSet();
		handlerMapping.lookupHandlerMethod("/people/search/findByLastnameLike", mockRequest);

		assertThat(mockRequest.getAttribute(RepositoryRestHandlerMapping.EFFECTIVE_LOOKUP_PATH_ATTRIBUTE)) //
				.isInstanceOfSatisfying(PathPattern.class, it -> {
					assertThat(it.getPatternString()).isEqualTo("/people/search/{search}");
				});
	}

	@Test // DATAREST-1332
	public void handlesCorsPreflightRequestsProperly() throws Exception {

		when(mappings.exportsTopLevelResourceFor("/people")).thenReturn(true);

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/people/search");
		request.addHeader(HttpHeaders.ORIGIN, "test case");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

		handlerMapping.afterPropertiesSet();

		assertThatCode(() -> handlerMapping.lookupHandlerMethod("/people/search", request)) //
				.doesNotThrowAnyException();
	}

	private static Class<?> createProxy(Object source) {

		ProxyFactory factory = new ProxyFactory(source);
		Object proxy = factory.getProxy();

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();

		return proxy.getClass();
	}

	@RepositoryRestController
	static class SomeController {}

	static class HandlerMappingStub extends RepositoryRestHandlerMapping {

		public HandlerMappingStub(ResourceMappings mappings, RepositoryRestConfiguration configuration) {
			super(mappings, configuration);
		}

		@Override
		public boolean isHandler(Class<?> beanType) {
			return super.isHandler(beanType);
		}
	}
}
