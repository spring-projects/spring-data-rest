/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Unit tests for {@link DelegatingHandlerMapping}.
 * 
 * @author Oliver Gierke
 * @soundtrack Benny Greb - Stabila (Moving Parts)
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingHandlerMappingUnitTests {

	@Mock HandlerMapping first, second;
	@Mock HttpServletRequest request;

	/**
	 * @see DATAREST-490
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testname() throws Exception {

		HandlerMapping handlerMapping = new DelegatingHandlerMapping(Arrays.asList(first, second));

		when(first.getHandler(request)).thenThrow(HttpMediaTypeNotAcceptableException.class);

		try {

			handlerMapping.getHandler(request);
			fail(String.format("Expected %s!", HttpMediaTypeNotAcceptableException.class.getSimpleName()));

		} catch (HttpMediaTypeNotAcceptableException o_O) {
			verify(second, times(1)).getHandler(request);
		}
	}
}
