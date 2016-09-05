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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JacksonMappingAwareSortTranslator.SortTranslator}.
 *
 * @author Mark Paluch
 * @soundtrack dkn - Out Of This World (original version)
 */
public class SortTranslatorUnitTests {

	private MongoMappingContext mappingContext;

	@Before
	public void setUp() {

		mappingContext = new MongoMappingContext();
		mappingContext.getPersistentEntity(Plain.class);
		mappingContext.getPersistentEntity(WithJsonProperty.class);
	}

	/**
	 * @see DATAREST-883
	 */
	@Test
	public void shouldMapKnownProperties() {

		MappedProperties mappedProperties = MappedProperties
				.fromJacksonProperties(mappingContext.getPersistentEntity(Plain.class), new ObjectMapper());
		Sort translatedSort = new JacksonMappingAwareSortTranslator.SortTranslator(mappedProperties)
				.translateSort(new Sort("hello", "name"));

		assertThat(translatedSort.getOrderFor("hello"), is(nullValue()));
		assertThat(translatedSort.getOrderFor("name"), is(notNullValue()));
	}

	/**
	 * @see DATAREST-883
	 */
	@Test
	public void returnsNullSortIfNoPropertiesMatch() {

		MappedProperties mappedProperties = MappedProperties
				.fromJacksonProperties(mappingContext.getPersistentEntity(Plain.class), new ObjectMapper());
		Sort translatedSort = new JacksonMappingAwareSortTranslator.SortTranslator(mappedProperties)
				.translateSort(new Sort("hello", "world"));

		assertThat(translatedSort, is(nullValue()));
	}

	/**
	 * @see DATAREST-883
	 */
	@Test
	public void shouldMapKnownPropertiesWithJsonProperty() {

		MappedProperties mappedProperties = MappedProperties
				.fromJacksonProperties(mappingContext.getPersistentEntity(WithJsonProperty.class), new ObjectMapper());
		Sort translatedSort = new JacksonMappingAwareSortTranslator.SortTranslator(mappedProperties)
				.translateSort(new Sort("hello", "foo"));

		assertThat(translatedSort.getOrderFor("hello"), is(nullValue()));
		assertThat(translatedSort.getOrderFor("name"), is(notNullValue()));
	}

	/**
	 * @see DATAREST-883
	 */
	@Test
	public void shouldJacksonFieldNameForMapping() {

		MappedProperties mappedProperties = MappedProperties
				.fromJacksonProperties(mappingContext.getPersistentEntity(WithJsonProperty.class), new ObjectMapper());
		Sort translatedSort = new JacksonMappingAwareSortTranslator.SortTranslator(mappedProperties)
				.translateSort(new Sort("name"));

		assertThat(translatedSort, is(nullValue()));
	}

	static class Plain {
		public String name;
	}

	static class WithJsonProperty {
		public @JsonProperty("foo") String name;
	}
}
