/*
 * Copyright 2016-2020 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
@RunWith(MockitoJUnitRunner.class)
public class AssociationsUnitTests {

	@Mock RepositoryRestConfiguration configuration;
	@Mock ProjectionDefinitionConfiguration projectionDefinitionConfiguration;

	@Mock PersistentEntity<?, ?> entity;
	@Mock PersistentProperty<?> property;

	Associations associations;

	KeyValueMappingContext<?, ?> mappingContext;
	ResourceMappings mappings;

	@Before
	public void setUp() {
		doReturn(projectionDefinitionConfiguration).when(configuration).getProjectionConfiguration();

		this.mappingContext = new KeyValueMappingContext<>();
		this.mappingContext.getPersistentEntity(Root.class);

		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));

		this.associations = new Associations(mappings, configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappings() {
		new Associations(null, configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConfiguration() {
		new Associations(mappings, null);
	}

	@Test
	public void handlesNullPropertyForLookupTypeCheck() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> associations.isLookupType(null));
	}

	@Test
	public void forwardsLookupTypeCheckToConfiguration() {

		doReturn(Root.class).when(property).getActualType();
		assertThat(associations.isLookupType(property)).isFalse();

		doReturn(true).when(configuration).isLookupType(Root.class);
		assertThat(associations.isLookupType(property)).isTrue();
	}

	@Test
	public void forwardsIdExposureCheckToConfiguration() {

		doReturn(Root.class).when(entity).getType();
		assertThat(associations.isIdExposed(entity)).isFalse();

		doReturn(true).when(configuration).isIdExposedFor(Root.class);
		assertThat(associations.isIdExposed(entity)).isTrue();
	}

	@Test
	public void exposesConfiguredMapping() {
		assertThat(associations.getMappings()).isEqualTo(mappings);
	}

	@Test
	public void forwardsMetadataLookupToMappings() {
		assertThat(associations.getMetadataFor(Root.class)).isNotNull();
	}

	@Test
	public void detectsAssociationLinks() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedAndExported"), new Path(""));

		assertThat(links).hasSize(1);
		assertThat(links).contains(Link.of("/relatedAndExported", "relatedAndExported"));
	}

	@Test
	public void doesNotCreateAssociationLinkIfTargetIsNotExported() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedButNotExported"), new Path(""));

		assertThat(links).hasSize(0);
	}

	@Test // DATAREST-1105
	public void detectsProjectionsForAssociationLinks() {

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
