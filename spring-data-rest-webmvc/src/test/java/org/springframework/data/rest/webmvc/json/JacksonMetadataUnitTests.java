/*
 * Copyright 2015-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Unit tests for {@link JacksonMetadata}.
 * 
 * @author Oliver Gierke
 * @soundtrack Four Sided Cube - Bad Day's Remembrance (Bunch of Sides)
 */
public class JacksonMetadataUnitTests {

	MappingContext<?, ?> context;
	ObjectMapper mapper;

	@Before
	public void setUp() {

		this.context = new KeyValueMappingContext<>();

		this.mapper = new ObjectMapper();
		this.mapper.disable(MapperFeature.INFER_PROPERTY_MUTATORS);
	}

	@Test // DATAREST-644
	public void detectsReadOnlyProperty() {

		JacksonMetadata metadata = new JacksonMetadata(mapper, User.class);

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(User.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("username");

		assertThat(metadata.isExported(property)).isTrue();
		assertThat(metadata.isReadOnly(property)).isTrue();
	}

	@Test // DATAREST-644
	public void reportsConstructorArgumentAsJacksonWritable() {

		JacksonMetadata metadata = new JacksonMetadata(mapper, Value.class);

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Value.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("value");

		assertThat(metadata.isReadOnly(property)).isFalse();
	}

	@Test // DATAREST-644
	public void detectsCustomSerializerFortType() {

		JsonSerializer<?> serializer = new JacksonMetadata(new ObjectMapper(), SomeBean.class)
				.getTypeSerializer(SomeBean.class);

		assertThat(serializer).isInstanceOf(SomeBeanSerializer.class);
	}

	static class User {

		private String username;

		public String getUsername() {
			return username;
		}
	}

	static class Value {

		private String value;

		@JsonCreator
		public Value(@JsonProperty("value") String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@JsonSerialize(using = SomeBeanSerializer.class)
	static class SomeBean {}

	@SuppressWarnings("serial")
	static class SomeBeanSerializer extends StdSerializer<SomeBean> {

		public SomeBeanSerializer() {
			super(SomeBean.class);
		}

		@Override
		public void serialize(SomeBean value, JsonGenerator gen, SerializerProvider provider) throws IOException {}
	}
}
