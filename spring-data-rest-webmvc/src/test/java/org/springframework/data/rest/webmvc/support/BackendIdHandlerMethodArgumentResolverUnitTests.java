/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.config.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Unit tests for {@link BackendIdHandlerMethodArgumentResolver}.
 *
 * @author Oliver Drotbohm
 */
public class BackendIdHandlerMethodArgumentResolverUnitTests {

	BackendIdConverter converter = mock(BackendIdConverter.class);
	ResourceMetadataHandlerMethodArgumentResolver delegate = mock(ResourceMetadataHandlerMethodArgumentResolver.class);
	BackendIdHandlerMethodArgumentResolver resolver = new BackendIdHandlerMethodArgumentResolver(
			PluginRegistry.of(converter), delegate, BaseUri.NONE);

	@Test // DATAREST-1382
	public void returnsNullForUrisNotNotContainingUri() throws Exception {

		ResourceMetadata metadata = mock(ResourceMetadata.class);

		doReturn(metadata).when(delegate).resolveArgument(any(), any(), any(), any());
		doReturn(true).when(converter).supports(any());

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("sampleMethod", Serializable.class), 0);

		Serializable result = resolver.resolveArgument(parameter, new ModelAndViewContainer(),
				new ServletWebRequest(new MockHttpServletRequest()), mock(WebDataBinderFactory.class));

		assertThat(result).isNull();
		verify(converter, never()).fromRequestId(eq(null), any());
	}

	interface Sample {

		@RequestMapping("/{repository}")
		void sampleMethod(Serializable parameter);
	}
}
