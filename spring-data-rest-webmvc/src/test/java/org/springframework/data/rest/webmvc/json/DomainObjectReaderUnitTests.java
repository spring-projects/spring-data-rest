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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.mapping.ResourceMappings;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link DomainObjectReader}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainObjectReaderUnitTests {

	@Mock ResourceMappings mappings;

	DomainObjectReader reader;

	@Before
	public void setUp() {

		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.getPersistentEntity(SampleUser.class);
		mappingContext.getPersistentEntity(Person.class);
		mappingContext.getPersistentEntity(TypeWithGenericMap.class);
		mappingContext.afterPropertiesSet();

		PersistentEntities entities = new PersistentEntities(Collections.singleton(mappingContext));

		this.reader = new DomainObjectReader(entities, mappings);
	}

	/**
	 * @see DATAREST-461
	 */
	@Test
	public void doesNotConsiderIgnoredProperties() throws Exception {

		SampleUser user = new SampleUser("firstname", "password");
		JsonNode node = new ObjectMapper().readTree("{}");

		SampleUser result = reader.readPut((ObjectNode) node, user, new ObjectMapper());

		assertThat(result.name, is(nullValue()));
		assertThat(result.password, is("password"));
	}

	/**
	 * @see DATAREST-556
	 */
	@Test
	public void considersMappedFieldNamesWhenApplyingNodeToDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);

		JsonNode node = new ObjectMapper().readTree("{\"FirstName\":\"Carter\",\"LastName\":\"Beauford\"}");

		Person result = reader.readPut((ObjectNode) node, new Person("Dave", "Matthews"), mapper);

		assertThat(result.firstName, is("Carter"));
		assertThat(result.lastName, is("Beauford"));
	}

	/**
	 * @see DATAREST-605
	 */
	@Test
	public void mergesMapCorrectly() throws Exception {

		SampleUser user = new SampleUser("firstname", "password");
		user.relatedUsers = Collections.singletonMap("parent", new SampleUser("firstname", "password"));

		JsonNode node = new ObjectMapper()
				.readTree("{ \"relatedUsers\" : { \"parent\" : { \"password\" : \"sneeky\", \"name\" : \"Oliver\" } } }");

		SampleUser result = reader.readPut((ObjectNode) node, user, new ObjectMapper());

		// Assert that the nested Map values also consider ignored properties
		assertThat(result.relatedUsers.get("parent").password, is("password"));
		assertThat(result.relatedUsers.get("parent").name, is("Oliver"));
	}

	/**
	 * @see DATAREST-701
	 */
	@Test
	public void mergesNestedMapWithoutTypeInformation() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{\"map\" : {\"a\": \"1\", \"b\": {\"c\": \"2\"}}}");

		TypeWithGenericMap target = new TypeWithGenericMap();
		target.map = new HashMap<String, Object>();
		target.map.put("b", new HashMap<String, Object>());

		reader.readPut((ObjectNode) node, target, mapper);
	}

	/**
	 * @see DATAREST-701
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsMergingUnknownDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		reader.readPut(node, "", mapper);
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class SampleUser {

		String name;
		@JsonIgnore String password;
		Map<String, SampleUser> relatedUsers;

		public SampleUser(String name, String password) {
			this.name = name;
			this.password = password;
		}
	}

	/**
	 * @see DATAREST-556
	 */
	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Person {

		String firstName, lastName;

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class TypeWithGenericMap {

		Map<String, Object> map;
	}
}
