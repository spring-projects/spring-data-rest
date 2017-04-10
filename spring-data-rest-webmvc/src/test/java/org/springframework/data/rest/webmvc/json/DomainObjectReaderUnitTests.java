/*
 * Copyright 2015-2017 the original author or authors.
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
import static org.mockito.Mockito.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.mapping.Associations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

/**
 * Unit tests for {@link DomainObjectReader}.
 * 
 * @author Oliver Gierke
 * @author Craig Andrews
 * @author Mathias Düsterhöft
 * @author Ken Dombeck
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainObjectReaderUnitTests {

	@Mock ResourceMappings mappings;

	DomainObjectReader reader;
	PersistentEntities entities;

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
		mappingContext.getPersistentEntity(Inner.class);
		mappingContext.getPersistentEntity(Outer.class);
		mappingContext.getPersistentEntity(Parent.class);
		mappingContext.getPersistentEntity(Product.class);
		mappingContext.getPersistentEntity(TransientReadOnlyProperty.class);
		mappingContext.getPersistentEntity(CollectionOfEnumWithMethods.class);
		mappingContext.getPersistentEntity(SampleWithReference.class);
		mappingContext.getPersistentEntity(Note.class);
		mappingContext.afterPropertiesSet();

		this.entities = new PersistentEntities(Collections.singleton(mappingContext));
		this.reader = new DomainObjectReader(entities, new Associations(mappings, mock(RepositoryRestConfiguration.class)));
	}

	@Test // DATAREST-461
	public void doesNotConsiderIgnoredProperties() throws Exception {

		SampleUser user = new SampleUser("firstname", "password");
		JsonNode node = new ObjectMapper().readTree("{}");

		SampleUser result = reader.readPut((ObjectNode) node, user, new ObjectMapper());

		assertThat(result.name, is(nullValue()));
		assertThat(result.password, is("password"));
	}

	@Test // DATAREST-556
	public void considersMappedFieldNamesWhenApplyingNodeToDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);

		JsonNode node = new ObjectMapper().readTree("{\"FirstName\":\"Carter\",\"LastName\":\"Beauford\"}");

		Person result = reader.readPut((ObjectNode) node, new Person("Dave", "Matthews"), mapper);

		assertThat(result.firstName, is("Carter"));
		assertThat(result.lastName, is("Beauford"));
	}

	@Test // DATAREST-605
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

	@Test // DATAREST-701
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

	@Test(expected = IllegalArgumentException.class) // DATAREST-701
	public void rejectsMergingUnknownDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		reader.readPut(node, "", mapper);
	}

	@Test // DATAREST-705
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

	@Test // DATAREST-873
	public void doesNotApplyInputToReadOnlyFields() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		Date reference = new Date();

		SampleWithCreatedDate sample = new SampleWithCreatedDate();
		sample.createdDate = reference;

		assertThat(reader.readPut(node, sample, mapper).createdDate, is(reference));
	}

	@Test // DATAREST-931
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

	@Test // DATAREST-919
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

	@Test // DATAREST-938
	public void nestedEntitiesAreUpdated() throws Exception {

		Inner inner = new Inner();
		inner.name = "inner name";
		inner.prop = "something";

		Outer outer = new Outer();
		outer.prop = "else";
		outer.name = "outer name";
		outer.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"name\" : \"new inner name\" } }");

		Outer result = reader.doMerge((ObjectNode) node, outer, new ObjectMapper());

		assertThat(result, is(sameInstance(outer)));
		assertThat(result.prop, is("else"));
		assertThat(result.inner.prop, is("something"));
		assertThat(result.inner.name, is("new inner name"));
		assertThat(result.inner, is(sameInstance(inner)));
	}

	@Test // DATAREST-937
	public void considersTransientProperties() throws Exception {

		SampleWithTransient sample = new SampleWithTransient();
		sample.name = "name";
		sample.temporary = "temp";

		JsonNode node = new ObjectMapper().readTree("{ \"name\" : \"new name\", \"temporary\" : \"new temp\" }");

		SampleWithTransient result = reader.readPut((ObjectNode) node, sample, new ObjectMapper());

		assertThat(result.name, is("new name"));
		assertThat(result.temporary, is("new temp"));
	}

	@Test // DATAREST-953
	public void writesArrayForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item());

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.get(0).some, is("value"));
	}

	@Test // DATAREST-956
	public void writesArrayWithAddedItemForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item());

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ " + "{ \"some\" : \"value1\" },"
				+ "{ \"some\" : \"value2\" }," + "{ \"some\" : \"value3\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.size(), is(3));
		assertThat(result.inner.items.get(0).some, is("value1"));
		assertThat(result.inner.items.get(1).some, is("value2"));
		assertThat(result.inner.items.get(2).some, is("value3"));
	}

	@Test // DATAREST-956
	public void writesArrayWithRemovedItemForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item("test1"));
		inner.items.add(new Item("test2"));
		inner.items.add(new Item("test3"));

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.size(), is(1));
		assertThat(result.inner.items.get(0).some, is("value"));
	}

	@Test // DATAREST-959
	public void addsElementToPreviouslyEmptyCollection() throws Exception {

		Parent source = new Parent();
		source.inner = new Child();
		source.inner.items = null;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.size(), is(1));
		assertThat(result.inner.items.get(0).some, is("value"));
	}

	@Test // DATAREST-959
	@SuppressWarnings("unchecked")
	public void turnsObjectIntoCollection() throws Exception {

		Parent source = new Parent();
		source.inner = new Child();
		source.inner.object = new Item("value");

		JsonNode node = new ObjectMapper()
				.readTree("{ \"inner\" : { \"object\" : [ { \"some\" : \"value\" }, { \"some\" : \"otherValue\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());
		assertThat(result.inner.object, is(instanceOf(Collection.class)));

		Collection<?> collection = (Collection<?>) result.inner.object;
		assertThat(collection.size(), is(2));

		Iterator<Map<String, Object>> iterator = (Iterator<Map<String, Object>>) collection.iterator();
		assertThat(iterator.next().get("some"), is((Object) "value"));
		assertThat(iterator.next().get("some"), is((Object) "otherValue"));
	}

	@Test // DATAREST-965
	public void writesObjectWithRemovedItemsForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item("test1"));
		inner.items.add(new Item("test2"));

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"object\" : \"value\" } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items, is(nullValue()));
		assertThat((String) result.inner.object, is("value"));
	}

	@Test // DATAREST-965
	public void writesArrayWithRemovedObjectForPut() throws Exception {

		Child inner = new Child();
		inner.object = "value";

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.size(), is(1));
		assertThat(result.inner.items.get(0).some, is("value"));
		assertThat(result.inner.object, is(nullValue()));
	}

	@Test // DATAREST-986
	public void readsComplexMap() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(
				"{ \"map\" : { \"en\" : { \"value\" : \"eventual\" }, \"de\" : { \"value\" : \"schlussendlich\" } } }");

		Product result = reader.readPut((ObjectNode) node, new Product(), mapper);

		assertThat(result.map.get(Locale.ENGLISH), is(new LocalizedValue("eventual")));
		assertThat(result.map.get(Locale.GERMAN), is(new LocalizedValue("schlussendlich")));
	}

	@Test // DATAREST-987
	public void handlesTransientPropertyWithoutFieldProperly() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{ \"name\" : \"Foo\" }");

		reader.readPut((ObjectNode) node, new TransientReadOnlyProperty(), mapper);
	}

	@Test // DATAREST-977
	public void readsCollectionOfComplexEnum() throws Exception {

		CollectionOfEnumWithMethods sample = new CollectionOfEnumWithMethods();
		sample.enums.add(SampleEnum.FIRST);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{ \"enums\" : [ \"SECOND\", \"FIRST\" ] }");

		CollectionOfEnumWithMethods result = reader.merge((ObjectNode) node, sample, mapper);

		assertThat(result.enums, contains(SampleEnum.SECOND, SampleEnum.FIRST));
	}

	@Test // DATAREST-944
	public void mergesAssociations() {

		List<Nested> originalCollection = Arrays.asList(new Nested(2, 3));
		SampleWithReference source = new SampleWithReference(Arrays.asList(new Nested(1, 2), new Nested(2, 3)));
		SampleWithReference target = new SampleWithReference(originalCollection);

		SampleWithReference result = reader.mergeForPut(source, target, new ObjectMapper());

		assertThat(result.nested, is(source.nested));
		assertThat(result.nested == originalCollection, is(false));
	}

	@Test // DATAREST-944
	public void mergesAssociationsAndKeepsMutableCollection() {

		ArrayList<Nested> originalCollection = new ArrayList<Nested>(Arrays.asList(new Nested(2, 3)));
		SampleWithReference source = new SampleWithReference(
				new ArrayList<Nested>(Arrays.asList(new Nested(1, 2), new Nested(2, 3))));
		SampleWithReference target = new SampleWithReference(originalCollection);

		SampleWithReference result = reader.mergeForPut(source, target, new ObjectMapper());

		assertThat(result.nested, is(source.nested));
		assertThat(result.nested == originalCollection, is(true));
	}

	@Test // DATAREST-1030
	public void patchWithReferenceToRelatedEntityIsResolvedCorrectly() throws Exception {

		Associations associations = mock(Associations.class);
		when(associations.isLinkableAssociation(Matchers.any(PersistentProperty.class))).thenReturn(true);

		DomainObjectReader reader = new DomainObjectReader(entities, associations);

		Tag first = new Tag();
		Tag second = new Tag();

		Note note = new Note();
		note.tags.add(first);
		note.tags.add(second);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(Tag.class, new SelectValueByIdSerializer<Tag>(Collections.singletonMap(second.id, second)));

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);

		ObjectNode readTree = (ObjectNode) mapper.readTree(String.format("{ \"tags\" : [ \"%s\"]}", second.id));

		Note result = reader.doMerge(readTree, note, mapper);

		assertThat(result.tags, contains(second));
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

		protected SampleUser() {}
	}

	// DATAREST-556
	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Person {

		String firstName, lastName;

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		protected Person() {}
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

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Outer {

		String name;
		String prop;
		Inner inner;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Inner {

		String name;
		String prop;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Parent {
		Child inner;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Child {
		List<Item> items;
		Object object;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	@NoArgsConstructor
	@AllArgsConstructor
	static class Item {
		String some;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Product {
		Map<Locale, LocalizedValue> map = new HashMap<Locale, LocalizedValue>();
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	static class LocalizedValue {
		String value;
	}

	@JsonAutoDetect(getterVisibility = Visibility.ANY)
	static class TransientReadOnlyProperty {

		@Transient
		public String getName() {
			return null;
		}

		public void setName(String name) {}
	}

	// DATAREST-977

	interface EnumInterface {
		String getFoo();
	}

	static enum SampleEnum implements EnumInterface {

		FIRST {

			@Override
			public String getFoo() {
				return "first";
			}

		},
		SECOND {

			public String getFoo() {
				return "second";
			}
		};
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class CollectionOfEnumWithMethods {
		List<SampleEnum> enums = new ArrayList<SampleEnum>();
	}

	@Value
	static class SampleWithReference {
		@Reference List<Nested> nested;
	}

	@Value
	static class Nested {
		int x, y;
	}

	// DATAREST-1030

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Note {
		@Id UUID id = UUID.randomUUID();
		@Reference List<Tag> tags = new ArrayList<Tag>();
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Tag {
		@Id UUID id = UUID.randomUUID();
		String name;
	}

	@RequiredArgsConstructor
	static class SelectValueByIdSerializer<T> extends JsonDeserializer<T> {

		private final Map<? extends Object, T> values;

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			String text = p.getText();

			for (Entry<? extends Object, T> entry : values.entrySet()) {
				if (entry.getKey().toString().equals(text)) {
					return entry.getValue();
				}
			}

			return null;
		}
	}
}
