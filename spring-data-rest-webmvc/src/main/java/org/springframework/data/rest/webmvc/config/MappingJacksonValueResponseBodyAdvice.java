/*
 * Copyright 2024-present the original author or authors.
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

import tools.jackson.databind.ser.FilterProvider;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * {@link ResponseBodyAdvice} that transparently handles
 * {@link org.springframework.http.converter.json.MappingJacksonValue} return values from
 * {@link org.springframework.data.rest.webmvc.RepositoryRestController} methods. In Spring Framework 7,
 * {@code MappingJacksonValue} is no longer supported as a direct return type by the Jackson message converters
 * (which now use Jackson 3 / {@code tools.jackson}). This advice unwraps the inner value so that the HAL
 * message converters can serialize it correctly, producing proper {@code _links} output.
 *
 * <p>Any {@link FilterProvider} set on the {@code MappingJacksonValue} is propagated as a Jackson 3 converter
 * hint provided it is already an instance of {@link tools.jackson.databind.ser.FilterProvider}. If a legacy
 * Jackson 2 ({@code com.fasterxml.jackson.databind.ser.FilterProvider}) instance is detected, a warning is
 * logged and the filter is skipped; callers should migrate to Jackson 3's
 * {@link tools.jackson.databind.ser.std.SimpleFilterProvider} instead.
 *
 * <p>Serialization views set via {@code MappingJacksonValue#setSerializationView(Class)} are always forwarded
 * as hints regardless of the Jackson version.
 *
 * @author Steve Rutherford
 * @see ResponseBodyAdvice#determineWriteHints(Object, MethodParameter, MediaType, Class)
 */
@SuppressWarnings({ "deprecation", "removal" })
class MappingJacksonValueResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	private static final Logger logger = LoggerFactory.getLogger(MappingJacksonValueResponseBodyAdvice.class);

	/**
	 * Thread-local storage for hints extracted from a {@code MappingJacksonValue} during
	 * {@link #beforeBodyWrite}. These are consumed in {@link #determineWriteHints}, which is
	 * called immediately after on the same thread by the Spring MVC infrastructure.
	 */
	private static final ThreadLocal<Map<String, Object>> PENDING_HINTS = new ThreadLocal<>();

	/**
	 * The hint key used by {@code AbstractJacksonHttpMessageConverter} for a Jackson 3
	 * {@link FilterProvider}. The key is the fully-qualified class name of
	 * {@link tools.jackson.databind.ser.FilterProvider}.
	 */
	private static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();

	/**
	 * The hint key used by {@code AbstractJacksonHttpMessageConverter} for a JSON serialization view.
	 * The key is the fully-qualified class name of {@link Class}.
	 */
	private static final String JSON_VIEW_HINT = Class.class.getName();

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return org.springframework.http.converter.json.MappingJacksonValue.class
				.isAssignableFrom(returnType.getParameterType())
				|| hasWrappedMappingJacksonValueGeneric(returnType);
	}

	@Override
	public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		if (!(body instanceof org.springframework.http.converter.json.MappingJacksonValue wrapper)) {
			return body;
		}

		Map<String, Object> hints = new HashMap<>();

		Object rawFilters = wrapper.getFilters();
		if (rawFilters != null) {
			if (rawFilters instanceof FilterProvider jackson3Filters) {
				// The filter is already a Jackson 3 FilterProvider — pass it as a hint
				hints.put(FILTER_PROVIDER_HINT, jackson3Filters);
			} else {
				logger.warn("MappingJacksonValue contains a Jackson 2 FilterProvider ({}), which is incompatible "
						+ "with the Jackson 3 message converters used by Spring Data REST. The filter will be ignored. "
						+ "Migrate to tools.jackson.databind.ser.std.SimpleFilterProvider instead.",
						rawFilters.getClass().getName());
			}
		}

		Class<?> serializationView = wrapper.getSerializationView();
		if (serializationView != null) {
			hints.put(JSON_VIEW_HINT, serializationView);
		}

		if (!hints.isEmpty()) {
			PENDING_HINTS.set(hints);
		}

		return wrapper.getValue();
	}

	@Override
	public @Nullable Map<String, Object> determineWriteHints(@Nullable Object body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType) {

		Map<String, Object> hints = PENDING_HINTS.get();
		PENDING_HINTS.remove();
		return hints;
	}

	/**
	 * Checks whether the return type is a generic wrapper (e.g. {@code ResponseEntity<MappingJacksonValue>})
	 * whose type argument is {@code MappingJacksonValue}.
	 */
	private static boolean hasWrappedMappingJacksonValueGeneric(MethodParameter returnType) {

		Class<?> nested = returnType.nested().getNestedParameterType();
		return org.springframework.http.converter.json.MappingJacksonValue.class.isAssignableFrom(nested);
	}
}
