/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Unit tests for {@link MappingResourceMetadata}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingResourceMetadataUnitTests {

	KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();

	KeyValuePersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Entity.class);
	ResourceMappings resourceMappings = new PersistentEntitiesResourceMappings(
			new PersistentEntities(Arrays.asList(context)));
	MappingResourceMetadata metadata = new MappingResourceMetadata(entity, resourceMappings);

	@Test // DATAREST-514
	public void allowsLookupOfPropertyByMappedName() {

		KeyValuePersistentProperty<?> property = entity.getRequiredPersistentProperty("related");

		PropertyAwareResourceMapping propertyMapping = metadata.getProperty("foo");

		assertThat(propertyMapping).isNotNull();
		assertThat(propertyMapping.getProperty()).isEqualTo(property);
		assertThat(metadata.getMappingFor(property).getPath().matches("foo")).isTrue();
	}

	@Test // DATAREST-518
	public void isNotExportedByDefault() {

		assertThat(metadata.isExported()).isFalse();
	}

	@Test // DATAREST-518
	public void isExportedIfExplicitlyAnnotated() {

		MappingResourceMetadata metadata = new MappingResourceMetadata(context.getRequiredPersistentEntity(Related.class),
				resourceMappings);
		assertThat(metadata.isExported()).isTrue();
	}

	static class Entity {
		@Reference @RestResource(rel = "foo", path = "foo") private Related related;
	}

	@RestResource
	static class Related {

	}
}
