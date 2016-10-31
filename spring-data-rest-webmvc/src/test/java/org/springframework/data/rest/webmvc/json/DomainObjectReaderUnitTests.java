/*
 * Copyright 2015-2016 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Version;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.mapping.Associations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

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

		KeyValueMappingContext mappingContext = new KeyValueMappingContext();
		mappingContext.getPersistentEntity(SampleUser.class);
		mappingContext.getPersistentEntity(Person.class);
		mappingContext.getPersistentEntity(TypeWithGenericMap.class);
		mappingContext.getPersistentEntity(VersionedType.class);
		mappingContext.getPersistentEntity(SampleWithCreatedDate.class);
		mappingContext.getPersistentEntity(User.class);
		mappingContext.afterPropertiesSet();

		PersistentEntities entities = new PersistentEntities(Collections.singleton(mappingContext));

		this.reader = new DomainObjectReader(entities, new Associations(mappings, mock(RepositoryRestConfiguration.class)));
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

	/**
	 * @see DATAREST-705
	 */
	@Test
	public void doesNotWipeIdAndVersionPropertyForPut() throws Exception {

		VersionedType type = new VersionedType();
		type.id = 1L;
		type.version = 1L;
		type.firstname = "Dave";

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{ \"lastname\" : \"Matthews\" }");

		VersionedType result = reader.readPut(node, type, mapper);

		assertThat(result.lastname, is("Matthews"));
		assertThat(result.firstname, is(nullValue()));
		assertThat(result.id, is(1L));
		assertThat(result.version, is(1L));
	}

	/**
	 * @see DATAREST-873
	 */
	@Test
	public void doesNotApplyInputToReadOnlyFields() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		Date reference = new Date();

		SampleWithCreatedDate sample = new SampleWithCreatedDate();
		sample.createdDate = reference;

		assertThat(reader.readPut(node, sample, mapper).createdDate, is(reference));
	}

	@Test
	public void readsPatchForEntityNestedInCollection() throws Exception {

		Phone phone = new Phone();
		phone.creationDate = new GregorianCalendar();

		User user = new User();
		user.phones.add(phone);

		ByteArrayInputStream source = new ByteArrayInputStream(
				"{ \"phones\" : [ { \"label\" : \"some label\" } ] }".getBytes(Charsets.UTF_8));

		User result = reader.read(source, user, new ObjectMapper());

		assertThat(result.phones.get(0).creationDate, is(notNullValue()));
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

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class VersionedType {

		@Id Long id;
		@Version Long version;

		String firstname, lastname;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class SampleWithCreatedDate {

		@CreatedDate //
		@ReadOnlyProperty //
		Date createdDate;
	}

	static class User {

		public List<Phone> phones = new ArrayList<Phone>();
	}

	static class Phone {

		public Calendar creationDate;
		public String label;
	}
}
