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
import java.util.Arrays;
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
import org.springframework.data.annotation.Transient;
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
 * @author Craig Andrews
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
		mappingContext.getPersistentEntity(SampleWithTransient.class);
		mappingContext.getPersistentEntity(User.class);
		mappingContext.afterPropertiesSet();

		PersistentEntities entities = new PersistentEntities(Collections.singleton(mappingContext));

		this.reader = new DomainObjectReader(entities, new Associations(mappings, mock(RepositoryRestConfiguration.class)));
	}

	/**
	 * @see DATAREST-
	 */
	@Test
	public void considersTransientProperties() throws Exception {

		SampleWithTransient sample = new SampleWithTransient();
		sample.name="name";
		sample.temporary="temp";
		JsonNode node = new ObjectMapper().readTree("{\"name\": \"new name\", \"temporary\": \"new temp\"}");

		SampleWithTransient result = reader.readPut((ObjectNode) node, sample, new ObjectMapper());

		assertThat(result.name, is("new name"));
		assertThat(result.temporary, is("new temp"));
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
	@SuppressWarnings("unchecked")
	public void mergesNestedMapWithoutTypeInformation() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{\"map\" : {\"a\": \"1\", \"b\": {\"c\": \"2\"}}}");

		TypeWithGenericMap target = new TypeWithGenericMap();
		target.map = new HashMap<String, Object>();
		target.map.put("b", new HashMap<String, Object>());

		TypeWithGenericMap result = reader.readPut((ObjectNode) node, target, mapper);

		assertThat(result.map.get("a"), is((Object) "1"));

		Object object = result.map.get("b");
		assertThat(object, is(instanceOf(Map.class)));
		assertThat(((Map<Object, Object>) object).get("c"), is((Object) "2"));
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

	/**
	 * @see DATAREST-931
	 */
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

	/**
	 * @see DATAREST-919
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void readsComplexNestedMapsAndArrays() throws Exception {

		Map<String, Object> childMap = new HashMap<String, Object>();
		childMap.put("child1", "ok");

		HashMap<String, Object> nestedMap = new HashMap<String, Object>();
		nestedMap.put("c1", "v1");

		TypeWithGenericMap map = new TypeWithGenericMap();
		map.map = new HashMap<String, Object>();
		map.map.put("sub1", "ok");
		map.map.put("sub2", new ArrayList<String>(Arrays.asList("ok1", "ok2")));
		map.map.put("sub3", new ArrayList<Object>(Arrays.asList(childMap)));
		map.map.put("sub4", nestedMap);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode payload = (ObjectNode) mapper.readTree("{ \"map\" : { \"sub1\" : \"ok\","
				+ " \"sub2\" : [ \"ok1\", \"ok2\" ], \"sub3\" : [ { \"childOk1\" : \"ok\" }], \"sub4\" : {"
				+ " \"c1\" : \"v1\", \"c2\" : \"new\" } } }");

		TypeWithGenericMap result = reader.readPut(payload, map, mapper);

		assertThat(result.map.get("sub1"), is((Object) "ok"));

		List<String> sub2 = as(result.map.get("sub2"), List.class);
		assertThat(sub2.get(0), is("ok1"));
		assertThat(sub2.get(1), is("ok2"));

		List<Map<String, String>> sub3 = as(result.map.get("sub3"), List.class);
		assertThat(sub3.get(0).get("childOk1"), is("ok"));

		Map<Object, String> sub4 = as(result.map.get("sub4"), Map.class);
		assertThat(sub4.get("c1"), is("v1"));
		assertThat(sub4.get("c2"), is("new"));
	}

	@SuppressWarnings("unchecked")
	private static <T> T as(Object source, Class<T> type) {

		assertThat(source, is(instanceOf(type)));
		return (T) source;
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

	/**
	 * @see DATAREST-937
	 */
	@Test
	public void considersTransientProperties() throws Exception {

		SampleWithTransient sample = new SampleWithTransient();
		sample.name = "name";
		sample.temporary = "temp";

		JsonNode node = new ObjectMapper().readTree("{ \"name\" : \"new name\", \"temporary\" : \"new temp\" }");

		SampleWithTransient result = reader.readPut((ObjectNode) node, sample, new ObjectMapper());

		assertThat(result.name, is("new name"));
		assertThat(result.temporary, is("new temp"));
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

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class SampleWithTransient {

		String name;
		@Transient String temporary;
	}
}
