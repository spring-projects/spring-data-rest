/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.ResourceMappings.PersistentPropertyResourceMapping;

/**
 * Unit tests for {@link PersistentPropertyResourceMapping}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentPropertyResourceMappingUnitTests {

	MongoMappingContext mappingContext = new MongoMappingContext();
	MongoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(Entity.class);

	@Mock ResourceMapping typeMapping;

	@Before
	public void setUp() {
		when(typeMapping.isExported()).thenReturn(true);
	}

	/**
	 * @see DATAREST-175
	 */
	@Test
	public void usesPropertyNameAsDefaultResourceMappingRelAndPath() {

		MongoPersistentProperty persistentProperty = persistentEntity.getPersistentProperty("first");
		ResourceMapping propertyMapping = new PersistentPropertyResourceMapping(persistentProperty, typeMapping);

		assertThat(propertyMapping, is(notNullValue()));
		assertThat(propertyMapping.getPath(), is(new Path("first")));
		assertThat(propertyMapping.getRel(), is("first"));
		assertThat(propertyMapping.isExported(), is(true));
	}

	/**
	 * @see DATAREST-175
	 */
	@Test
	public void considersMappingAnnotationOnDomainClassProperty() {

		MongoPersistentProperty persistentProperty = persistentEntity.getPersistentProperty("second");
		ResourceMapping propertyMapping = new PersistentPropertyResourceMapping(persistentProperty, typeMapping);

		assertThat(propertyMapping, is(notNullValue()));
		assertThat(propertyMapping.getPath(), is(new Path("secPath")));
		assertThat(propertyMapping.getRel(), is("secRel"));
		assertThat(propertyMapping.isExported(), is(false));
	}

	/**
	 * @see DATAREST-175
	 */
	@Test
	public void considersMappingAnnotationOnDomainClassPropertyMethod() {

		MongoPersistentProperty persistentProperty = persistentEntity.getPersistentProperty("third");
		ResourceMapping propertyMapping = new PersistentPropertyResourceMapping(persistentProperty, typeMapping);

		assertThat(propertyMapping, is(notNullValue()));
		assertThat(propertyMapping.getPath(), is(new Path("thirdPath")));
		assertThat(propertyMapping.getRel(), is("thirdRel"));
		assertThat(propertyMapping.isExported(), is(false));
	}

	static class Entity {

		Related first, third;

		@RestResource(path = "secPath", rel = "secRel", exported = false)//
		List<Related> second;

		@RestResource(path = "thirdPath", rel = "thirdRel", exported = false)
		public Related getThird() {
			return third;
		}
	}

	static class Related {

	}
}
