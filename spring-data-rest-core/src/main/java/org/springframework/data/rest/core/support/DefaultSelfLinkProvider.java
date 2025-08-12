/*
 * Copyright 2015-2025 the original author or authors.
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

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;

/**
 * Default implementation of SelfLinkProvider that uses an {@link EntityLinks} instance to create self links. Considers
 * the configured {@link EntityLookup}s to use the returned resource identifier to eventually create the link.
 *
 * @author Oliver Gierke
 * @since 2.5
 * @soundtrack Trio Rotation - Travis
 */
public class DefaultSelfLinkProvider implements SelfLinkProvider {

	private final PersistentEntities entities;
	private final EntityLinks entityLinks;
	private final PluginRegistry<EntityLookup<?>, Class<?>> lookups;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link DefaultSelfLinkProvider} from the {@link PersistentEntities}, {@link EntityLinks} and
	 * {@link EntityLookup}s.
	 *
	 * @param entities must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param lookups must not be {@literal null}.
	 */
	public DefaultSelfLinkProvider(PersistentEntities entities, EntityLinks entityLinks,
			List<? extends EntityLookup<?>> lookups, ConversionService conversionService) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(entityLinks, "EntityLinks must not be null");
		Assert.notNull(lookups, "EntityLookups must not be null");

		this.entities = entities;
		this.entityLinks = entityLinks;
		this.lookups = PluginRegistry.of(lookups);
		this.conversionService = conversionService;
	}

	public Link createSelfLinkFor(Object instance) {

		Assert.notNull(instance, "Domain object must not be null");

		return createSelfLinkFor(instance.getClass(), instance);
	}

	public Link createSelfLinkFor(Class<?> type, Object reference) {

		if (type.isInstance(reference)) {
			return entityLinks.linkToItemResource(type, getResourceId(type, reference));
		}

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(type);
		PersistentProperty<?> idProperty = entity.getRequiredIdProperty();

		Object identifier = conversionService.convert(reference, idProperty.getType());

		if (lookups.hasPluginFor(type)) {
			identifier = getResourceId(type, conversionService.convert(identifier, type));
		}

		return entityLinks.linkToItemResource(type, identifier);
	}

	/**
	 * Returns the identifier to be used to create the self link URI.
	 *
	 * @param reference must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private Object getResourceId(Class<?> type, @Nullable Object reference) {

		Assert.notNull(reference, "Reference must not be null");

		if (!lookups.hasPluginFor(type)) {
			return entityIdentifierOrNull(reference);
		}

		return lookups.getPluginFor(type)//
				.map(it -> it.getClass().cast(it))//
				.map(it -> it.getResourceIdentifier(reference))//
				.orElseGet(() -> entityIdentifierOrNull(reference));
	}

	private @Nullable Object entityIdentifierOrNull(Object instance) {

		return entities.getRequiredPersistentEntity(instance.getClass()) //
				.getIdentifierAccessor(instance) //
				.getIdentifier();
	}
}
