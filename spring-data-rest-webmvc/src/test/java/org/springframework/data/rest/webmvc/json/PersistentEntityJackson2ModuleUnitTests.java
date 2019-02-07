/*
 * Copyright 2014-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
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
import org.springframework.plugin.core.PluginRegistry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
	@Mock RepositoryInvokerFactory factory;

	PersistentEntities persistentEntities;
	ObjectMapper mapper;

	@Before
	public void setUp() {

		KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();
		mappingContext.getPersistentEntity(Sample.class);
		mappingContext.getPersistentEntity(SampleWithAdditionalGetters.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.PetOwner.class);

		this.persistentEntities = new PersistentEntities(Arrays.asList(mappingContext));

		ResourceProcessorInvoker invoker = new ResourceProcessorInvoker(Collections.<ResourceProcessor<?>> emptyList());

		NestedEntitySerializer nestedEntitySerializer = new NestedEntitySerializer(persistentEntities,
				new EmbeddedResourcesAssembler(persistentEntities, associations, mock(ExcerptProjector.class)), invoker);
		SimpleModule module = new SimpleModule();

		module.setSerializerModifier(new AssociationOmittingSerializerModifier(persistentEntities, associations,
				nestedEntitySerializer, new LookupObjectSerializer(PluginRegistry.of(new HomeLookup()))));
		module.setDeserializerModifier(
				new AssociationUriResolvingDeserializerModifier(persistentEntities, associations, converter, factory));

		this.mapper = new ObjectMapper();
		this.mapper.registerModule(module);
	}

	@Test // DATAREST-328, DATAREST-320
	public void doesNotDropPropertiesWithCustomizedNames() throws Exception {

		Sample sample = new Sample();
		sample.name = "bar";

		String result = mapper.writeValueAsString(sample);

		assertThat(JsonPath.<String> read(result, "$.foo")).isEqualTo("bar");
	}

	@Test // DATAREST-340
	public void rendersAdditionalJsonPropertiesNotBackedByAPersistentField() throws Exception {

		SampleWithAdditionalGetters sample = new SampleWithAdditionalGetters();

		String result = mapper.writeValueAsString(sample);

		assertThat(JsonPath.<Integer> read(result, "$.number")).isEqualTo(5);
	}

	@Test // DATAREST-662
	public void resolvesReferenceToSubtypeCorrectly() throws IOException {

		PersistentProperty<?> property = persistentEntities.getRequiredPersistentEntity(PetOwner.class)
				.getRequiredPersistentProperty("pet");

		when(associations.isLinkableAssociation(property)).thenReturn(true);
		when(converter.convert(new UriTemplate("/pets/1").expand(), TypeDescriptor.valueOf(URI.class),
				TypeDescriptor.valueOf(Pet.class))).thenReturn(new Cat());

		PetOwner petOwner = mapper.readValue("{\"pet\":\"/pets/1\"}", PetOwner.class);

		assertThat(petOwner).isNotNull();
		assertThat(petOwner.getPet()).isNotNull();
	}

	@Test // DATAREST-1321
	public void allowsNumericIdsForLookupTypes() throws Exception {

		RepositoryInvoker invoker = mock(RepositoryInvoker.class);
		when(invoker.invokeFindById(any(Long.class))).thenReturn(Optional.of(new Home()));

		when(factory.getInvokerFor(Home.class)).thenReturn(invoker);

		PersistentProperty<?> property = persistentEntities.getRequiredPersistentEntity(PetOwner.class)
				.getRequiredPersistentProperty("home");

		when(associations.isLookupType(property)).thenReturn(true);

		PetOwner petOwner = mapper.readValue("{\"home\": 1 }", PetOwner.class);

		assertThat(petOwner).isNotNull();
		assertThat(petOwner.getHome()).isInstanceOf(Home.class);
	}

	@Test // DATAREST-1321
	public void serializesNonStringLookupValues() throws Exception {

		// Given Pet defined as lookup type
		PersistentProperty<?> property = persistentEntities.getRequiredPersistentEntity(PetOwner.class)
				.getRequiredPersistentProperty("home");
		when(associations.isLookupType(property)).thenReturn(true);

		// When a Pet is rendered
		PetOwner owner = new PetOwner();
		owner.home = new Home();

		String result = mapper.writeValueAsString(owner);

		// The it appears as numeric value
		assertThat(JsonPath.parse(result).read("$.home", Integer.class)) //
				.isEqualTo(41);
	}

	/**
	 * @author Oliver Gierke
	 */
	private static class HomeLookup implements EntityLookup<Home> {

		@Override
		public Object getResourceIdentifier(Home entity) {
			return 41;
		}

		@Override
		public boolean supports(Class<?> delimiter) {
			return delimiter.equals(Home.class);
		}

		@Override
		public Optional<Home> lookupEntity(Object id) {
			return Optional.of(new Home());
		}
	}

	@Getter
	@JsonInclude(Include.NON_NULL)
	static class PetOwner {

		Pet pet;
		Home home;
	}

	@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.MINIMAL_CLASS)
	static class Pet {}

	static class Cat extends Pet {}

	static class Home {}

	static class Sample {
		public @JsonProperty("foo") String name;
	}

	static class SampleWithAdditionalGetters extends Sample {

		public int getNumber() {
			return 5;
		}
	}
}
