/*
 * Copyright 2014-2016 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.projection.SubTypeAwareProxyProjectionFactory;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ResourceProcessorInvoker;
import org.springframework.plugin.core.OrderAwarePluginRegistry;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PersistentEntityJackson2Module}.
 *
 * @author Oliver Gierke
 * @author Valentin Rentschler
 * @author Anton Koscejev
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityJackson2ModuleUnitTests {

	@Mock Associations associations;
	@Mock UriToEntityConverter converter;
	@Mock EntityLinks entityLinks;
	@Mock LinkCollector collector;

	PersistentEntities persistentEntities;
	ObjectMapper mapper;
	SubTypeAwareProxyProjectionFactory projectionFactory;

	@Before
	public void setUp() {

		KeyValueMappingContext mappingContext = new KeyValueMappingContext();
		mappingContext.getPersistentEntity(Sample.class);
		mappingContext.getPersistentEntity(SampleWithAdditionalGetters.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.PetOwner.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.Pet.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.Cat.class);

		persistentEntities = new PersistentEntities(singletonList(mappingContext));

		ResourceProcessorInvoker invoker = new ResourceProcessorInvoker(Collections.<ResourceProcessor<?>> emptyList());
		EmbeddedResourcesAssembler assembler = new EmbeddedResourcesAssembler(persistentEntities, associations, mock(ExcerptProjector.class));

		OrderAwarePluginRegistry<EntityLookup<?>, Class<?>> lookups = OrderAwarePluginRegistry.create();
		LookupObjectSerializer lookupObjectSerializer = new LookupObjectSerializer(lookups);

		SimpleModule module = new PersistentEntityJackson2Module(
						associations,
						persistentEntities,
						converter,
						collector,
						mock(RepositoryInvokerFactory.class),
						lookupObjectSerializer,
						invoker,
						assembler);

		mapper = new ObjectMapper();
		mapper.registerModule(module);

		projectionFactory = new SubTypeAwareProxyProjectionFactory();
	}

	/**
	 * @see DATAREST-328, DATAREST-320
	 */
	@Test
	public void doesNotDropPropertiesWithCustomizedNames() throws Exception {

		Sample sample = new Sample();
		sample.name = "bar";

		String result = mapper.writeValueAsString(sample);

		assertThat(JsonPath.read(result, "$.foo"), is((Object) "bar"));
	}

	/**
	 * @see DATAREST-340
	 */
	@Test
	public void rendersAdditionalJsonPropertiesNotBackedByAPersistentField() throws Exception {

		SampleWithAdditionalGetters sample = new SampleWithAdditionalGetters();

		String result = mapper.writeValueAsString(sample);
		assertThat(JsonPath.read(result, "$.number"), is((Object) 5));
	}

	/**
	 * @see DATAREST-662
	 */
	@Test
	public void resolvesReferenceToSubtypeCorrectly() throws IOException {

		PersistentProperty<?> property = persistentEntities.getPersistentEntity(PetOwner.class)
						.getPersistentProperty("pet");

		mockLinkableAssociation(property);
		when(converter.convert(new UriTemplate("/pets/1").expand(), TypeDescriptor.valueOf(URI.class),
						TypeDescriptor.valueOf(Pet.class))).thenReturn(new Cat());

		PetOwner petOwner = mapper.readValue("{\"pet\":\"/pets/1\"}", PetOwner.class);

		assertThat(petOwner, is(notNullValue()));
		assertThat(petOwner.getPet(), is(notNullValue()));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void rendersResourceWithContentTypeInfo() throws IOException {
		when(collector.getLinksFor(any(), anyListOf(Link.class))).thenReturn(new Links());
		mockMetadataIsExported(Pet.class, Cat.class);

		PersistentEntity<?, ?> entity = persistentEntities.getPersistentEntity(Pet.class);
		String result = mapper.writeValueAsString(PersistentEntityResource.build(new Cat(), entity).build());

		String petSubType = JsonPath.read(result, "$.type");
		assertThat("Resource should be serialized with type information of its content",
						petSubType, is("Cat"));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void rendersResourceNestedContentTypeInfo() throws IOException {
		when(collector.getLinksFor(any(), anyListOf(Link.class))).thenReturn(new Links());
		when(collector.getLinksForNested(any(), anyListOf(Link.class))).thenReturn(new Links());
		mockMetadataIsExported(PetOwner.class);

		PetOwner catOwner = new PetOwner();
		catOwner.pet = new Cat();

		PersistentEntity<?, ?> entity = persistentEntities.getPersistentEntity(PetOwner.class);
		String result = mapper.writeValueAsString(PersistentEntityResource.build(catOwner, entity).build());

		String petSubType = JsonPath.read(result, "$.pet.type");
		assertThat("Resource wrapper should not interfere with serialization of any sub-content type information",
						petSubType, is("Cat"));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void rendersProjectionWithTargetTypeInfo() throws IOException {
		when(collector.getLinksFor(any(), anyListOf(Link.class))).thenReturn(new Links());
		PersistentEntity<?, ?> entity = persistentEntities.getPersistentEntity(Cat.class);

		mockMetadataIsExported(Pet.class, Cat.class);
		CatProjection projection = projectionFactory.createProjection(CatProjection.class, new Cat());

		String result = mapper.writeValueAsString(PersistentEntityResource.build(projection, entity).build());

		String petSubType = JsonPath.read(result, "$.type");
		assertThat("Projection should be serialized with type information of its target",
						petSubType, is("Cat"));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void rendersNestedSubProjectionWithTypeInfo() throws IOException {
		when(collector.getLinksFor(any())).thenReturn(new Links());

		Cat cat = new Cat();
		cat.whiskerLength = 12;
		PetOwner catOwner = new PetOwner();
		catOwner.pet = cat;

		mockMetadataIsExported(PetOwner.class, Pet.class, Cat.class);
		OwnerProjection projection = projectionFactory.createProjection(OwnerProjection.class, catOwner);
		String result = mapper.writeValueAsString(projection);

		int catProjectionProperty = JsonPath.read(result, "$.pet.whiskerLength");
		assertThat("Nested projection properties should match the sub-projection selected based on source object type",
						catProjectionProperty, is(12));

		String petSubType = JsonPath.read(result, "$.pet.type");
		assertThat("Nested projection should be serialized with type information of source object",
						petSubType, is("Cat"));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void rendersProjectionCustomizedTypeInfo() throws IOException {
		when(collector.getLinksFor(any(), anyListOf(Link.class))).thenReturn(new Links());

		PersistentEntity<?, ?> entity = persistentEntities.getPersistentEntity(Cat.class);

		mockMetadataIsExported(Pet.class, Cat.class);
		// create a projection with custom type information "Kitty"
		Object projection = projectionFactory.createProjection(PetWithCustomTypeInformation.class, new Cat());

		String result = mapper.writeValueAsString(PersistentEntityResource.build(projection, entity).build());
		ReadContext resultJson = JsonPath.parse(result);

		assertThat("Projection should be serialized with its own type information, if provided",
						resultJson.read("$.clazz", String.class), is("Kitty")); // customized via @JsonTypeInfo
	}

	private void mockLinkableAssociation(PersistentProperty<?> property) {
		when(associations.isLinkableAssociation(property)).thenReturn(true);

		mockMetadataIsExported(property.getActualType());
	}

	private void mockMetadataIsExported(Class<?>... types) {
		for (Class<?> type : types) {
			ResourceMetadata metadata = mock(ResourceMetadata.class);
			when(metadata.isExported()).thenReturn(true);

			when(associations.getMetadataFor(type)).thenReturn(metadata);
		}
	}

	static class PetOwner {

		@Reference
		Pet pet;

		public Pet getPet() {
			return pet;
		}
	}

	@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME, property = "type")
	@JsonSubTypes({@JsonSubTypes.Type(Cat.class)})
	static class Pet {}

	@JsonTypeName("Cat")
	static class Cat extends Pet {
		int whiskerLength;

		public int getWhiskerLength() {
			return whiskerLength;
		}
	}

	static class Sample {
		public @JsonProperty("foo") String name;
	}

	static class SampleWithAdditionalGetters extends Sample {

		public int getNumber() {
			return 5;
		}
	}

	@Projection(types = PetOwner.class)
	interface OwnerProjection {
		PetProjection getPet();
	}

	@Projection(types = Pet.class, subProjections = CatProjection.class)
	interface PetProjection {
	}

	@Projection(types = Cat.class)
	interface CatProjection extends PetProjection {
		int getWhiskerLength();
	}

	@Projection(types = Pet.class, subProjections = CatWithCustomTypeInformation.class)
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "clazz") // overrides JsonTypeInfo on Pet
	interface PetWithCustomTypeInformation {
	}

	@Projection(types = Cat.class)
	@JsonTypeName("Kitty")
	interface CatWithCustomTypeInformation extends PetWithCustomTypeInformation {
		int getWhiskerLength();
	}
}