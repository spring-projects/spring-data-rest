/*
 * Copyright 2015-2024 the original author or authors.
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

import static com.fasterxml.jackson.annotation.JsonProperty.Access.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
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
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link DomainObjectReader}.
 *
 * @author Oliver Gierke
 * @author Craig Andrews
 * @author Mathias Düsterhöft
 * @author Ken Dombeck
 * @author Thomas Mrozinski
 */
@ExtendWith(MockitoExtension.class)
class DomainObjectReaderUnitTests {

	@Mock ResourceMappings mappings;

	DomainObjectReader reader;
	PersistentEntities entities;

	@BeforeEach
	void setUp() {

		KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();
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
		mappingContext.getPersistentEntity(WithNullCollection.class);
		mappingContext.getPersistentEntity(ArrayHolder.class);
		mappingContext.getPersistentEntity(Apple.class);
		mappingContext.getPersistentEntity(Pear.class);
		mappingContext.getPersistentEntity(WithCustomMappedPrimitiveCollection.class);
		mappingContext.getPersistentEntity(BugModel.class);
		mappingContext.getPersistentEntity(ArrayListHolder.class);
		mappingContext.getPersistentEntity(MapWrapper.class);
		mappingContext.getPersistentEntity(SlotsContainer.class);
		mappingContext.afterPropertiesSet();

		this.entities = new PersistentEntities(Collections.singleton(mappingContext));
		this.reader = new DomainObjectReader(entities, new Associations(mappings, mock(RepositoryRestConfiguration.class)));
	}

	@Test // DATAREST-461
	void doesNotConsiderIgnoredProperties() throws Exception {

		SampleUser user = new SampleUser("firstname", "password");
		JsonNode node = new ObjectMapper().readTree("{}");

		SampleUser result = reader.readPut((ObjectNode) node, user, new ObjectMapper());

		assertThat(result.name).isNull();
		assertThat(result.password).isEqualTo("password");
	}

