/*
 * Copyright 2014-2025 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Integration tests for {@link BackendIdHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
class BackendIdConverterHandlerMethodArgumentResolverIntegrationTests
		extends AbstractControllerIntegrationTests {

	@Autowired BackendIdHandlerMethodArgumentResolver resolver;

	@Test // DATAREST-155
	void translatesUriToBackendId() throws Exception {

		Method method = ReflectionUtils.findMethod(SampleController.class, "resolveId", Serializable.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		NativeWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/books/5-5-5-5-5"));

		Object resolvedId = resolver.resolveArgument(parameter, null, request, null);

		assertThat(resolvedId).isEqualTo(5L);
	}

	static class SampleController {

		@RequestMapping("/{repository}/{id}")
		void resolveId(@BackendId Serializable backendId) {}
	}
}
