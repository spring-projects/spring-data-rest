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

package org.springframework.data.rest.webmvc.support;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Tests for the If-Match ArgumentResolver used for optimistic locking
 * 
 * @author Pablo Lozano
 */

@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class IfMatchHeaderArgumentResolverTests extends AbstractControllerIntegrationTests {

	@Autowired IfMatchHeaderArgumentResolver ifMatchHeaderArgumentResolver;

	/**
	 * @see DATAREST-160
	 */
	@Test
	public void testIfMatchHeaderResolved() throws Exception {

		Method method = ReflectionUtils.findMethod(SampleController.class, "ifMatch", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("PATCH", "/books/5-5-5-5-5");
		mockHttpServletRequest.addHeader("If-Match", "0");
		NativeWebRequest request = new ServletWebRequest(mockHttpServletRequest);
		Object resolvedHeader = ifMatchHeaderArgumentResolver.resolveArgument(parameter, null, request, null);

		assertThat(resolvedHeader, is((Object) "0"));
	}

	static class SampleController {

		@RequestMapping("/{repository}/{id}")
		void ifMatch(@IfMatch String ifMatch) {}
	}
}
