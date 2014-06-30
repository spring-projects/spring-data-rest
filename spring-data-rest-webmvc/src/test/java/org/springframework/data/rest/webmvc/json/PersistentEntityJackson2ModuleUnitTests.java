/*
 * Copyright 2014 the original author or authors.
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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.JsonPath;

/**
 * Unit tests for {@link PersistentEntityJackson2Module}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityJackson2ModuleUnitTests {

	@Mock AssociationLinks associationLinks;

	ObjectMapper mapper;

	@Before
	public void setUp() {

		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.getPersistentEntity(Sample.class);
		mappingContext.getPersistentEntity(SampleWithAdditionalGetters.class);

		PersistentEntities persistentEntities = new PersistentEntities(Arrays.asList(mappingContext));

		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new PersistentEntityJackson2Module.AssociationOmittingSerializerModifier(
				persistentEntities, associationLinks, new RepositoryRestConfiguration()));

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

	static class Sample {
		public @JsonProperty("foo") String name;
	}

	static class SampleWithAdditionalGetters extends Sample {

		public int getNumber() {
			return 5;
		}
	}
}
