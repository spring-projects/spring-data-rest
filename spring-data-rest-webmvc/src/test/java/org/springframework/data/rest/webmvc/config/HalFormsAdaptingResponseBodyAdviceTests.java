/*
 * Copyright 2021 the original author or authors.
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

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

/**
 * Unit tests for {@link HalFormsAdaptingResponseBodyAdvice}.
 *
 * @author Oliver Drotbohm
 */
@ExtendWith(MockitoExtension.class)
public class HalFormsAdaptingResponseBodyAdviceTests<T extends RepresentationModel<T>> {

	HalFormsAdaptingResponseBodyAdvice<T> advice = new HalFormsAdaptingResponseBodyAdvice<>();

	@Mock MethodParameter parameter;
	MockHttpServletRequest request = new MockHttpServletRequest();
	MockHttpServletResponse response = new MockHttpServletResponse();

	@Test // #2060
	void supportsTypeConstraintedHttpMessageConverterOnly() {

		assertThat(advice.supports(parameter, TypeConstrainedMappingJackson2HttpMessageConverter.class)).isTrue();
		assertThat(advice.supports(parameter, MappingJackson2HttpMessageConverter.class)).isFalse();
	}

	@Test // #2060
	void usesHalJsonContentTypeIfNoAffordancesSet() {

		request.addHeader(HttpHeaders.ACCEPT,
				MediaType.toString(Arrays.asList(MediaTypes.HAL_FORMS_JSON, MediaTypes.HAL_JSON)));

		RepresentationModel<T> model = new RepresentationModel<>();

		assertResponseContentType(model, MediaTypes.HAL_JSON);
	}

	@Test // #2060
	void usesHalFormsContentTypeIfAffordancesPresent() {

		request.addHeader(HttpHeaders.ACCEPT,
				MediaType.toString(Arrays.asList(MediaTypes.HAL_FORMS_JSON, MediaTypes.HAL_JSON)));

		RepresentationModel<T> model = new RepresentationModel<>();
		model.add(Affordances.of(Link.of("localhost")).afford(HttpMethod.GET).build().toLink());

		assertResponseContentType(model, MediaTypes.HAL_FORMS_JSON);
	}

	@Test // #2060
	void issues415IfNoCompatibleMediaTypeWasRequested() {

		request.addHeader(HttpHeaders.ACCEPT,
				MediaType.toString(Arrays.asList(MediaTypes.HAL_FORMS_JSON)));

		RepresentationModel<T> model = new RepresentationModel<>();

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
				.isThrownBy(() -> assertResponseContentType(model, MediaTypes.HAL_JSON));
	}

	private void assertResponseContentType(RepresentationModel<T> model, MediaType mediaType) {

		this.response.addHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_FORMS_JSON_VALUE);
		ServletServerHttpResponse response = new ServletServerHttpResponse(this.response);

		advice.beforeBodyWrite(model, parameter, MediaTypes.HAL_FORMS_JSON,
				TypeConstrainedMappingJackson2HttpMessageConverter.class,
				new ServletServerHttpRequest(request), response);

		assertThat(response.getHeaders().getContentType()).isEqualTo(mediaType);
	}
}
