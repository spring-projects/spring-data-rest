/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.hateoas.Link;

/**
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssociationsUnitTests {

	@Mock RepositoryRestConfiguration configuration;
	@Mock ProjectionDefinitionConfiguration projectionDefinitionConfiguration;

	@Mock PersistentEntity<?, ?> entity;
	@Mock PersistentProperty<?> property;

	Associations associations;

	KeyValueMappingContext<?, ?> mappingContext;
	ResourceMappings mappings;

	@BeforeEach
	void setUp() {
		doReturn(projectionDefinitionConfiguration).when(configuration).getProjectionConfiguration();

		this.mappingContext = new KeyValueMappingContext<>();
		this.mappingContext.getPersistentEntity(Root.class);

		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));

		this.associations = new Associations(mappings, configuration);
	}

	@Test
	void rejectsNullMappings() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new Associations(null, configuration));
	}

	@Test
	void rejectsNullConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new Associations(mappings, null));
	}

	@Test
	void handlesNullPropertyForLookupTypeCheck() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> associations.isLookupType(null));
	}

	@Test
	void forwardsLookupTypeCheckToConfiguration() {

		doReturn(Root.class).when(property).getActualType();
		assertThat(associations.isLookupType(property)).isFalse();

		doReturn(true).when(configuration).isLookupType(Root.class);
		assertThat(associations.isLookupType(property)).isTrue();
	}

	@Test
	void forwardsIdExposureCheckToConfiguration() {

		doReturn(Root.class).when(entity).getType();
		assertThat(associations.isIdExposed(entity)).isFalse();

		doReturn(true).when(configuration).isIdExposedFor(Root.class);
		assertThat(associations.isIdExposed(entity)).isTrue();
	}

	@Test
	void exposesConfiguredMapping() {
		assertThat(associations.getMappings()).isEqualTo(mappings);
	}

	@Test
	void forwardsMetadataLookupToMappings() {
		assertThat(associations.getMetadataFor(Root.class)).isNotNull();
	}

	@Test
	void detectsAssociationLinks() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedAndExported"), new Path(""));

		assertThat(links).hasSize(1);
		assertThat(links).contains(Link.of("/relatedAndExported", "relatedAndExported"));
	}

	@Test
	void doesNotCreateAssociationLinkIfTargetIsNotExported() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedButNotExported"), new Path(""));

		assertThat(links).hasSize(0);
	}

	@Test // DATAREST-1105
	void detectsProjectionsForAssociationLinks() {

		String projectionParameterName = "projection";

		doReturn(true).when(projectionDefinitionConfiguration).hasProjectionFor(RelatedAndExported.class);
		doReturn(projectionParameterName).when(projectionDefinitionConfiguration).getParameterName();

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedAndExported"), new Path(""));

		assertThat(links).hasSize(1);
		assertThat(links).contains(Link.of("/relatedAndExported{?" + projectionParameterName + "}", "relatedAndExported"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Association<? extends PersistentProperty<?>> getAssociation(Class<?> type, String name) {

		KeyValuePersistentEntity<?, ? extends KeyValuePersistentProperty<?>> rootEntity = mappingContext
				.getRequiredPersistentEntity(type);
		KeyValuePersistentProperty<?> property = rootEntity.getRequiredPersistentProperty(name);

		return new Association(property, null);
	}

	static class Root {
		@Reference RelatedAndExported relatedAndExported;
		@Reference RelatedButNotExported relatedButNotExported;
	}

	@RestResource(exported = true)
	static class RelatedAndExported {}

	static class RelatedButNotExported {}
}
