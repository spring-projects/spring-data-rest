/*
 * Copyright 2014-2018 original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;

/**
 * Unit tests for {@link PersistentEntityResource}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityResourceUnitTests {

	@Mock Object payload;
	@Mock PersistentEntity<?, ?> entity;

	CollectionModel<EmbeddedWrapper> resources;
	Link link = Link.of("http://localhost", "foo");

	@Before
	public void setUp() {

		EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
		EmbeddedWrapper wrapper = wrappers.wrap("Embedded", LinkRelation.of("foo"));
		this.resources = CollectionModel.of(Collections.singleton(wrapper));
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-317
	public void rejectsNullPayload() {
		PersistentEntityResource.build(null, entity);
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-317
	public void rejectsNullPersistentEntity() {
		PersistentEntityResource.build(payload, null);
	}

	@Test // DATAREST-317
	public void defaultsEmbeddedsToEmptyResources() {

		PersistentEntityResource resource = PersistentEntityResource.build(payload, entity).build();

		assertThat(resource.getEmbeddeds()).isNotNull();
		assertThat(resource.getEmbeddeds()).isEmpty();
	}
}
