/*
 * Copyright 2014-2015 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.hateoas.UriTemplate;

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

	@Mock AssociationLinks associationLinks;
	@Mock UriToEntityConverter converter;

	PersistentEntities persistentEntities;
	ObjectMapper mapper;

	@Before
	public void setUp() {

		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.getPersistentEntity(Sample.class);
		mappingContext.getPersistentEntity(SampleWithAdditionalGetters.class);
		mappingContext.getPersistentEntity(PersistentEntityJackson2ModuleUnitTests.PetOwner.class);

		this.persistentEntities = new PersistentEntities(Arrays.asList(mappingContext));

		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new PersistentEntityJackson2Module.AssociationOmittingSerializerModifier(
				persistentEntities, associationLinks, new RepositoryRestConfiguration()));

		module.setDeserializerModifier(new PersistentEntityJackson2Module.AssociationUriResolvingDeserializerModifier(
				persistentEntities, converter, associationLinks));

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

		when(associationLinks.isLinkableAssociation(property)).thenReturn(true);
		when(converter.convert(new UriTemplate("/pets/1").expand(), TypeDescriptor.valueOf(URI.class),
				TypeDescriptor.valueOf(Pet.class))).thenReturn(new Cat());

		PetOwner petOwner = mapper.readValue("{\"pet\":\"/pets/1\"}", PetOwner.class);

		assertThat(petOwner, is(notNullValue()));
		assertThat(petOwner.getPet(), is(notNullValue()));
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
}
