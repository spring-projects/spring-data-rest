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

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link MappedProperties}.
 *
 * @author Oliver Gierke
 */
class MappedPropertiesUnitTests {

	ObjectMapper mapper = new ObjectMapper();
	KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
	PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Sample.class);
	MappedProperties properties = MappedProperties.forDeserialization(entity, mapper);

	@Test // DATAREST-575
	void doesNotExposeMappedPropertyForNonSpringDataPersistentProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedBySpringData")).isFalse();
		assertThat(properties.getPersistentProperty("notExposedBySpringData")).isNull();
	}

	@Test // DATAREST-575
	void doesNotExposeMappedPropertyForNonJacksonProperty() {

		assertThat(properties.hasPersistentPropertyForField("notExposedByJackson")).isFalse();
		assertThat(properties.getPersistentProperty("notExposedByJackson")).isNull();
	}

	@Test // DATAREST-575
	void exposesProperty() {

		assertThat(properties.hasPersistentPropertyForField("exposedProperty")).isTrue();
		assertThat(properties.getPersistentProperty("exposedProperty")).isNotNull();
	}

	@Test // DATAREST-575
	void exposesRenamedPropertyByExternalName() {

		assertThat(properties.hasPersistentPropertyForField("email")).isTrue();
		assertThat(properties.getPersistentProperty("email")).isNotNull();
		assertThat(properties.getMappedName(entity.getRequiredPersistentProperty("emailAddress"))).isEqualTo("email");
	}

	@Test // DATAREST-1006
	void doesNotExposeIgnoredPropertyViaJsonProperty() {

		assertThat(properties.hasPersistentPropertyForField("readOnlyProperty")).isFalse();
		assertThat(properties.getPersistentProperty("readOnlyProperty")).isNull();
	}

	@Test // DATAREST-1248
	void doesNotExcludeReadOnlyPropertiesForSerialization() {

		MappedProperties properties = MappedProperties.forSerialization(entity, mapper);

		assertThat(properties.hasPersistentPropertyForField("readOnlyProperty")).isTrue();
		assertThat(properties.getPersistentProperty("readOnlyProperty")).isNotNull();
	}

	@Test // DATAREST-1383
	void doesNotRegardReadOnlyPropertyForDeserialization() {

		MappedProperties properties = MappedProperties.forDeserialization(entity, mapper);

		assertThat(properties.isWritableProperty("anotherReadOnlyProperty")).isFalse();
		assertThat(properties.getPersistentProperty("readOnlyProperty")).isNull();

		properties = MappedProperties.forSerialization(entity, mapper);

		assertThat(properties.hasPersistentPropertyForField("anotherReadOnlyProperty")).isTrue();
		assertThat(properties.getPersistentProperty("readOnlyProperty")).isNotNull();
	}

	@Test // DATAREST-1440
	void exposesExistanceOfCatchAllMethod() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(SampleWithJsonAnySetter.class);

		MappedProperties properties = MappedProperties.forDeserialization(entity, mapper);

		assertThat(properties.isWritableProperty("someProperty")).isTrue();
		assertThat(properties.isWritableProperty("readOnlyProperty")).isFalse();
		assertThat(properties.isWritableProperty("anotherReadOnlyProperty")).isFalse();

		// Due to @JsonAnySetter
		assertThat(properties.isWritableProperty("someRandomProperty")).isTrue();
	}

	static class Sample {

		public @Transient String notExposedBySpringData;
		public @JsonIgnore String notExposedByJackson;
		public String exposedProperty;
		public @JsonProperty("email") String emailAddress;
		public @JsonProperty(access = Access.READ_ONLY) String readOnlyProperty;
		public @ReadOnlyProperty String anotherReadOnlyProperty;
	}

	static class SampleWithJsonAnySetter {

		public String someProperty;
		public @JsonProperty(access = Access.READ_ONLY) String readOnlyProperty;
		public @ReadOnlyProperty String anotherReadOnlyProperty;

		@JsonAnySetter
		void set(String key, String value) {}
	}
}
