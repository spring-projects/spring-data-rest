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

import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Unit tests for {@link WrappedJackson3Properties}.
 *
 * @author Mark Paluch
 */
class WrappedJackson3PropertiesUnitTests {

	static final ObjectMapper MAPPER = new ObjectMapper();

	KeyValueMappingContext<?, ?> mappingContext;
	PersistentEntities persistentEntities;

	@BeforeEach
	void setUp() {

		mappingContext = new KeyValueMappingContext<>();
		mappingContext.getPersistentEntity(MultiLevelNesting.class);
		mappingContext.getPersistentEntity(SyntheticProperties.class);

		persistentEntities = new PersistentEntities(Collections.singleton(mappingContext));
	}

	@Test // DATAREST-910
	void wrappedPropertiesShouldConsiderSingleLevelUnwrapping() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(OneLevelNesting.class);
		WrappedJackson3Properties wrappedProperties = WrappedJackson3Properties.fromJacksonProperties(persistentEntities,
				persistentEntity, MAPPER);

		assertThat(wrappedProperties.hasPersistentPropertiesForField("street")).isTrue();
		assertThat(wrappedProperties.hasPersistentPropertiesForField("one")).isFalse();

		List<PersistentProperty<?>> street = wrappedProperties.getPersistentProperties("street");

		PersistentProperty<?> addressProperty = persistentEntity.getRequiredPersistentProperty("address");
		PersistentProperty<?> streetProperty = persistentEntities.getRequiredPersistentEntity(Address.class)
				.getRequiredPersistentProperty("street");

		assertThat(street).contains(addressProperty, streetProperty);
	}

	@Test // DATAREST-910
	void wrappedPropertiesShouldConsiderMultiLevelUnwrapping() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(MultiLevelNesting.class);
		WrappedJackson3Properties wrappedProperties = WrappedJackson3Properties.fromJacksonProperties(persistentEntities,
				persistentEntity, new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-one-post")).isTrue();
		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-street-post")).isTrue();
		assertThat(wrappedProperties.hasPersistentPropertiesForField("nested")).isFalse();

		List<PersistentProperty<?>> street = wrappedProperties.getPersistentProperties("pre-street-post");

		PersistentProperty<?> oneLevelNestingProperty = persistentEntity.getRequiredPersistentProperty("unwrapped");
		PersistentProperty<?> addressProperty = persistentEntities.getRequiredPersistentEntity(OneLevelNesting.class)
				.getRequiredPersistentProperty("address");
		PersistentProperty<?> streetProperty = persistentEntities.getRequiredPersistentEntity(Address.class)
				.getRequiredPersistentProperty("street");

		assertThat(street).contains(oneLevelNestingProperty, addressProperty, streetProperty);
	}

	@Test // DATAREST-910
	void wrappedPropertiesShouldConsiderJacksonFieldNames() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(MultiLevelNesting.class);
		WrappedJackson3Properties wrappedProperties = WrappedJackson3Properties.fromJacksonProperties(persistentEntities,
				persistentEntity, new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-zip-post")).isTrue();
	}

	@Test // DATAREST-910
	void wrappedPropertiesShouldIgnoreIgnoredJacksonFields() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(MultiLevelNesting.class);
		WrappedJackson3Properties wrappedProperties = WrappedJackson3Properties.fromJacksonProperties(persistentEntities,
				persistentEntity, new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-street-ignored")).isFalse();
	}

	@Test // DATAREST-910
	void wrappedPropertiesShouldIgnoreSyntheticProperties() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(SyntheticProperties.class);
		WrappedJackson3Properties wrappedProperties = WrappedJackson3Properties.fromJacksonProperties(persistentEntities,
				persistentEntity, new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("street")).isFalse();
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class OneLevelNesting {

		String one;
		@JsonUnwrapped Address address;
	}

	static class SyntheticProperties {

		@JsonUnwrapped
		Address getUnwrapped() {
			return null;
		}

		MultiLevelNesting getSynthetic() {
			return null;
		}

		@JsonUnwrapped
		void setWrapped(OneLevelNesting address) {}
	}

	/**
	 * <pre>
	 * <code>
	  {
		"multi": "multi",
		"pre-one-post": "one",
		"pre-street-post": "street",
		"pre-zip-post": "zip",
		"nested": {
		  "one": "one",
		  "street": "street",
		  "zip": "zip"
		}
		}
	  </code>
	 * </pre>
	 */
	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class MultiLevelNesting {

		String multi;
		@JsonUnwrapped(prefix = "pre-", suffix = "-post") OneLevelNesting unwrapped;
		@JsonIgnore
		@JsonUnwrapped(prefix = "pre-", suffix = "-ignored") OneLevelNesting ignored;
		@JsonUnwrapped(enabled = false) OneLevelNesting nested;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Address {

		String street;
		@JsonProperty("zip") String zipCode;
	}
}
