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
package org.springframework.data.rest.webmvc.json;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.UriStringDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link UriStringDeserializer}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class UriStringDeserializerUnitTests {

	@Mock UriToEntityConverter converter;

	@Mock JsonParser parser;
	DeserializationContext context;

	UriStringDeserializer deserializer;

	@BeforeEach
	void setUp() {

		this.deserializer = new UriStringDeserializer(Object.class, converter);

		// Need to hack the context as there's virtually no way wo set up a combined parser and context easily
		this.context = new ObjectMapper().getDeserializationContext();
		ReflectionTestUtils.setField(context, "_parser", parser);
	}

	@Test // DATAREST-316
	void extractsUriToForwardToConverter() throws Exception {
		assertConverterInvokedWithUri("/foo/32", URI.create("/foo/32"));
	}

	@Test // DATAREST-316
	void extractsUriFromTemplateToForwardToConverter() throws Exception {
		assertConverterInvokedWithUri("/foo/32{?projection}", URI.create("/foo/32"));
	}

	@Test // DATAREST-377
	void returnsNullUriIfSourceIsEmptyOrNull() throws Exception {

		assertThat(invokeConverterWith("")).isNull();
		assertThat(invokeConverterWith(null)).isNull();
	}

	@Test // DATAREST-377
	void rejectsNonUriValue() throws Exception {

		assertThatExceptionOfType(JsonMappingException.class) //
				.isThrownBy(() -> invokeConverterWith("{ \"foo\" : \"bar\" }"))
				.withMessageContaining("managed domain type");
	}

	private Object invokeConverterWith(String source) throws Exception {

		when(parser.getValueAsString()).thenReturn(source);

		return deserializer.deserialize(parser, context);
	}

	private void assertConverterInvokedWithUri(String source, URI expected) throws Exception {

		invokeConverterWith(source);

		verify(converter).convert(eq(expected), Mockito.any(TypeDescriptor.class),
				eq(TypeDescriptor.valueOf(Object.class)));
	}
}