	@Test // DATAREST-556
	void considersMappedFieldNamesWhenApplyingNodeToDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);

		JsonNode node = new ObjectMapper().readTree("{\"FirstName\":\"Carter\",\"LastName\":\"Beauford\"}");

		Person result = reader.readPut((ObjectNode) node, new Person("Dave", "Matthews"), mapper);

		assertThat(result.firstName).isEqualTo("Carter");
		assertThat(result.lastName).isEqualTo("Beauford");
	}

	@Test // DATAREST-605
	void mergesMapCorrectly() throws Exception {

		SampleUser user = new SampleUser("firstname", "password");
		user.relatedUsers = Collections.singletonMap("parent", new SampleUser("firstname", "password"));

		JsonNode node = new ObjectMapper()
				.readTree("{ \"relatedUsers\" : { \"parent\" : { \"password\" : \"sneeky\", \"name\" : \"Oliver\" } } }");

		SampleUser result = reader.readPut((ObjectNode) node, user, new ObjectMapper());

		// Assert that the nested Map values also consider ignored properties
		assertThat(result.relatedUsers.get("parent").password).isEqualTo("password");
		assertThat(result.relatedUsers.get("parent").name).isEqualTo("Oliver");
	}

	@Test // DATAREST-701
	@SuppressWarnings("unchecked")
	void mergesNestedMapWithoutTypeInformation() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{\"map\" : {\"a\": \"1\", \"b\": {\"c\": \"2\"}}}");

		TypeWithGenericMap target = new TypeWithGenericMap();
		target.map = new HashMap<String, Object>();
		target.map.put("b", new HashMap<String, Object>());

		TypeWithGenericMap result = reader.readPut((ObjectNode) node, target, mapper);

		assertThat(result.map.get("a")).isEqualTo("1");

		Object object = result.map.get("b");
		assertThat(object).isInstanceOf(Map.class);
		assertThat(((Map<Object, Object>) object).get("c")).isEqualTo("2");
	}

	@Test // DATAREST-701
	void rejectsMergingUnknownDomainObject() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		assertThatExceptionOfType(JsonMappingException.class) //
				.isThrownBy(() -> reader.readPut(node, "", mapper));
	}

	@Test // DATAREST-705
	void doesNotWipeIdAndVersionPropertyForPut() throws Exception {

		VersionedType type = new VersionedType();
		type.id = 1L;
		type.version = 1L;
		type.firstname = "Dave";

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{ \"lastname\" : \"Matthews\" }");

		VersionedType result = reader.readPut(node, type, mapper);

		assertThat(result.lastname).isEqualTo("Matthews");
		assertThat(result.firstname).isNull();
		assertThat(result.id).isEqualTo(1L);
		assertThat(result.version).isEqualTo(1L);
	}

	@Test // DATAREST-1006
	void doesNotWipeReadOnlyJsonPropertyForPut() throws Exception {

		SampleUser sampleUser = new SampleUser("name", "password");
		sampleUser.lastLogin = new Date();

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{ \"name\" : \"another\" }");

		SampleUser result = reader.readPut(node, sampleUser, mapper);

		assertThat(result.name).isEqualTo("another");
		assertThat(result.password).isNotNull();
		assertThat(result.lastLogin).isNotNull();
	}

	@Test // DATAREST-873
	void doesNotApplyInputToReadOnlyFields() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{}");

		Date reference = new Date();

		SampleWithCreatedDate sample = new SampleWithCreatedDate();
		sample.createdDate = reference;

		assertThat(reader.readPut(node, sample, mapper).createdDate).isEqualTo(reference);
	}

	@Test // DATAREST-931, GH-2358
	// https://datatracker.ietf.org/doc/html/rfc7386#section-2
	void handlesEntityNestedInAnArrayLikePutForPatchRequest() throws Exception {

		Phone phone = new Phone();
		phone.creationDate = new GregorianCalendar();

		User user = new User();
		user.phones.add(phone);

		ByteArrayInputStream source = new ByteArrayInputStream(
				"{ \"phones\" : [ { \"label\" : \"some label\" } ] }".getBytes(StandardCharsets.UTF_8));

		User result = reader.read(source, user, new ObjectMapper());

		assertThat(result.phones.get(0).creationDate).isNull();
	}

	@Test // DATAREST-919
	@SuppressWarnings("unchecked")
	void readsComplexNestedMapsAndArrays() throws Exception {

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

		assertThat(result.map.get("sub1")).isEqualTo("ok");

		List<String> sub2 = as(result.map.get("sub2"), List.class);
		assertThat(sub2.get(0)).isEqualTo("ok1");
		assertThat(sub2.get(1)).isEqualTo("ok2");

		List<Map<String, String>> sub3 = as(result.map.get("sub3"), List.class);
		assertThat(sub3.get(0).get("childOk1")).isEqualTo("ok");

		Map<Object, String> sub4 = as(result.map.get("sub4"), Map.class);
		assertThat(sub4.get("c1")).isEqualTo("v1");
		assertThat(sub4.get("c2")).isEqualTo("new");
	}

	@Test // DATAREST-938
	void nestedEntitiesAreUpdated() throws Exception {

		Inner inner = new Inner();
		inner.name = "inner name";
		inner.prop = "something";

		Outer outer = new Outer();
		outer.prop = "else";
		outer.name = "outer name";
		outer.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"name\" : \"new inner name\" } }");

		Outer result = reader.doMerge((ObjectNode) node, outer, new ObjectMapper());

		assertThat(result).isSameAs(outer);
		assertThat(result.prop).isEqualTo("else");
		assertThat(result.inner.prop).isEqualTo("something");
		assertThat(result.inner.name).isEqualTo("new inner name");
		assertThat(result.inner).isSameAs(inner);
	}

	@Test // DATAREST-937
	void considersTransientProperties() throws Exception {

		SampleWithTransient sample = new SampleWithTransient();
		sample.name = "name";
		sample.temporary = "temp";

		JsonNode node = new ObjectMapper().readTree("{ \"name\" : \"new name\", \"temporary\" : \"new temp\" }");

		SampleWithTransient result = reader.readPut((ObjectNode) node, sample, new ObjectMapper());

		assertThat(result.name).isEqualTo("new name");
		assertThat(result.temporary).isEqualTo("new temp");
	}

	@Test // DATAREST-953
	void writesArrayForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item());

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items.get(0).some).isEqualTo("value");
	}

	@Test // DATAREST-956
	void writesArrayWithAddedItemForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item());

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ " + "{ \"some\" : \"value1\" },"
				+ "{ \"some\" : \"value2\" }," + "{ \"some\" : \"value3\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items).hasSize(3);
		assertThat(result.inner.items.get(0).some).isEqualTo("value1");
		assertThat(result.inner.items.get(1).some).isEqualTo("value2");
		assertThat(result.inner.items.get(2).some).isEqualTo("value3");
	}

	@Test // DATAREST-956
	void writesArrayWithRemovedItemForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item("test1"));
		inner.items.add(new Item("test2"));
		inner.items.add(new Item("test3"));

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items).hasSize(1);
		assertThat(result.inner.items.get(0).some).isEqualTo("value");
	}

	@Test // DATAREST-959
	void addsElementToPreviouslyEmptyCollection() throws Exception {

		Parent source = new Parent();
		source.inner = new Child();
		source.inner.items = null;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items).hasSize(1);
		assertThat(result.inner.items.get(0).some).isEqualTo("value");
	}

	@Test // DATAREST-959
	@SuppressWarnings("unchecked")
	void turnsObjectIntoCollection() throws Exception {

		Parent source = new Parent();
		source.inner = new Child();
		source.inner.object = new Item("value");

		JsonNode node = new ObjectMapper()
				.readTree("{ \"inner\" : { \"object\" : [ { \"some\" : \"value\" }, { \"some\" : \"otherValue\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());
		assertThat(result.inner.object).isInstanceOf(Collection.class);

		Collection<?> collection = (Collection<?>) result.inner.object;
		assertThat(collection).hasSize(2);

		Iterator<Map<String, Object>> iterator = (Iterator<Map<String, Object>>) collection.iterator();
		assertThat(iterator.next().get("some")).isEqualTo("value");
		assertThat(iterator.next().get("some")).isEqualTo("otherValue");
	}

	@Test // DATAREST-965
	void writesObjectWithRemovedItemsForPut() throws Exception {

		Child inner = new Child();
		inner.items = new ArrayList<Item>();
		inner.items.add(new Item("test1"));
		inner.items.add(new Item("test2"));

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"object\" : \"value\" } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items).isNull();
		assertThat((String) result.inner.object).isEqualTo("value");
	}

	@Test // DATAREST-965
	void writesArrayWithRemovedObjectForPut() throws Exception {

		Child inner = new Child();
		inner.object = "value";

		Parent source = new Parent();
		source.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"items\" : [ { \"some\" : \"value\" } ] } }");

		Parent result = reader.readPut((ObjectNode) node, source, new ObjectMapper());

		assertThat(result.inner.items).hasSize(1);
		assertThat(result.inner.items.get(0).some).isEqualTo("value");
		assertThat(result.inner.object).isNull();
	}

	@Test // DATAREST-986
	void readsComplexMap() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(
				"{ \"map\" : { \"en\" : { \"value\" : \"eventual\" }, \"de\" : { \"value\" : \"schlussendlich\" } } }");

		Product result = reader.readPut((ObjectNode) node, new Product(), mapper);

		assertThat(result.map.get(Locale.ENGLISH)).isEqualTo(new LocalizedValue("eventual"));
		assertThat(result.map.get(Locale.GERMAN)).isEqualTo(new LocalizedValue("schlussendlich"));
	}

	@Test // DATAREST-987
	void handlesTransientPropertyWithoutFieldProperly() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{ \"name\" : \"Foo\" }");

		reader.readPut((ObjectNode) node, new TransientReadOnlyProperty(), mapper);
	}

	@Test // DATAREST-977
	void readsCollectionOfComplexEnum() throws Exception {

		CollectionOfEnumWithMethods sample = new CollectionOfEnumWithMethods();
		sample.enums.add(SampleEnum.FIRST);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("{ \"enums\" : [ \"SECOND\", \"FIRST\" ] }");

		@SuppressWarnings("deprecation")
		CollectionOfEnumWithMethods result = reader.merge((ObjectNode) node, sample, mapper);

		assertThat(result.enums).containsExactly(SampleEnum.SECOND, SampleEnum.FIRST);
	}

	@Test // DATAREST-944
	void mergesAssociations() {

		List<Nested> originalCollection = Arrays.asList(new Nested(2, 3));
		SampleWithReference source = new SampleWithReference(Arrays.asList(new Nested(1, 2), new Nested(2, 3)));
		SampleWithReference target = new SampleWithReference(originalCollection);

		SampleWithReference result = reader.mergeForPut(source, target, new ObjectMapper());

		assertThat(result.nested).isEqualTo(source.nested);
		assertThat(result.nested == originalCollection).isFalse();
	}

	@Test // DATAREST-944
	void mergesAssociationsAndKeepsMutableCollection() {

		ArrayList<Nested> originalCollection = new ArrayList<Nested>(Arrays.asList(new Nested(2, 3)));
		SampleWithReference source = new SampleWithReference(
				new ArrayList<Nested>(Arrays.asList(new Nested(1, 2), new Nested(2, 3))));
		SampleWithReference target = new SampleWithReference(originalCollection);

		SampleWithReference result = reader.mergeForPut(source, target, new ObjectMapper());

		assertThat(result.nested).isEqualTo(source.nested);
		assertThat(result.nested).isSameAs(originalCollection);
	}

	@Test // DATAREST-1030
	void patchWithReferenceToRelatedEntityIsResolvedCorrectly() throws Exception {

		Associations associations = mock(Associations.class);
		PersistentProperty<?> any = ArgumentMatchers.any(PersistentProperty.class);
		when(associations.isLinkableAssociation(any)).thenReturn(true);

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

		assertThat(result.tags).contains(second);
	}

	@Test // DATAREST-1249
	void mergesIntoUninitializedCollection() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode source = (ObjectNode) mapper.readTree("{ \"strings\" : [ \"value\" ] }");

		WithNullCollection result = reader.readPut(source, new WithNullCollection(), mapper);

		assertThat(result.strings).containsExactly("value");
	}

	@Test // DATAREST-1383
	void doesNotWipeReadOnlyPropertyForPatch() throws Exception {

		SampleUser user = new SampleUser("name", "password");
		user.lastLogin = new Date();
		user.email = "foo@bar.com";

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode source = (ObjectNode) mapper.readTree("{ \"lastLogin\" : null, \"email\" : \"bar@foo.com\"}");

		@SuppressWarnings("deprecation")
		SampleUser result = reader.merge(source, user, mapper);

		assertThat(result.lastLogin).isNotNull();
		assertThat(result.email).isEqualTo("foo@bar.com");
	}

	@Test // DATAREST-1068
	void arraysCanBeResizedDuringMerge() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		ArrayHolder target = new ArrayHolder(new String[] {});
		JsonNode node = mapper.readTree("{ \"array\" : [ \"new\" ] }");

		ArrayHolder updated = reader.doMerge((ObjectNode) node, target, mapper);
		assertThat(updated.array).containsExactly("new");
	}

	@Test // #2130
	void writesPolymorphicArrayWithSwitchedItemForPut() throws Exception {

		Apple apple = new Apple();
		apple.apple = "apple";
		apple.color = "red";
		apple.ignored = "ignored";

		Pear pear = new Pear();
		pear.pear = "pear";

		Fruit result = reader.mergeForPut(pear, apple, new ObjectMapper());

		assertThat(result).isInstanceOfSatisfying(Pear.class, it -> {

			// Exposed property is wiped as expected for PUT
			assertThat(it.color).isNull();

			// Non-exposed state is transferred
			assertThat(it.ignored).isEqualTo("ignored");

			// Type specific state applied, too
			assertThat(it.pear).isEqualTo("pear");
		});
	}

	@Test // #2174
	void nestedEntitiesWithReadonlyFieldAreKeptForPut() throws Exception {

		Inner inner = new Inner();
		inner.name = "inner name";
		inner.prop = "something";
		inner.readOnly = "readonly value";
		inner.hidden = "hidden value";

		Outer outer = new Outer();
		outer.prop = "else";
		outer.name = "outer name";
		outer.inner = inner;

		JsonNode node = new ObjectMapper().readTree("{ \"inner\" : { \"name\" : \"new inner name\" } }");

		Outer result = reader.readPut((ObjectNode) node, outer, new ObjectMapper());

		assertThat(result).isSameAs(outer);
		assertThat(result.prop).isNull();
		assertThat(result.inner.prop).isNull();
		assertThat(result.inner.name).isEqualTo("new inner name");
		assertThat(result.inner.readOnly).isEqualTo("readonly value");
		assertThat(result.inner.hidden).isEqualTo("hidden value");
		assertThat(result.inner).isSameAs(inner);
	}

	@Test // GH-2261
	void deserializesCustomCollectionOfPrimitives() throws Exception {

		var node = new ObjectMapper().readTree("""
				{ "longs" : [ "foo:1", "bar:2" ] }
				""");

		var collection = new WithCustomMappedPrimitiveCollection();
		collection.longs = List.of(3L);

		var result = reader.doMerge((ObjectNode) node, collection, new ObjectMapper());

		assertThat(result.longs).isEqualTo(List.of(1L, 2L));
	}

	@Test // GH-2264
	void nestedEntitiesAreCreatedWhenMissingForPut() throws Exception {

		var outer = new Outer();
		outer.name = "outer name";
		outer.prop = "something";

		var node = new ObjectMapper().readTree(
				"{ \"inner\" : { \"name\" : \"new inner name\", \"readOnly\" : \"readonly value\", \"hidden\" : \"hidden value\" } }");

		var result = reader.readPut((ObjectNode) node, outer, new ObjectMapper());

		assertThat(result).isSameAs(outer);
		assertThat(result.inner).isNotNull();
		assertThat(result.inner.prop).isNull();
		assertThat(result.inner.name).isEqualTo("new inner name");
		assertThat(result.inner.readOnly).isNull();
		assertThat(result.inner.hidden).isNull();
	}

	@Test
	void deserializesNewNestedEntitiesCorrectly() throws Exception {

		var mapper = new ObjectMapper();
		var node = mapper.readTree("{ \"list\" : [ { \"value\" : \"Foo\" }, { \"value\" : \"Bar\" }] }");

		var nested = new BugModel.NestedModel();
		nested.value = "FooBar";

		var model = new BugModel();
		model.list = new ArrayList<>();
		model.list.add(nested);

		var result = reader.doMerge((ObjectNode) node, model, mapper);

		assertThat(result.list)
				.extracting(it -> it.value)
				.containsExactly("Foo", "Bar");
	}

	@Test // #2325
	void arraysCanMutateAndAppendDuringMerge() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ArrayHolder target = new ArrayHolder(new String[] { "ancient", "old", "older" });
		JsonNode node = mapper.readTree("{ \"array\" : [ \"new\", \"old\", \"newer\", \"bleeding edge\" ] }");

		ArrayHolder updated = reader.doMerge((ObjectNode) node, target, mapper);

		assertThat(updated.array).containsExactly("new", "old", "newer", "bleeding edge");
	}

	@Test // #2325
	void arraysCanAppendMoreThanOneElementDuringMerge() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ArrayListHolder target = new ArrayListHolder("ancient", "old", "older");
		JsonNode node = mapper.readTree("{ \"values\" : [ \"ancient\", \"old\", \"older\", \"new\", \"newer\" ] }");

		ArrayListHolder updated = reader.doMerge((ObjectNode) node, target, mapper);

		assertThat(updated.values).containsExactly("ancient", "old", "older", "new", "newer");
	}

	@Test // #2325
	void arraysCanRemoveElementsDuringMerge() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ArrayHolder target = new ArrayHolder(new String[] { "ancient", "old", "older" });
		JsonNode node = mapper.readTree("{ \"array\" : [ \"ancient\" ] }");

		ArrayHolder updated = reader.doMerge((ObjectNode) node, target, mapper);

		assertThat(updated.array).containsExactly("ancient");
	}

	@Test // GH-2350
	void replacesArrays() throws Exception {

		ArrayHolder holder = new ArrayHolder(new String[] { "original" });

		ObjectMapper mapper = new ObjectMapper();

		JsonNode node = mapper.readTree("{ \"array\" : [ \"first\", \"update\" ] }");
		ArrayHolder result = reader.doMerge((ObjectNode) node, holder, mapper);

		node = mapper.readTree("{ \"array\" : [ \"second\", \"update\" ] }");
		result = reader.doMerge((ObjectNode) node, holder, mapper);

		assertThat(result.getArray()).isEqualTo(new String[] { "second", "update" });
	}

	@Test // GH-2350
	void replacesNestedArrays() throws Exception {

		MapWrapper wrapper = new MapWrapper();
		wrapper.map.put("array", new String[] { "original" });

		ObjectMapper mapper = new ObjectMapper();

		JsonNode node = mapper.readTree("{ \"map\" : { \"array\" : [ \"first\", \"update\" ] } }");
		MapWrapper result = reader.doMerge((ObjectNode) node, wrapper, mapper);

		node = mapper.readTree("{ \"map\" : { \"array\" : [ \"second\", \"update\" ] } }");
		result = reader.doMerge((ObjectNode) node, wrapper, mapper);

		assertThat(result.map.get("array")).isEqualTo(new String[] { "second", "update" });
	}

	@Test // GH-2357
	void addsElementToPreviouslyEmptyCollectionForPatch() throws Exception {

		Child child = new Child();
		child.items = new ArrayList<>();

		JsonNode node = new ObjectMapper()
				.readTree("{ \"items\" : [ { \"some\" : \"value\" }, { \"some\" : \"otherValue\" } ] }");

		Child result = reader.doMerge((ObjectNode) node, child, new ObjectMapper());

		assertThat(result.items).hasSize(2);
		assertThat(result.items.get(0).some).isEqualTo("value");
		assertThat(result.items.get(1).some).isEqualTo("otherValue");
	}

	@Test // GH-2357
	void augmentsCollectionForPatch() throws Exception {

		Child child = new Child();
		child.items = new ArrayList<>(Arrays.asList(new Item("old")));

		JsonNode node = new ObjectMapper()
				.readTree(
						"{ \"items\" : [ { \"some\" : \"value\" }, { \"some\" : \"otherValue\" }, { \"some\" : \"yetAnotherValue\" } ] }");

		Child result = reader.doMerge((ObjectNode) node, child, new ObjectMapper());

		assertThat(result.items).hasSize(3);
		assertThat(result.items.get(0).some).isEqualTo("value");
		assertThat(result.items.get(1).some).isEqualTo("otherValue");
		assertThat(result.items.get(2).some).isEqualTo("yetAnotherValue");
	}
	
	@Test // GH-2407
	void deserializesAnySetterForPatch() throws Exception {

		Slots slots = new Slots();
		slots.slots.add(new Slot("slot-1", 1));
		slots.slots.add(new Slot("slot-2", null));
		slots.slots.add(new Slot("slot-3", 3));
		slots.slots.add(new Slot("slot-4", 4));

		SlotsContainer container = new SlotsContainer();
		container.slots = slots;

		// changing value of slot-1, setting one for slot-2, removing for slot-4, leaving slot-3 unchanged
		JsonNode node = new ObjectMapper()
				.readTree("{ \"slots\" : { \"slot-1\" : 12 , \"slot-2\" : 2, \"slot-4\" : null } }");

		SlotsContainer result = reader.doMerge((ObjectNode) node, container, new ObjectMapper());

		final List<Slot> list = new ArrayList<>(result.slots.slots);
		assertThat(list).hasSize(4);
		assertThat(list.get(0).name).isEqualTo("slot-1");
		assertThat(list.get(0).value).isEqualTo(12);
		assertThat(list.get(1).name).isEqualTo("slot-2");
		assertThat(list.get(1).value).isEqualTo(2);
		assertThat(list.get(2).name).isEqualTo("slot-3");
		assertThat(list.get(2).value).isEqualTo(3);
		assertThat(list.get(3).name).isEqualTo("slot-4");
		assertThat(list.get(3).value).isNull();
	}

	@SuppressWarnings("unchecked")
	private static <T> T as(Object source, Class<T> type) {

		assertThat(source).isInstanceOf(type);
		return (T) source;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class SampleUser {

		String name;
		@JsonIgnore String password;
		Map<String, SampleUser> relatedUsers;

		@JsonProperty(access = READ_ONLY) //
		private Date lastLogin;

		@ReadOnlyProperty //
		private String email;

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
		@JsonProperty(access = READ_ONLY) String readOnly;
		@JsonIgnore String hidden;
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
	static class Item {
		String some;

		public Item(String some) {
			this.some = some;
		}

		public Item() {}
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Product {
		Map<Locale, LocalizedValue> map = new HashMap<Locale, LocalizedValue>();
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class LocalizedValue {
		String value;

		public LocalizedValue(String value) {
			this.value = value;
		}

		public LocalizedValue() {}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof LocalizedValue)) {
				return false;
			}
			final LocalizedValue other = (LocalizedValue) o;
			if (!other.canEqual(this)) {
				return false;
			}
			final Object this$value = this.value;
			final Object other$value = other.value;
			if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof LocalizedValue;
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $value = this.value;
			result = result * PRIME + ($value == null ? 43 : $value.hashCode());
			return result;
		}
	}

	@JsonAutoDetect(getterVisibility = Visibility.ANY)
	static class TransientReadOnlyProperty {

		@Transient
		public String getName() {
			return null;
		}

		void setName(String name) {}
	}

	// DATAREST-977

	interface EnumInterface {
		String getFoo();
	}

	enum SampleEnum implements EnumInterface {

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

	static class SampleWithReference {
		private @Reference List<Nested> nested;

		public SampleWithReference(List<Nested> nested) {
			this.nested = nested;
		}

		public List<Nested> getNested() {
			return this.nested;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			SampleWithReference that = (SampleWithReference) o;

			return ObjectUtils.nullSafeEquals(nested, that.nested);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(nested);
		}
	}

	@Immutable
	static final class Nested {
		private final int x, y;

		public Nested(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}
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

	static class SelectValueByIdSerializer<T> extends JsonDeserializer<T> {

		private final Map<? extends Object, T> values;

		public SelectValueByIdSerializer(Map<? extends Object, T> values) {
			this.values = values;
		}

		@Override
		public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			String text = p.getText();

			return values.entrySet().stream()//
					.filter(it -> it.getKey().toString().equals(text))//
					.map(it -> it.getValue())//
					.findFirst().orElse(null);
		}
	}

	// DATAREST-1249

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class WithNullCollection {
		List<String> strings;
	}

	// DATAREST-1068
	static final class ArrayHolder {

		private final String[] array;

		ArrayHolder(String[] array) {
			this.array = array;
		}

		public String[] getArray() {
			return array;
		}
	}

	// DATAREST-1026

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Basket {

		@Id Long id;
		List<Fruit> fruits;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(name = "Apple", value = Apple.class),
			@JsonSubTypes.Type(name = "Pear", value = Pear.class) })
	static class Fruit {
		String color;
		@JsonIgnore String ignored;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Apple extends Fruit {
		String apple;
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Pear extends Fruit {
		String pear;
	}

	// GH-2261
	static class WithCustomMappedPrimitiveCollection {

		@JsonDeserialize(contentUsing = CustomDeserializer.class) //
		List<Long> longs;

		@SuppressWarnings("serial")
		static class CustomDeserializer extends StdDeserializer<Long> {

			protected CustomDeserializer() {
				super(Long.class);
			}

			/*
			 * (non-Javadoc)
			 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
			 */
			@Override
			public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {

				var elements = p.getText().split(":");

				return Long.valueOf(elements[elements.length - 1]);
			}
		}
	}

	// GH-2287
	static class BugModel {

		public List<NestedModel> list;

		static class NestedModel {
			public String value;
		}
	}

	static class ArrayListHolder {
		Collection<String> values;

		ArrayListHolder(String... values) {
			this.values = new ArrayList<>(Arrays.asList(values));
		}

		public void setValues(Collection<String> values) {
			this.values = values;
		}
	}

	static class MapWrapper {
		public Map<String, Object> map = new HashMap<>();
	}
	
	// GH-2407

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class SlotsContainer {

		Slots slots;

	}

	static class Slots {

		// The internal, non-json representation is a Set
		@JsonIgnore private SortedSet<Slot> slots = new TreeSet<>();

		@JsonAnySetter
		public void put(final String name, final Integer value) {
			slots.removeIf(slot -> slot.name.equals(name));
			slots.add(new Slot(name, value));
		}

		// We expose however a Map
		@JsonAnyGetter
		public Map<String, Integer> toMap() {
			final Map<String, Integer> map = new HashMap<>();
			slots.forEach(slot -> map.put(slot.name, slot.value));
			return map;
		}
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Slot implements Comparable<Slot> {

		String name;
		Integer value;

		Slot(String name, Integer value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public int compareTo(Slot o) {
			return name.compareTo(o.name);
		}

	}
}
