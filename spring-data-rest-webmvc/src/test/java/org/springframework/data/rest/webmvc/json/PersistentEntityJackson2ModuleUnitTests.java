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

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.json.JacksonSerializers.EnumTranslatingDeserializer;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.AssociationOmittingSerializerModifier;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.AssociationUriResolvingDeserializerModifier;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.LookupObjectSerializer;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.NestedEntitySerializer;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ResourceProcessorInvoker;
import org.springframework.plugin.core.OrderAwarePluginRegistry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.JsonPath;

/**
 * Unit tests for {@link PersistentEntityJackson2Module}.
 * 
 * @author Oliver Gierke
 * @author Valentin Rentschler
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityJackson2ModuleUnitTests {

	@Mock Associations associations;
	@Mock UriToEntityConverter converter;
	@Mock EntityLinks entityLinks;
	@Mock ResourceMappings mappings;
	@Mock SelfLinkProvider selfLinks;

	PersistentEntities persistentEntities;
	ObjectMapper mapper;

	@Before
	public void setUp() {

		KeyValueMappingContext mappingContext = new KeyValueMappingContext();
		mappingContext.getPersistentEntity(Sample.class);
		mappingContext.getPersistentEntity(SampleWithAdditionalGetters.class);
		mappingContext.getPersistentEntity(SampleWithEnumContainer.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.PetOwner.class);

		this.persistentEntities = new PersistentEntities(asList(mappingContext));

		ResourceProcessorInvoker invoker = new ResourceProcessorInvoker(Collections.<ResourceProcessor<?>> emptyList());

		NestedEntitySerializer nestedEntitySerializer = new NestedEntitySerializer(persistentEntities,
				new EmbeddedResourcesAssembler(persistentEntities, associations, mock(ExcerptProjector.class)), invoker);
		OrderAwarePluginRegistry<EntityLookup<?>, Class<?>> lookups = OrderAwarePluginRegistry.create();

		SimpleModule module = new SimpleModule();

		module.setSerializerModifier(new AssociationOmittingSerializerModifier(persistentEntities, associations,
				nestedEntitySerializer, new LookupObjectSerializer(lookups)));
		module.setDeserializerModifier(new AssociationUriResolvingDeserializerModifier(persistentEntities, associations,
				converter, mock(RepositoryInvokerFactory.class)));
		module.addDeserializer(Enum.class, new EnumTranslatingDeserializer(new EnumTranslator(new MessageSourceAccessor(new StaticMessageSource()))));
		this.mapper = new ObjectMapper();
		this.mapper.registerModule(module);
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

		when(associations.isLinkableAssociation(property)).thenReturn(true);
		when(converter.convert(new UriTemplate("/pets/1").expand(), TypeDescriptor.valueOf(URI.class),
				TypeDescriptor.valueOf(Pet.class))).thenReturn(new Cat());

		PetOwner petOwner = mapper.readValue("{\"pet\":\"/pets/1\"}", PetOwner.class);

		assertThat(petOwner, is(notNullValue()));
		assertThat(petOwner.getPet(), is(notNullValue()));
	}

	/**
	 * @see DATAREST-929
	 */
	@Test
	public void deserializeEnumCollectionAndArray() throws IOException {

		SampleWithEnumContainer result = mapper.readValue("{\"enumContainerSet\": [\"SECOND\", \"FIRST\"], \"enumContainerArray\": [\"SECOND\", \"FIRST\"]}",
				SampleWithEnumContainer.class);

		assertThat(result, is(notNullValue()));

		assertThat(result.enumContainerSet.size(), is(2));
		assertTrue(result.enumContainerSet.contains(SampleEnum.FIRST));
		assertTrue(result.enumContainerSet.contains(SampleEnum.SECOND));

		assertThat(result.enumContainerArray.length, is(2));
		assertTrue(asList(result.enumContainerArray).contains(SampleEnum.FIRST));
		assertTrue(asList(result.enumContainerArray).contains(SampleEnum.SECOND));
	}

	static class PetOwner {

		Pet pet;

		public Pet getPet() {
			return pet;
		}
	}

	@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.MINIMAL_CLASS)
	static class Pet {}

	static class Cat extends Pet {}

	static class Sample {
		public @JsonProperty("foo") String name;
	}

	static class SampleWithAdditionalGetters extends Sample {

		public int getNumber() {
			return 5;
		}
	}

	static class SampleWithEnumContainer {
		public Set<SampleEnum> enumContainerSet;
		public SampleEnum[] enumContainerArray;
	}

	static enum SampleEnum {
		FIRST, SECOND
	}
}
