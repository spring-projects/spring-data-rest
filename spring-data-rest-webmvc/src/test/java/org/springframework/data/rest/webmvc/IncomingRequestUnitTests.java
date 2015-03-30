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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link IncomingRequest}.
 * 
 * @author Oliver Gierke
 */
public class IncomingRequestUnitTests {

	MockHttpServletRequest request;

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest("PATCH", "/");
	}

	/**
	 * @see DATAREST-498
	 */
	@Test
	public void identifiesJsonPatchRequestForRequestWithContentTypeParameters() {

		request.addHeader("Content-Type", "application/json-patch+json;charset=UTF-8");

		IncomingRequest incomingRequest = new IncomingRequest(new ServletServerHttpRequest(request));

		assertThat(incomingRequest.isJsonPatchRequest(), is(true));
		assertThat(incomingRequest.isJsonMergePatchRequest(), is(false));
	}

	/**
	 * @see DATAREST-498
	 */
	@Test
	public void identifiesJsonMergePatchRequestForRequestWithContentTypeParameters() {

		request.addHeader("Content-Type", "application/merge-patch+json;charset=UTF-8");

		IncomingRequest incomingRequest = new IncomingRequest(new ServletServerHttpRequest(request));

		assertThat(incomingRequest.isJsonPatchRequest(), is(false));
		assertThat(incomingRequest.isJsonMergePatchRequest(), is(true));
	}
}
