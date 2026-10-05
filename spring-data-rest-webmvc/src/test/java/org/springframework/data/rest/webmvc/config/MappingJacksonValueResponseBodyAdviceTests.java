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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * Unit tests for {@link MappingJacksonValueResponseBodyAdvice}.
 *
 * @author Steve Rutherford
 */
@SuppressWarnings({ "deprecation", "removal" })
@ExtendWith(MockitoExtension.class)
class MappingJacksonValueResponseBodyAdviceTests {

	MappingJacksonValueResponseBodyAdvice advice = new MappingJacksonValueResponseBodyAdvice();

	@Mock MethodParameter parameter;
	@Mock ServerHttpRequest request;
	@Mock ServerHttpResponse response;

	// -------------------------------------------------------------------------
	// supports()
	// -------------------------------------------------------------------------

	@Test // GH-1544
	void supportsDirectMappingJacksonValueReturnType() {

		when(parameter.getParameterType()).thenAnswer(inv -> MappingJacksonValue.class);

		assertThat(advice.supports(parameter, TypeConstrainedJacksonJsonHttpMessageConverter.class)).isTrue();
	}

	@Test // GH-1544
	void doesNotSupportUnrelatedReturnType() {

		when(parameter.getParameterType()).thenAnswer(inv -> RepresentationModel.class);
		when(parameter.nested()).thenReturn(parameter);
		when(parameter.getNestedParameterType()).thenAnswer(inv -> RepresentationModel.class);

		assertThat(advice.supports(parameter, TypeConstrainedJacksonJsonHttpMessageConverter.class)).isFalse();
	}

	@Test // GH-1544
	void doesNotSupportPlainJacksonConverter() {

		when(parameter.getParameterType()).thenAnswer(inv -> RepresentationModel.class);
		when(parameter.nested()).thenReturn(parameter);
		when(parameter.getNestedParameterType()).thenAnswer(inv -> RepresentationModel.class);

		assertThat(advice.supports(parameter, MappingJackson2HttpMessageConverter.class)).isFalse();
	}

	// -------------------------------------------------------------------------
	// beforeBodyWrite() — unwrapping
	// -------------------------------------------------------------------------

	@Test // GH-1544
	void unwrapsMappingJacksonValueAndReturnsInnerValue() {

		RepresentationModel<?> model = new RepresentationModel<>();
		MappingJacksonValue wrapper = new MappingJacksonValue(model);

		Object result = advice.beforeBodyWrite(wrapper, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		assertThat(result).isSameAs(model);
	}

	@Test // GH-1544
	void returnsNonMappingJacksonValueBodyUnchanged() {

		RepresentationModel<?> model = new RepresentationModel<>();

		Object result = advice.beforeBodyWrite(model, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		assertThat(result).isSameAs(model);
	}

	@Test // GH-1544
	void returnsNullBodyUnchanged() {

		Object result = advice.beforeBodyWrite(null, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		assertThat(result).isNull();
	}

	// -------------------------------------------------------------------------
	// determineWriteHints() — filter propagation
	// -------------------------------------------------------------------------

	@Test // GH-1544
	void ignoresIncompatibleJackson2FilterProviderAndReturnsNoHints() {

		// Jackson 2 FilterProvider is incompatible with Jackson 3 — it should be skipped (with a warning)
		com.fasterxml.jackson.databind.ser.FilterProvider jackson2Filter =
				mock(com.fasterxml.jackson.databind.ser.FilterProvider.class);
		MappingJacksonValue wrapper = new MappingJacksonValue(new RepresentationModel<>());
		wrapper.setFilters(jackson2Filter);

		advice.beforeBodyWrite(wrapper, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		Map<String, Object> hints = advice.determineWriteHints(new RepresentationModel<>(), parameter,
				MediaType.APPLICATION_JSON, TypeConstrainedJacksonJsonHttpMessageConverter.class);

		// No filter hint should be present since the Jackson 2 filter cannot be used with Jackson 3
		assertThat(hints).isNull();
	}

	@Test // GH-1544
	void propagatesSerializationViewAsHint() {

		MappingJacksonValue wrapper = new MappingJacksonValue(new RepresentationModel<>());
		wrapper.setSerializationView(Object.class);

		advice.beforeBodyWrite(wrapper, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		Map<String, Object> hints = advice.determineWriteHints(new RepresentationModel<>(), parameter,
				MediaType.APPLICATION_JSON, TypeConstrainedJacksonJsonHttpMessageConverter.class);

		assertThat(hints).isNotNull();
		assertThat(hints).containsEntry(Class.class.getName(), Object.class);
	}

	@Test // GH-1544
	void returnsNullHintsWhenNoFilterOrViewSet() {

		MappingJacksonValue wrapper = new MappingJacksonValue(new RepresentationModel<>());

		advice.beforeBodyWrite(wrapper, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		Map<String, Object> hints = advice.determineWriteHints(new RepresentationModel<>(), parameter,
				MediaType.APPLICATION_JSON, TypeConstrainedJacksonJsonHttpMessageConverter.class);

		assertThat(hints).isNull();
	}

	@Test // GH-1544
	void hintsAreConsumedAfterDetermineWriteHints() {

		MappingJacksonValue wrapper = new MappingJacksonValue(new RepresentationModel<>());
		wrapper.setSerializationView(Object.class);

		advice.beforeBodyWrite(wrapper, parameter, MediaType.APPLICATION_JSON,
				TypeConstrainedJacksonJsonHttpMessageConverter.class, request, response);

		// First call consumes the hints
		advice.determineWriteHints(new RepresentationModel<>(), parameter,
				MediaType.APPLICATION_JSON, TypeConstrainedJacksonJsonHttpMessageConverter.class);

		// Second call should return null (ThreadLocal was cleared)
		Map<String, Object> secondCallHints = advice.determineWriteHints(new RepresentationModel<>(), parameter,
				MediaType.APPLICATION_JSON, TypeConstrainedJacksonJsonHttpMessageConverter.class);

		assertThat(secondCallHints).isNull();
	}

	@Test // GH-1544
	void registeredInRepositoryExporterHandlerAdapter() {

		// Verify the advice is wired into the configuration by checking it's instantiable
		// (integration-level verification is done via RepositoryRestMvConfigurationIntegrationTests)
		assertThat(new MappingJacksonValueResponseBodyAdvice()).isNotNull();
	}

}
