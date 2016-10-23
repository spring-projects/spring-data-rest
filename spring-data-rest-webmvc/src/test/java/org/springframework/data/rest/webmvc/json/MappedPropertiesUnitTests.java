/*
 * Copyright 2016 the original author or authors.
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

import org.junit.Test;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.rest.webmvc.json.DomainObjectReader.MappedProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link MappedProperties}.
 * 
 * @author Oliver Gierke
 */
public class MappedPropertiesUnitTests {

	ObjectMapper mapper = new ObjectMapper();
	MongoMappingContext context = new MongoMappingContext();
	MongoPersistentEntity<?> entity = context.getPersistentEntity(Sample.class);
	MappedProperties properties = MappedProperties.fromJacksonProperties(entity, mapper);

	/**
	 * @see DATAREST-575
	 */
	@Test
	public void doesNotExposeMappedPropertyForNonSpringDataPersistentProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedBySpringData"), is(false));
		assertThat(properties.getPersistentProperty("notExposedBySpringData"), is(nullValue()));
	}

	/**
	 * @see DATAREST-575
	 */
	@Test
	public void doesNotExposeMappedPropertyForNonJacksonProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedByJackson"), is(false));
		assertThat(properties.getPersistentProperty("notExposedByJackson"), is(nullValue()));
	}

	/**
	 * @see DATAREST-575
	 */
	@Test
	public void exposesProperty() {

		assertThat(properties.hasPersistentPropertyForField("exposedProperty"), is(true));
		assertThat(properties.getPersistentProperty("exposedProperty"), is(notNullValue()));
	}

	/**
	 * @see DATAREST-575
	 */
	@Test
	public void exposesRenamedPropertyByExternalName() {

		assertThat(properties.hasPersistentPropertyForField("email"), is(true));
		assertThat(properties.getPersistentProperty("email"), is(notNullValue()));
		assertThat(properties.getMappedName(entity.getPersistentProperty("emailAddress")), is("email"));
	}

	static class Sample {

		public @Transient String notExposedBySpringData;
		public @JsonIgnore String notExposedByJackson;
		public String exposedProperty;
		public @JsonProperty("email") String emailAddress;
	}
}
