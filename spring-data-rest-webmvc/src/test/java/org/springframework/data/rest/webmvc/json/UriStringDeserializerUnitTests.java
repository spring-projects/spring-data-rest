/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PersistentProperty;
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
@RunWith(MockitoJUnitRunner.class)
public class UriStringDeserializerUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Mock UriToEntityConverter converter;
	@Mock PersistentProperty<?> property;

	@Mock JsonParser parser;
	DeserializationContext context;

	UriStringDeserializer deserializer;

	@Before
	public void setUp() {

		this.deserializer = new UriStringDeserializer(property, converter);

		// Need to hack the context as there's virtually no way wo set up a combined parser and context easily
		this.context = new ObjectMapper().getDeserializationContext();
		ReflectionTestUtils.setField(context, "_parser", parser);
	}

	/**
	 * @see DATAREST-316
	 */
	@Test
	public void extractsUriToForwardToConverter() throws Exception {
		assertConverterInvokedWithUri("/foo/32", URI.create("/foo/32"));
	}

	/**
	 * @see DATAREST-316
	 */
	@Test
	public void extractsUriFromTemplateToForwardToConverter() throws Exception {
		assertConverterInvokedWithUri("/foo/32{?projection}", URI.create("/foo/32"));
	}

	/**
	 * @see DATAREST-377
	 */
	@Test
	public void returnsNullUriIfSourceIsEmptyOrNull() throws Exception {

		assertThat(invokeConverterWith(""), is(nullValue()));
		assertThat(invokeConverterWith(null), is(nullValue()));
	}

	/**
	 * @see DATAREST-377
	 */
	@Test
	public void rejectsNonUriValue() throws Exception {

		exception.expect(JsonMappingException.class);
		exception.expectMessage("managed domain type");

		invokeConverterWith("{ \"foo\" : \"bar\" }");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object invokeConverterWith(String source) throws Exception {

		when(property.getActualType()).thenReturn((Class) Object.class);
		when(parser.getValueAsString()).thenReturn(source);
		when(parser.getText()).thenReturn(source);

		return deserializer.deserialize(parser, context);
	}

	private void assertConverterInvokedWithUri(String source, URI expected) throws Exception {

		invokeConverterWith(source);

		verify(converter).convert(eq(expected), Mockito.any(TypeDescriptor.class),
				eq(TypeDescriptor.valueOf(Object.class)));
	}
}
