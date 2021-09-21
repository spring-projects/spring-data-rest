/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.support.BackendIdHandlerMethodArgumentResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Unit tests for {@link PersistentEntityResourceHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
class PersistentEntityResourceHandlerMethodArgumentResolverUnitTests {

	HttpMessageConverter<?> converter;
	RootResourceInformationHandlerMethodArgumentResolver rootResourceResolver;
	BackendIdHandlerMethodArgumentResolver backendIdResolver;
	DomainObjectReader reader;

	@BeforeEach
	void setUp() throws Exception {

		this.converter = mock(HttpMessageConverter.class);
		when(this.converter.canRead((Class<?>) any(), (MediaType) any())).thenReturn(true);

		this.rootResourceResolver = mock(RootResourceInformationHandlerMethodArgumentResolver.class);
		setupRootResourceInfoFor(Foo.class);

		this.backendIdResolver = mock(BackendIdHandlerMethodArgumentResolver.class);
		this.reader = mock(DomainObjectReader.class);
	}

	@Test // DATAREST-1050
	@SuppressWarnings("unchecked")
	void returnsAggregateInstanceWithIdentifierPopulatedForPutRequests() throws Exception {

		PersistentEntityResourceHandlerMethodArgumentResolver argumentResolver = new PersistentEntityResourceHandlerMethodArgumentResolver(
				Arrays.<HttpMessageConverter<?>> asList(converter), rootResourceResolver, backendIdResolver, reader,
				PluginRegistry.empty());

		HttpServletRequest request = new MockHttpServletRequest("PUT", "/foo/4711");

		doReturn(new Foo()).when(converter).read(Mockito.any(Class.class), Mockito.any(HttpInputMessage.class));
		mockInvocationOfResolver(backendIdResolver, "4711");

		Object result = argumentResolver.resolveArgument(null, null, new ServletWebRequest(request), null);

		assertThat(result).isInstanceOfSatisfying(PersistentEntityResource.class, it -> {
			assertThat(it.getContent()).isInstanceOfSatisfying(Foo.class, foo -> assertThat(foo.id).isEqualTo(4711L));
		});
	}

	@Test // DATAREST-1304
	@SuppressWarnings("unchecked")
	void setsLookupPropertyForEntitiesWithCustomLookup() throws Exception {

		EntityLookup<?> lookup = mock(EntityLookup.class);
		doReturn(Optional.of("name")).when(lookup).getLookupProperty();
		doReturn(true).when(lookup).supports(Foo.class);

		PersistentEntityResourceHandlerMethodArgumentResolver argumentResolver = new PersistentEntityResourceHandlerMethodArgumentResolver(
				Arrays.<HttpMessageConverter<?>> asList(converter), rootResourceResolver, backendIdResolver, reader,
				PluginRegistry.of(Arrays.asList(lookup)));

		HttpServletRequest request = new MockHttpServletRequest("PUT", "/foo/someName");

		doReturn(new Foo()).when(converter).read(Mockito.any(Class.class), Mockito.any(HttpInputMessage.class));
		mockInvocationOfResolver(backendIdResolver, "someName");

		Object result = argumentResolver.resolveArgument(null, null, new ServletWebRequest(request), null);

		assertThat(result).isInstanceOfSatisfying(PersistentEntityResource.class, it -> {
			assertThat(it.getContent()).isInstanceOfSatisfying(Foo.class, foo -> {
				assertThat(foo.name).isEqualTo("someName");
			});
		});
	}

	private void setupRootResourceInfoFor(Class<?> type) throws Exception {

		RootResourceInformation information = mock(RootResourceInformation.class);

		doReturn(type).when(information).getDomainType();
		mockInvocationOfResolver(rootResourceResolver, information);

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		KeyValuePersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Foo.class);

		doReturn(entity).when(information).getPersistentEntity();
		doReturn(mock(RepositoryInvoker.class)).when(information).getInvoker();
	}

	private static void mockInvocationOfResolver(HandlerMethodArgumentResolver resolver, Object result) throws Exception {

		doReturn(result).when(resolver).resolveArgument((MethodParameter) any(), //
				(ModelAndViewContainer) any(), //
				(NativeWebRequest) any(), //
				(WebDataBinderFactory) any());
	}

	static class Foo {
		@Id Long id;
		String name;
	}
}
