/*
 * Copyright 2015-present the original author or authors.
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

import java.io.Serializable;
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
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
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

	@Test // DATAREST-846
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void fallsBackToIdPropertyAccessorWhenIdentifierAccessorReturnsNullForCompositeKey() {

		// Simulate an entity whose IdentifierAccessor returns null (as happens with
		// @EmbeddedId / @IdClass in Spring Data JPA when the mapping context cannot
		// resolve the composite key via its standard path), but whose ID property
		// is still readable via PersistentPropertyAccessor.
		CompositeKey compositeKey = new CompositeKey(1L, "part2");

		IdentifierAccessor identifierAccessor = mock(IdentifierAccessor.class);
		when(identifierAccessor.getIdentifier()).thenReturn(null);

		PersistentProperty idProperty = mock(PersistentProperty.class);

		PersistentPropertyAccessor propertyAccessor = mock(PersistentPropertyAccessor.class);
		when(propertyAccessor.getProperty(idProperty)).thenReturn(compositeKey);

		PersistentEntity persistentEntity = mock(PersistentEntity.class);
		when(persistentEntity.getIdentifierAccessor(any())).thenReturn(identifierAccessor);
		when(persistentEntity.getIdProperty()).thenReturn(idProperty);
		when(persistentEntity.getPropertyAccessor(any())).thenReturn(propertyAccessor);

		PersistentEntities mockEntities = mock(PersistentEntities.class);
		when(mockEntities.getRequiredPersistentEntity(CompositeKeyEntity.class)).thenReturn(persistentEntity);

		SelfLinkProvider providerUnderTest = new DefaultSelfLinkProvider(mockEntities, entityLinks, lookups,
				conversionService);

		CompositeKeyEntity entity = new CompositeKeyEntity(compositeKey);
		Link link = providerUnderTest.createSelfLinkFor(entity);

		assertThat(link.getHref()).endsWith(compositeKey.toString());
	}

	// ---------------------------------------------------------------------------
	// Helper types for the composite-key test
	// ---------------------------------------------------------------------------

	static class CompositeKey implements Serializable {

		final Long part1;
		final String part2;

		CompositeKey(Long part1, String part2) {
			this.part1 = part1;
			this.part2 = part2;
		}

		@Override
		public String toString() {
			return part1 + "_" + part2;
		}
	}

	static class CompositeKeyEntity {

		@Id CompositeKey id;

		CompositeKeyEntity(CompositeKey id) {
			this.id = id;
		}
	}
}
