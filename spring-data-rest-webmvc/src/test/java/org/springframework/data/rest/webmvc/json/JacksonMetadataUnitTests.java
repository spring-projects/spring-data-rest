/*
 * Copyright 2015 the original author or authors.
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

import java.io.IOException;

import org.junit.Test;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Unit tests for {@link JacksonMetadata}.
 * 
 * @author Oliver Gierke
 * @soundtrack Four Sided Cube - Bad Day's Rememberance (Bunch of Sides)
 */
public class JacksonMetadataUnitTests {

	/**
	 * @see DATAREST-644
	 */
	@Test
	public void testname() {

		MongoMappingContext context = new MongoMappingContext();
		MongoPersistentEntity<?> entity = context.getPersistentEntity(User.class);

		ObjectMapper mapper = new ObjectMapper();

		JacksonMetadata metadata = new JacksonMetadata(mapper, User.class);

		MongoPersistentProperty property = entity.getPersistentProperty("username");

		assertThat(metadata.isExported(property), is(true));
		assertThat(metadata.isReadOnly(property), is(true));
	}

	/**
	 * @see DATAREST-644
	 */
	@Test
	public void detectsCustomSerializerFortType() {

		JsonSerializer<?> serializer = new JacksonMetadata(new ObjectMapper(), SomeBean.class)
				.getTypeSerializer(SomeBean.class);

		assertThat(serializer, is(instanceOf(SomeBeanSerializer.class)));
	}

	static class User {

		private String username;

		@JsonProperty(access = Access.READ_ONLY)
		public String getUsername() {
			return username;
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
