/*
 * Copyright 2016-2017 original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.annotation.Transient;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link MappedProperties}.
 * 
 * @author Oliver Gierke
 */
public class MappedPropertiesUnitTests {

	ObjectMapper mapper = new ObjectMapper();
	KeyValueMappingContext context = new KeyValueMappingContext();
	KeyValuePersistentEntity<?> entity = context.getPersistentEntity(Sample.class);
	MappedProperties properties = MappedProperties.forDeserialization(entity, mapper);

	@Test // DATAREST-575
	public void doesNotExposeMappedPropertyForNonSpringDataPersistentProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedBySpringData"), is(false));
		assertThat(properties.getPersistentProperty("notExposedBySpringData"), is(nullValue()));
	}

	@Test // DATAREST-575
	public void doesNotExposeMappedPropertyForNonJacksonProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedByJackson"), is(false));
		assertThat(properties.getPersistentProperty("notExposedByJackson"), is(nullValue()));
	}

	@Test // DATAREST-575
	public void exposesProperty() {

		assertThat(properties.hasPersistentPropertyForField("exposedProperty"), is(true));
		assertThat(properties.getPersistentProperty("exposedProperty"), is(notNullValue()));
	}

	@Test // DATAREST-575
	public void exposesRenamedPropertyByExternalName() {

		assertThat(properties.hasPersistentPropertyForField("email"), is(true));
		assertThat(properties.getPersistentProperty("email"), is(notNullValue()));
		assertThat(properties.getMappedName(entity.getPersistentProperty("emailAddress")), is("email"));
	}

	@Test // DATAREST-1006
	public void doesNotExposeIgnoredPropertyViaJsonProperty() {

		assertThat(properties.hasPersistentPropertyForField("readOnlyProperty"), is(false));
		assertThat(properties.getPersistentProperty("readOnlyProperty"), is(nullValue()));
	}

	@Test // DATAREST-1248
	public void doesNotExcludeReadOnlyPropertiesForSerialization() {

		MappedProperties properties = MappedProperties.forSerialization(entity, mapper);

		assertThat(properties.hasPersistentPropertyForField("readOnlyProperty"), is(true));
		assertThat(properties.getPersistentProperty("readOnlyProperty"), is(notNullValue()));
	}

	static class Sample {

		public @Transient String notExposedBySpringData;
		public @JsonIgnore String notExposedByJackson;
		public String exposedProperty;
		public @JsonProperty("email") String emailAddress;
		public @JsonProperty(access = Access.READ_ONLY) String readOnlyProperty;
	}
}
