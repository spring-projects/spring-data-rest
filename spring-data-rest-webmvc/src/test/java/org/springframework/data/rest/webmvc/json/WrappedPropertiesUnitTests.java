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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link WrappedProperties}.
 * 
 * @author Mark Paluch
 */
public class WrappedPropertiesUnitTests {

	static final ObjectMapper MAPPER = new ObjectMapper();

	KeyValueMappingContext mappingContext;
	PersistentEntities persistentEntities;

	@Before
	public void setUp() {

		mappingContext = new KeyValueMappingContext();
		mappingContext.getPersistentEntity(MultiLevelNesting.class);
		mappingContext.getPersistentEntity(SyntheticProperties.class);

		persistentEntities = new PersistentEntities(Collections.singleton(mappingContext));
	}

	/**
	 * @see DATAREST-910
	 */
	@Test
	public void wrappedPropertiesShouldConsiderSingleLevelUnwrapping() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(OneLevelNesting.class);
		WrappedProperties wrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, persistentEntity,
				MAPPER);

		assertThat(wrappedProperties.hasPersistentPropertiesForField("street"), is(true));
		assertThat(wrappedProperties.hasPersistentPropertiesForField("one"), is(false));

		List<PersistentProperty<?>> street = wrappedProperties.getPersistentProperties("street");

		PersistentProperty<?> addressProperty = persistentEntity.getPersistentProperty("address");
		PersistentProperty<?> streetProperty = persistentEntities.getPersistentEntity(Address.class)
				.getPersistentProperty("street");

		assertThat(street, contains(addressProperty, streetProperty));
	}

	/**
	 * @see DATAREST-910
	 */
	@Test
	public void wrappedPropertiesShouldConsiderMultiLevelUnwrapping() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(MultiLevelNesting.class);
		WrappedProperties wrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, persistentEntity,
				new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-one-post"), is(true));
		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-street-post"), is(true));
		assertThat(wrappedProperties.hasPersistentPropertiesForField("nested"), is(false));

		List<PersistentProperty<?>> street = wrappedProperties.getPersistentProperties("pre-street-post");

		PersistentProperty<?> oneLevelNestingProperty = persistentEntity.getPersistentProperty("unwrapped");
		PersistentProperty<?> addressProperty = persistentEntities.getPersistentEntity(OneLevelNesting.class)
				.getPersistentProperty("address");
		PersistentProperty<?> streetProperty = persistentEntities.getPersistentEntity(Address.class)
				.getPersistentProperty("street");

		assertThat(street, contains(oneLevelNestingProperty, addressProperty, streetProperty));
	}

	/**
	 * @see DATAREST-910
	 */
	@Test
	public void wrappedPropertiesShouldConsiderJacksonFieldNames() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(MultiLevelNesting.class);
		WrappedProperties wrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, persistentEntity,
				new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-zip-post"), is(true));
	}

	/**
	 * @see DATAREST-910
	 */
	@Test
	public void wrappedPropertiesShouldIgnoreIgnoredJacksonFields() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(MultiLevelNesting.class);
		WrappedProperties wrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, persistentEntity,
				new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("pre-street-ignored"), is(false));
	}

	/**
	 * @see DATAREST-910
	 */
	@Test
	public void wrappedPropertiesShouldIgnoreSyntheticProperties() {

		PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(SyntheticProperties.class);
		WrappedProperties wrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, persistentEntity,
				new ObjectMapper());

		assertThat(wrappedProperties.hasPersistentPropertiesForField("street"), is(false));
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
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
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class MultiLevelNesting {

		String multi;
		@JsonUnwrapped(prefix = "pre-", suffix = "-post") OneLevelNesting unwrapped;
		@JsonIgnore @JsonUnwrapped(prefix = "pre-", suffix = "-ignored") OneLevelNesting ignored;
		@JsonUnwrapped(enabled = false) OneLevelNesting nested;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Address {

		String street;
		@JsonProperty("zip") String zipCode;
	}
}
