/*
 * Copyright 2018-2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Unit tests for {@link ResourceMetadataHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
class ResourceMetadataHandlerMethodArgumentResolverUnitTests {

	@Test // DATAREST-1250
	void supportsResourceMetadataParameterType() {

		HandlerMethodArgumentResolver resolver = new ResourceMetadataHandlerMethodArgumentResolver(mock(Repositories.class),
				mock(ResourceMappings.class), BaseUri.NONE);

		MethodParameter parameter = mock(MethodParameter.class);
		doReturn(ResourceMetadata.class).when(parameter).getParameterType();

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // GH-2480
	void failedMetadataLookupResultsInNotFound() throws Exception {

		var repositories = new Repositories(new DefaultListableBeanFactory());

		var resolver = new ResourceMetadataHandlerMethodArgumentResolver(repositories,
				mock(ResourceMappings.class), BaseUri.NONE);

		var method = SampleController.class.getDeclaredMethod("method", ResourceMappings.class);
		var parameter = new MethodParameter(method, 0);

		var request = new MockHttpServletRequest();
		request.setRequestURI("/some/foo");

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(
				() -> resolver.resolveArgument(parameter, new ModelAndViewContainer(), new ServletWebRequest(request),
						new DefaultDataBinderFactory(new ConfigurableWebBindingInitializer())));
	}

	static class SampleController {

		@GetMapping("/some/{repository}")
		void method(ResourceMappings mappings) {}
	}
}
