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
package org.springframework.data.rest.webmvc.alps;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.alps.Alps;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link HttpMessageConverter} to render {@link Alps} and {@link RootResourceInformation} instances as
 * {@code application/alps+json}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class AlpsJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter
		implements ResponseBodyAdvice<Object> {

	private final RootResourceInformationToAlpsDescriptorConverter converter;

	/**
	 * Creates a new {@link AlpsJsonHttpMessageConverter} for the given {@link Converter}.
	 *
	 * @param converter must not be {@literal null}.
	 */
	public AlpsJsonHttpMessageConverter(RootResourceInformationToAlpsDescriptorConverter converter) {

		Assert.notNull(converter, "Converter must not be null");

		this.converter = converter;

		ObjectMapper mapper = getObjectMapper();
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		setPrettyPrint(true);
		setSupportedMediaTypes(Arrays.asList(MediaTypes.ALPS_JSON, MediaType.APPLICATION_JSON, MediaType.ALL));
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return (clazz.isAssignableFrom(Alps.class) || clazz.isAssignableFrom(RootResourceInformation.class))
				&& super.canWrite(clazz, mediaType);
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, mediaType);
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
			MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {

		return body instanceof RootResourceInformation
				? Collections.singletonMap("alps", converter.convert((RootResourceInformation) body))
				: body;
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return converterType.equals(AlpsJsonHttpMessageConverter.class);
	}
}
