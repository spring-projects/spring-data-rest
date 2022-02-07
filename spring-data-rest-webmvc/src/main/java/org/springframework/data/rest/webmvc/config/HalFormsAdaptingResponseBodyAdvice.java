/*
 * Copyright 2021-2022 the original author or authors.
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

import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * {@link ResponseBodyAdvice} that tweaks responses asking for HAL FORMS to potentially fall back to a non-forms
 * {@link MediaType} in case no affordances are registered on the {@link RepresentationModel} to be rendered.
 *
 * @author Oliver Drotbohm
 */
class HalFormsAdaptingResponseBodyAdvice<T extends RepresentationModel<T>>
		implements ResponseBodyAdvice<RepresentationModel<T>> {

	private static final Logger logger = LoggerFactory.getLogger(RequestResponseBodyMethodProcessor.class);
	private static final String MESSAGE = "HalFormsRejectingResponseBodyAdvice - Changing content type to '%s' as no affordances were registered on the representation model to be rendered!";
	private static final List<MediaType> SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaTypes.HAL_JSON,
			MediaType.APPLICATION_JSON);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice#supports(org.springframework.core.MethodParameter, java.lang.Class)
	 */
	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return TypeConstrainedMappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice#beforeBodyWrite(java.lang.Object, org.springframework.core.MethodParameter, org.springframework.http.MediaType, java.lang.Class, org.springframework.http.server.ServerHttpRequest, org.springframework.http.server.ServerHttpResponse)
	 */
	@Override
	@SneakyThrows
	public RepresentationModel<T> beforeBodyWrite(RepresentationModel<T> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		// Only step in if we are about to render HAL FORMS
		if (!MediaTypes.HAL_FORMS_JSON.equals(selectedContentType)) {
			return body;
		}

		List<MediaType> accept = request.getHeaders().getAccept();

		boolean hasAffordances = body.getLinks().stream()
				.anyMatch(it -> !it.getAffordances().isEmpty());

		// Affordances registered -> we're fine as we will render templates
		if (hasAffordances) {
			return body;
		}

		// Check whether either HAL or general JSON are acceptable
		for (MediaType candidate : accept) {
			for (MediaType supported : SUPPORTED_MEDIA_TYPES) {
				if (candidate.isCompatibleWith(supported)) {

					// Tweak response to expose that
					logger.debug(String.format(MESSAGE, supported));
					response.getHeaders().setContentType(supported);

					return body;
				}
			}
		}

		// Reject the request otherwise
		throw new HttpMediaTypeNotAcceptableException(SUPPORTED_MEDIA_TYPES);
	}
}
