/*
 * Copyright 2015-2017 the original author or authors.
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
package org.springframework.data.rest.core.support;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.domain.Profile;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;

/**
 * Unit tests for {@link DefaultSelfLinkProvider}.
 * 
 * @author Oliver Gierke
 * @soundtrack Trio Rotation - Triopane
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSelfLinkProviderUnitTests {

	SelfLinkProvider provider;

	@Mock EntityLinks entityLinks;
	PersistentEntities entities;
	List<EntityLookup<?>> lookups;

	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {

		when(entityLinks.linkToSingleResource((Class<?>) any(), any())).then(invocation -> {

			Class<?> type = invocation.getArgument(0);
			Serializable id = invocation.getArgument(1);

			return new Link("/".concat(type.getName()).concat("/").concat(id.toString()));
		});

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(Profile.class);
		context.afterPropertiesSet();

		this.entities = new PersistentEntities(Arrays.asList(context));
		this.lookups = Collections.emptyList();
		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, lookups);
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-724
	public void rejectsNullEntities() {
		new DefaultSelfLinkProvider(null, entityLinks, lookups);
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-724
	public void rejectsNullEntityLinks() {
		new DefaultSelfLinkProvider(entities, null, lookups);
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-724
	public void rejectsNullEntityLookups() {
		new DefaultSelfLinkProvider(entities, entityLinks, null);
	}

	@Test // DATAREST-724
	public void usesEntityIdIfNoLookupDefined() {

		Profile profile = new Profile("Name", "Type");
		Link link = provider.createSelfLinkFor(profile);

		assertThat(link.getHref(), Matchers.endsWith(profile.getId().toString()));
	}

	@Test // DATAREST-724
	@SuppressWarnings("unchecked")
	public void usesEntityLookupIfDefined() {

		EntityLookup<Object> lookup = mock(EntityLookup.class);
		when(lookup.supports(Profile.class)).thenReturn(true);
		when(lookup.getResourceIdentifier(any(Profile.class))).thenReturn("foo");

		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, Arrays.asList(lookup));

		Link link = provider.createSelfLinkFor(new Profile("Name", "Type"));

		assertThat(link.getHref(), Matchers.endsWith("foo"));
	}

	@Test // DATAREST-724
	public void rejectsLinkCreationForUnknownEntity() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(Object.class.getName());
		exception.expectMessage("Couldn't find PersistentEntity for");

		provider.createSelfLinkFor(new Object());
	}
}
