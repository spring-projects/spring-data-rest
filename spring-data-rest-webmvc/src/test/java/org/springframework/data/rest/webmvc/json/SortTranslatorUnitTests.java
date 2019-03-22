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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Reference;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.webmvc.json.JacksonMappingAwareSortTranslator.SortTranslator;
import org.springframework.data.rest.webmvc.mapping.Associations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JacksonMappingAwareSortTranslator.SortTranslator}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @soundtrack dkn - Out Of This World (original version)
 */
public class SortTranslatorUnitTests {

	ObjectMapper objectMapper = new ObjectMapper();
	KeyValueMappingContext mappingContext;
	PersistentEntities persistentEntities;
	SortTranslator sortTranslator;

	@Before
	public void setUp() {

		mappingContext = new KeyValueMappingContext();
		mappingContext.getPersistentEntity(Plain.class);
		mappingContext.getPersistentEntity(WithJsonProperty.class);
		mappingContext.getPersistentEntity(UnwrapEmbedded.class);
		mappingContext.getPersistentEntity(MultiUnwrapped.class);

		persistentEntities = new PersistentEntities(Collections.singleton(mappingContext));

		sortTranslator = new SortTranslator(persistentEntities, objectMapper, new Associations(
				new PersistentEntitiesResourceMappings(persistentEntities), mock(RepositoryRestConfiguration.class)));
	}

	@Test // DATAREST-883
	public void shouldMapKnownProperties() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("hello", "name"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort.getOrderFor("hello"), is(nullValue()));
		assertThat(translatedSort.getOrderFor("name"), is(notNullValue()));
	}

	@Test // DATAREST-883
	public void returnsNullSortIfNoPropertiesMatch() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("hello", "world"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort, is(nullValue()));
	}

	@Test // DATAREST-883
	public void shouldMapKnownPropertiesWithJsonProperty() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("hello", "foo"),
				mappingContext.getPersistentEntity(WithJsonProperty.class));

		assertThat(translatedSort.getOrderFor("hello"), is(nullValue()));
		assertThat(translatedSort.getOrderFor("name"), is(notNullValue()));
	}

	@Test // DATAREST-883
	public void shouldJacksonFieldNameForMapping() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("name"),
				mappingContext.getPersistentEntity(WithJsonProperty.class));

		assertThat(translatedSort, is(nullValue()));
	}

	@Test // DATAREST-910
	public void shouldMapKnownNestedProperties() {

		Sort translatedSort = sortTranslator.translateSort(
				new Sort("embedded.name", "embedded.collection", "embedded.someInterface"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort.getOrderFor("embedded.name"), is(notNullValue()));
		assertThat(translatedSort.getOrderFor("embedded.collection"), is(notNullValue()));
		assertThat(translatedSort.getOrderFor("embedded.someInterface"), is(notNullValue()));
	}

	@Test // DATAREST-910
	public void shouldSkipWrongNestedProperties() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("embedded.unknown"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort, is(nullValue()));
	}

	@Test // DATAREST-910, DATAREST-976
	public void shouldSkipKnownAssociationProperties() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("association.name"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort, is(nullValue()));
	}

	@Test // DATAREST-976
	public void shouldMapEmbeddableAssociationProperties() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("refEmbedded.name"),
				mappingContext.getPersistentEntity(Plain.class));

		assertThat(translatedSort.getOrderFor("refEmbedded.name"), is(notNullValue()));
	}

	@Test // DATAREST-910
	public void shouldJacksonFieldNameForNestedFieldMapping() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("em.foo"),
				mappingContext.getPersistentEntity(WithJsonProperty.class));

		assertThat(translatedSort.getOrderFor("embeddedWithJsonProperty.bar"), is(notNullValue()));
	}

	@Test // DATAREST-910
	public void shouldTranslatePathForSingleLevelJsonUnwrappedObject() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("un-name"),
				mappingContext.getPersistentEntity(UnwrapEmbedded.class));

		assertThat(translatedSort.getOrderFor("embedded.name"), is(notNullValue()));
	}

	@Test // DATAREST-910
	public void shouldTranslatePathForMultiLevelLevelJsonUnwrappedObject() {

		Sort translatedSort = sortTranslator.translateSort(new Sort("un-name", "burrito.un-name"),
				mappingContext.getPersistentEntity(MultiUnwrapped.class));

		assertThat(translatedSort.getOrderFor("anotherWrap.embedded.name"), is(notNullValue()));
		assertThat(translatedSort.getOrderFor("burrito.embedded.name"), is(notNullValue()));
	}

	static class Plain {

		public String name;
		public Embedded embedded;
		@Reference public Embedded refEmbedded;
		@Reference public AnotherRootEntity association;
	}

	static class UnwrapEmbedded {
		@JsonUnwrapped(prefix = "un-") public Embedded embedded;
	}

	static class MultiUnwrapped {

		public String name;
		@JsonUnwrapped public UnwrapEmbedded anotherWrap;
		public UnwrapEmbedded burrito;
	}

	static class Embedded {

		public String name;
		public List<String> collection;
		public SomeInterface someInterface;
	}

	static class WithJsonProperty {

		@JsonProperty("foo") public String name;
		@JsonProperty("em") public EmbeddedWithJsonProperty embeddedWithJsonProperty;
	}

	static class EmbeddedWithJsonProperty {
		@JsonProperty("foo") public String bar;
	}

	static interface SomeInterface {}

	@RestResource
	static class AnotherRootEntity {
		public String name;
	}
}
