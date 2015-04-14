/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Unit tests for {@link MappingResourceMetadata}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingResourceMetadataUnitTests {

	MongoMappingContext context = new MongoMappingContext();

	MongoPersistentEntity<?> entity = context.getPersistentEntity(Entity.class);
	ResourceMappings resourceMappings = new PersistentEntitiesResourceMappings(new PersistentEntities(
			Arrays.asList(context)));
	MappingResourceMetadata metadata = new MappingResourceMetadata(entity, resourceMappings).init();

	/**
	 * @see DATAREST-514
	 */
	@Test
	public void allowsLookupOfPropertyByMappedName() {

		MongoPersistentProperty property = entity.getPersistentProperty("related");

		PropertyAwareResourceMapping propertyMapping = metadata.getProperty("foo");

		assertThat(propertyMapping, is(notNullValue()));
		assertThat(propertyMapping.getProperty(), is((Object) property));
		assertThat(metadata.getMappingFor(property).getPath().matches("foo"), is(true));
	}

	/**
	 * @see DATAREST-518
	 */
	@Test
	public void isNotExportedByDefault() {

		assertThat(metadata.isExported(), is(false));
	}

	/**
	 * @see DATAREST-518
	 */
	@Test
	public void isExportedIfExplicitlyAnnotated() {

		MappingResourceMetadata metadata = new MappingResourceMetadata(context.getPersistentEntity(Related.class),
				resourceMappings).init();
		assertThat(metadata.isExported(), is(true));
	}

	static class Entity {
		@DBRef @RestResource(rel = "foo", path = "foo") private Related related;
	}

	@RestResource
	static class Related {

	}
}
