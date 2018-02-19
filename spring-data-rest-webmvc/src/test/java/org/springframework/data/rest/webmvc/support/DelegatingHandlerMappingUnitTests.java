/*
 * Copyright 2015-2018 original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;

/**
 * Unit tests for {@link DelegatingHandlerMapping}.
 *
 * @author Oliver Gierke
 * @soundtrack Benny Greb - Stabila (Moving Parts)
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DelegatingHandlerMappingUnitTests {

	@Mock HandlerMapping first, second;
	@Mock HttpServletRequest request;

	@Test // DATAREST-490, DATAREST-522
	public void consultsAllHandlerMappingsAndThrowsExceptionEventually() throws Exception {

		DelegatingHandlerMapping mapping = new DelegatingHandlerMapping(Arrays.asList(first, second));

		assertHandlerTriedButExceptionThrown(mapping, HttpMediaTypeNotAcceptableException.class);
		assertHandlerTriedButExceptionThrown(mapping, HttpRequestMethodNotSupportedException.class);
		assertHandlerTriedButExceptionThrown(mapping, UnsatisfiedServletRequestParameterException.class);
	}

	@Test // DATAREST-1193
	public void exposesMatchabilityOfSelectedMapping() {

		// Given:
		// A matching mapping that doesn't get selected
		MatchableHandlerMapping third = mock(MatchableHandlerMapping.class);
		doReturn(mock(RequestMatchResult.class)).when(third).match(any(), any());

		// A matching mapping that gets selected
		RequestMatchResult result = mock(RequestMatchResult.class);
		MatchableHandlerMapping fourth = mock(MatchableHandlerMapping.class, Answers.RETURNS_MOCKS);
		doReturn(result).when(fourth).match(any(), any());

		DelegatingHandlerMapping mapping = new DelegatingHandlerMapping(Arrays.asList(first, second, third, fourth));

		assertThat(mapping.match(request, "somePattern")).isEqualTo(result);
	}

	private final void assertHandlerTriedButExceptionThrown(HandlerMapping mapping, Class<? extends Exception> type)
			throws Exception {

		when(first.getHandler(request)).thenThrow(type);

		try {

			mapping.getHandler(request);
			fail(String.format("Expected %s!", type.getSimpleName()));

		} catch (Exception o_O) {
			assertThat(o_O).isInstanceOf(type);
			verify(second, times(1)).getHandler(request);
		} finally {
			reset(first, second);
		}
	}
}
