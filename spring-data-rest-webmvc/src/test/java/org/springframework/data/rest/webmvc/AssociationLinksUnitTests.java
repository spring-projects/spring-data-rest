/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
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
 */
@RunWith(MockitoJUnitRunner.class)
public class AssociationLinksUnitTests {

	Associations links;

	ResourceMappings mappings;
	KeyValueMappingContext mappingContext;
	KeyValuePersistentEntity<?> entity;
	ResourceMetadata sampleResourceMetadata;

	@Mock RepositoryRestConfiguration config;

	@Before
	public void setUp() {

		this.mappingContext = new KeyValueMappingContext();
		this.entity = mappingContext.getPersistentEntity(Sample.class);
		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));
		this.links = new Associations(mappings, config);
	}

	/**
	 * @see DATAREST-262
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappings() {
		new Associations(null, mock(RepositoryRestConfiguration.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConfiguration() {
		new Associations(mappings, null);
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void considersNullPropertyUnlinkable() {
		assertThat(links.isLinkableAssociation((PersistentProperty<?>) null), is(false));
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void consideredHiddenPropertyUnlinkable() {
		assertThat(links.isLinkableAssociation(entity.getPersistentProperty("hiddenProperty")), is(false));
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void considersUnexportedPropertyUnlinkable() {

		KeyValuePersistentProperty property = entity.getPersistentProperty("unexportedProperty");
		assertThat(links.isLinkableAssociation(property), is(false));
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void createsLinkToAssociationProperty() {

		PersistentProperty<?> property = entity.getPersistentProperty("property");
		List<Link> associationLinks = links.getLinksFor(property.getAssociation(), new Path("/base"));

		assertThat(associationLinks, hasSize(1));
		assertThat(associationLinks, hasItem(new Link("/base/property", "property")));
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void doesNotCreateLinksForHiddenProperty() {

		PersistentProperty<?> property = entity.getPersistentProperty("hiddenProperty");
		assertThat(links.getLinksFor(property.getAssociation(), new Path("/sample")), hasSize(0));
	}

	@Test
	public void detectsLookupTypes() {

		doReturn(true).when(config).isLookupType(Property.class);

		assertThat(links.isLookupType(entity.getPersistentProperty("hiddenProperty")), is(true));
	}

	@Test
	public void delegatesResourceMetadataLookupToMappings() {
		assertThat(links.getMetadataFor(Property.class), is(mappings.getMetadataFor(Property.class)));
	}

	public static class Sample {

		@Reference Property property;
		@RestResource(exported = false) @Reference Property hiddenProperty;
	}

	@RestResource
	public static class Property {

	}
}
