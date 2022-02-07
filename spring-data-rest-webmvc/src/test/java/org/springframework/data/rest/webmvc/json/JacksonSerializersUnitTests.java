/*
 * Copyright 2016-2022 original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.json.JacksonSerializersUnitTests.Sample.SampleEnum;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JacksonSerializers}.
 *
 * @author Oliver Gierke
 * @soundtrack James Bay - Move Together (Chaos And The Calm)
 */
class JacksonSerializersUnitTests {

	ObjectMapper mapper;

	@BeforeEach
	void setUp() {

		EnumTranslator translator = mock(EnumTranslator.class);
		doReturn(SampleEnum.VALUE).when(translator).fromText(SampleEnum.class, "value");

		this.mapper = new ObjectMapper();
		this.mapper.registerModule(new JacksonSerializers(translator));
	}

	@Test // DATAREST-929
	void translatesPlainEnumCorrectly() throws Exception {

		Sample result = mapper.readValue("{ \"property\" : \"value\"}", Sample.class);

		assertThat(result.property).isEqualTo(SampleEnum.VALUE);
	}

	@Test // DATAREST-929
	void translatesCollectionOfEnumsCorrectly() throws Exception {

		Sample result = mapper.readValue("{ \"collection\" : [ \"value\" ] }", Sample.class);

		assertThat(result.collection).contains(SampleEnum.VALUE);
	}

	@Test // DATAREST-929
	void translatesEnumArraysCorrectly() throws Exception {

		Sample result = mapper.readValue("{ \"array\" : [ \"value\" ] }", Sample.class);

		assertThat(result.array).contains(SampleEnum.VALUE);
	}

	@Test // DATAREST-929
	void translatesMapEnumValueCorrectly() throws Exception {

		Sample result = mapper.readValue("{ \"mapToEnum\" : { \"foo\" : \"value\" } }", Sample.class);

		assertThat(result.mapToEnum.get("foo")).isEqualTo(SampleEnum.VALUE);
	}

	static class Sample {

		enum SampleEnum {
			VALUE;
		}

		public SampleEnum property;
		public Collection<SampleEnum> collection;
		public SampleEnum[] array;
		public Map<String, SampleEnum> mapToEnum;
	}
}
