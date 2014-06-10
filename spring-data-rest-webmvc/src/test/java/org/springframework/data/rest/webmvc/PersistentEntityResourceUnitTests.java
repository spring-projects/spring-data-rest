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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;

/**
 * Unit tests for {@link PersistentEntityResource}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityResourceUnitTests {

	@Mock Object payload;
	@Mock PersistentEntity<?, ?> entity;

	Resources<EmbeddedWrapper> resources;
	Link link = new Link("http://localhost", "foo");

	@Before
	public void setUp() {

		EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
		EmbeddedWrapper wrapper = wrappers.wrap("Embedded", "foo");
		this.resources = new Resources<EmbeddedWrapper>(Collections.singleton(wrapper));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPayload() {
		PersistentEntityResource.build(null, entity);
	}

	/**
	 * @see DATAREST-317
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPersistentEntity() {
		PersistentEntityResource.build(payload, null);
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void defaultsEmbeddedsToEmptyResources() {

		PersistentEntityResource resource = PersistentEntityResource.build(payload, entity).build();

		assertThat(resource.getEmbeddeds(), is(notNullValue()));
		assertThat(resource.getEmbeddeds(), is(emptyIterable()));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void doesNotRenderAssociationLinksIfEmbeddedWithRelPresent() {

		PersistentEntityResource resource = PersistentEntityResource.build(payload, entity).//
				withEmbedded(resources).build();

		assertThat(resource.shouldRenderLink(link), is(false));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void rendersAssociationLinksIfEvenIfEmbeddedWithRelPresentButLinksEnforced() {

		PersistentEntityResource resource = PersistentEntityResource.build(payload, entity).//
				withEmbedded(resources).renderAllAssociationLinks().build();

		assertThat(resource.shouldRenderLink(link), is(true));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void rendersAssociationIfNoEmbeddedPresent() {

		PersistentEntityResource resource = PersistentEntityResource.build(payload, entity).build();
		assertThat(resource.shouldRenderLink(link), is(true));
	}
}
