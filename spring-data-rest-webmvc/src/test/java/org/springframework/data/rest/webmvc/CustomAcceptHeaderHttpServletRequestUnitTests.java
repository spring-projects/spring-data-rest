/*
 * Copyright 2016-2018 original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.BasePathAwareHandlerMapping.CustomAcceptHeaderHttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link CustomAcceptHeaderHttpServletRequest}.
 *
 * @author Oliver Gierke
 * @soundtrack Spring engineering team meeting @ SpringOne Platform 2016
 */
class CustomAcceptHeaderHttpServletRequestUnitTests {

	HttpServletRequest request = new MockHttpServletRequest();

	@Test // DATAREST-863
	void returnsRegisterdHeadersOnAccessForMultipleOnes() {

		List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_ATOM_XML);
		HttpServletRequest servletRequest = new CustomAcceptHeaderHttpServletRequest(request, mediaTypes);

		assertThat(servletRequest.getHeader(HttpHeaders.ACCEPT))
				.isEqualTo(StringUtils.collectionToCommaDelimitedString(mediaTypes));

		List<String> expected = Collections.list(servletRequest.getHeaders(HttpHeaders.ACCEPT));

		assertThat(expected).hasSize(2);
		assertThat(expected).contains(MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_ATOM_XML_VALUE);
	}
}
