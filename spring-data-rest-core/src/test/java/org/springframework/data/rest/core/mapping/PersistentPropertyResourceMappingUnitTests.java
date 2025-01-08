/*
 * Copyright 2013-2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.LinkRelation;

/**
 * Unit tests for {@link PersistentPropertyResourceMapping}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class PersistentPropertyResourceMappingUnitTests {

	KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();

	@Test // DATAREST-175
	void usesPropertyNameAsDefaultResourceMappingRelAndPath() {

		ResourceMapping mapping = getPropertyMappingFor(Entity.class, "first");

		assertThat(mapping).isNotNull();
		assertThat(mapping.getPath()).isEqualTo(new Path("first"));
		assertThat(mapping.getRel()).isEqualTo(IanaLinkRelations.FIRST);
		assertThat(mapping.isExported()).isFalse();
	}

	@Test // DATAREST-175
	void considersMappingAnnotationOnDomainClassProperty() {

		ResourceMapping mapping = getPropertyMappingFor(Entity.class, "second");

		assertThat(mapping).isNotNull();
		assertThat(mapping.getPath()).isEqualTo(new Path("secPath"));
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("secRel"));
		assertThat(mapping.isExported()).isFalse();
	}

	@Test // DATAREST-175
	void considersMappingAnnotationOnDomainClassPropertyMethod() {

		ResourceMapping mapping = getPropertyMappingFor(Entity.class, "third");

		assertThat(mapping).isNotNull();
		assertThat(mapping.getPath()).isEqualTo(new Path("thirdPath"));
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("thirdRel"));
		assertThat(mapping.isExported()).isFalse();
	}

	@Test // DATAREST-233
	void returnsDefaultDescriptionKey() {

		ResourceMapping mapping = getPropertyMappingFor(Entity.class, "second");

		ResourceDescription description = mapping.getDescription();

		assertThat(description.isDefault()).isTrue();
		assertThat(description.getMessage()).isEqualTo("rest.description.entity.second");
	}

	@Test // DATAREST-233
	void considersAtDescription() {

		ResourceMapping mapping = getPropertyMappingFor(Entity.class, "fourth");

		ResourceDescription description = mapping.getDescription();
		assertThat(description.isDefault()).isFalse();
		assertThat(description.getMessage()).isEqualTo("Some description");
	}

	@Test // #1994
	void doesNotConsiderPropertyExportedIfTargetTypeIsNotMapped() {

		PersistentEntity<?, ?> entity = mappingContext.getRequiredPersistentEntity(Entity.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("third");

		ResourceMappings mappings = mock(ResourceMappings.class);
		when(mappings.getMetadataFor(property.getType())).thenReturn(null);

		ResourceMapping mapping = new PersistentPropertyResourceMapping(property, mappings);

		assertThat(mapping.isExported()).isFalse();
	}

	private ResourceMapping getPropertyMappingFor(Class<?> entity, String propertyName) {

		KeyValuePersistentEntity<?, ?> persistentEntity = mappingContext.getRequiredPersistentEntity(entity);
		KeyValuePersistentProperty<?> property = persistentEntity.getRequiredPersistentProperty(propertyName);

		ResourceMappings resourceMappings = new PersistentEntitiesResourceMappings(
				new PersistentEntities(Arrays.asList(mappingContext)));

		return new PersistentPropertyResourceMapping(property, resourceMappings);
	}

	public static class Entity {

		Related first;
		@Reference Related third;

		@Reference //
		@RestResource(path = "secPath", rel = "secRel", exported = false) //
		List<Related> second;

		@Description("Some description") String fourth;

		@RestResource(path = "thirdPath", rel = "thirdRel", exported = false)
		public Related getThird() {
			return third;
		}
	}

	public static class Related {

	}
}
