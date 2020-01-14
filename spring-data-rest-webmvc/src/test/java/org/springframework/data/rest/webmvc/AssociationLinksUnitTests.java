/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.hateoas.Link;

/**
 * Unit tests for {@link Associations}.
 *
 * @author Oliver Gierke
 * @author Haroun Pacquee
 */
@RunWith(MockitoJUnitRunner.class)
public class AssociationLinksUnitTests {

	Associations links;

	ResourceMappings mappings;
	KeyValueMappingContext<?, ?> mappingContext;
	PersistentEntity<?, ?> entity;
	ResourceMetadata sampleResourceMetadata;

	@Mock RepositoryRestConfiguration config;
	@Mock ProjectionDefinitionConfiguration projectionDefinitionConfig;

	@Before
	public void setUp() {

		doReturn(projectionDefinitionConfig).when(config).getProjectionConfiguration();

		this.mappingContext = new KeyValueMappingContext<>();
		this.entity = mappingContext.getRequiredPersistentEntity(Sample.class);
		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));
		this.links = new Associations(mappings, config);
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-262
	public void rejectsNullMappings() {
		new Associations(null, mock(RepositoryRestConfiguration.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConfiguration() {
		new Associations(mappings, null);
	}

	@Test // DATAREST-262
	public void rejectsNullPropertyForIsLinkable() {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			links.isLinkableAssociation((PersistentProperty<?>) null);
		});
	}

	@Test // DATAREST-262
	public void consideredHiddenPropertyUnlinkable() {
		assertThat(links.isLinkableAssociation(entity.getRequiredPersistentProperty("hiddenProperty"))).isFalse();
	}

	@Test // DATAREST-262
	public void createsLinkToAssociationProperty() {

		PersistentProperty<?> property = entity.getRequiredPersistentProperty("property");
		List<Link> associationLinks = links.getLinksFor(property.getRequiredAssociation(), new Path("/base"));

		assertThat(associationLinks).hasSize(1);
		assertThat(associationLinks).contains(Link.of("/base/property", "property"));
	}

	@Test // DATAREST-262
	public void doesNotCreateLinksForHiddenProperty() {

		PersistentProperty<?> property = entity.getRequiredPersistentProperty("hiddenProperty");
		assertThat(links.getLinksFor(property.getRequiredAssociation(), new Path("/sample"))).hasSize(0);
	}

	@Test
	public void detectsLookupTypes() {

		doReturn(true).when(config).isLookupType(Property.class);

		assertThat(links.isLookupType(entity.getRequiredPersistentProperty("hiddenProperty"))).isTrue();
	}

	@Test
	public void delegatesResourceMetadataLookupToMappings() {
		assertThat(links.getMetadataFor(Property.class)).isEqualTo(mappings.getMetadataFor(Property.class));
	}

	public static class Sample {

		@Reference Property property;
		@RestResource(exported = false) @Reference Property hiddenProperty;
	}

	@RestResource
	public static class Property {

	}
}
