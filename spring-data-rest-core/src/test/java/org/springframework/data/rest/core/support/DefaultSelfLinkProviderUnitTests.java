/*
 * Copyright 2015-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.domain.Profile;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;

/**
 * Unit tests for {@link DefaultSelfLinkProvider}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Trio Rotation - Triopane
 */
@ExtendWith(MockitoExtension.class)
class DefaultSelfLinkProviderUnitTests {

	SelfLinkProvider provider;

	@Mock(lenient = true) EntityLinks entityLinks;
	PersistentEntities entities;
	List<EntityLookup<?>> lookups;
	ConversionService conversionService;

	@BeforeEach
	void setUp() {

		when(entityLinks.linkToItemResource(any(Class.class), any(Object.class))).then(invocation -> {

			Class<?> type = invocation.getArgument(0);
			Object id = invocation.getArgument(1);

			return Link.of("/".concat(type.getName()).concat("/").concat(id.toString()));
		});

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(Profile.class);
		context.afterPropertiesSet();

		this.entities = new PersistentEntities(Arrays.asList(context));
		this.lookups = Collections.emptyList();
		this.conversionService = new DefaultConversionService();
		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, lookups, conversionService);
	}

	@Test // DATAREST-724
	void rejectsNullEntities() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new DefaultSelfLinkProvider(null, entityLinks, lookups, conversionService));
	}

	@Test // DATAREST-724
	void rejectsNullEntityLinks() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new DefaultSelfLinkProvider(entities, null, lookups, conversionService));
	}

	@Test // DATAREST-724
	void rejectsNullEntityLookups() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new DefaultSelfLinkProvider(entities, entityLinks, null, conversionService));
	}

	@Test // DATAREST-724
	void usesEntityIdIfNoLookupDefined() {

		Profile profile = new Profile("Name", "Type");
		Link link = provider.createSelfLinkFor(profile);

		assertThat(link.getHref()).endsWith(profile.getId().toString());
	}

	@Test // DATAREST-724
	@SuppressWarnings("unchecked")
	void usesEntityLookupIfDefined() {

		EntityLookup<Object> lookup = mock(EntityLookup.class);
		when(lookup.supports(Profile.class)).thenReturn(true);
		when(lookup.getResourceIdentifier(any(Profile.class))).thenReturn("foo");

		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, Collections.singletonList(lookup),
				conversionService);

		Link link = provider.createSelfLinkFor(new Profile("Name", "Type"));

		assertThat(link.getHref()).endsWith("foo");
	}

	@Test // DATAREST-724, DATAREST-1549
	void rejectsLinkCreationForUnknownEntity() {

		assertThatExceptionOfType(MappingException.class) //
				.isThrownBy(() -> provider.createSelfLinkFor(new Object())) //
				.withMessageContaining(Object.class.getName()) //
				.withMessageContaining("Couldn't find PersistentEntity for");
	}
}
