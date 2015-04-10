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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.hateoas.Link;

/**
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class AssociationLinksUnitTests {

	AssociationLinks links;

	ResourceMappings mappings;

	MongoMappingContext mappingContext;
	MongoPersistentEntity<?> entity;
	ResourceMetadata sampleResourceMetadata;

	@Before
	public void setUp() {

		this.mappingContext = new MongoMappingContext();
		this.entity = mappingContext.getPersistentEntity(Sample.class);
		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));
		this.links = new AssociationLinks(mappings);
	}

	/**
	 * @see DATAREST-262
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappings() {
		new AssociationLinks(null);
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void considersNullPropertyUnlinkable() {
		assertThat(links.isLinkableAssociation(null), is(false));
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

		MongoPersistentProperty property = entity.getPersistentProperty("unexportedProperty");
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

	public static class Sample {

		@Reference Property property;
		@RestResource(exported = false) @Reference Property hiddenProperty;
	}

	public static class Property {

	}
}
