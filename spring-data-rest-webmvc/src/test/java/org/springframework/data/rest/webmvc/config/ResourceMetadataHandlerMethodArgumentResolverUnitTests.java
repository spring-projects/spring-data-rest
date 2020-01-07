/*
 * Copyright 2018-2020 the original author or authors.
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

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Unit tests for {@link ResourceMetadataHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 */
public class ResourceMetadataHandlerMethodArgumentResolverUnitTests {

	@Test // DATAREST-1250
	public void supportsResourceMetadataParameterType() {

		HandlerMethodArgumentResolver resolver = new ResourceMetadataHandlerMethodArgumentResolver(mock(Repositories.class),
				mock(ResourceMappings.class), BaseUri.NONE);

		MethodParameter parameter = mock(MethodParameter.class);
		doReturn(ResourceMetadata.class).when(parameter).getParameterType();

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}
}
