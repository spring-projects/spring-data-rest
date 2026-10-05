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

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;

/**
 * Default implementation of SelfLinkProvider that uses an {@link EntityLinks} instance to create self links. Considers
 * the configured {@link EntityLookup}s to use the returned resource identifier to eventually create the link.
 * <p>
 * When the concrete type of an instance is a subclass of an abstract base class that is the repository domain type
 * (e.g. JPA single-table inheritance), the provider walks up the type hierarchy to find the nearest supertype that has
 * a registered repository and uses that type for link generation. This ensures that subclass instances receive correct
 * self-links pointing to the base-class resource path rather than a non-existent subclass path.
 *
 * @author Oliver Gierke
 * @author Steve Rutherford
 * @since 2.5
 * @soundtrack Trio Rotation - Travis
 */
public class DefaultSelfLinkProvider implements SelfLinkProvider {

	private final PersistentEntities entities;
	private final EntityLinks entityLinks;
	private final PluginRegistry<EntityLookup<?>, Class<?>> lookups;
	private final ConversionService conversionService;
	private final @Nullable Repositories repositories;

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
		this(entities, entityLinks, lookups, conversionService, null);
	}

	/**
	 * Creates a new {@link DefaultSelfLinkProvider} from the {@link PersistentEntities}, {@link EntityLinks},
	 * {@link EntityLookup}s and {@link Repositories}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param lookups must not be {@literal null}.
	 * @param repositories can be {@literal null}.
	 */
	public DefaultSelfLinkProvider(PersistentEntities entities, EntityLinks entityLinks,
			List<? extends EntityLookup<?>> lookups, ConversionService conversionService,
			@Nullable Repositories repositories) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(entityLinks, "EntityLinks must not be null");
		Assert.notNull(lookups, "EntityLookups must not be null");

		this.entities = entities;
		this.entityLinks = entityLinks;
		this.lookups = PluginRegistry.of(lookups);
		this.conversionService = conversionService;
		this.repositories = repositories;
	}

	public Link createSelfLinkFor(Object instance) {

		Assert.notNull(instance, "Domain object must not be null");

		return createSelfLinkFor(instance.getClass(), instance);
	}

	public Link createSelfLinkFor(Class<?> type, Object reference) {

		if (type.isInstance(reference)) {

			// Resolve the effective link type: if the concrete type has no registered repository
			// (e.g. it is a JPA subclass of an abstract base that owns the repository), walk up
			// the superclass hierarchy until we find a type that has a repository.
			Class<?> linkType = resolveRepositoryType(type);

			var identifier = getResourceId(linkType, reference);

			if (identifier == null) {
				throw new IllegalArgumentException("Cannot resolve identifier from reference %s".formatted(reference));
			}

			return entityLinks.linkToItemResource(linkType, identifier);
		}

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(type);
		PersistentProperty<?> idProperty = entity.getRequiredIdProperty();

		Object identifier = conversionService.convert(reference, idProperty.getType());

		if (lookups.hasPluginFor(type)) {
			identifier = getResourceId(type, conversionService.convert(identifier, type));
		}

		if (identifier == null) {
			throw new IllegalArgumentException("Cannot resolve identifier from reference %s".formatted(reference));
		}

		return entityLinks.linkToItemResource(type, identifier);
	}

	/**
	 * Resolves the effective repository-backed type for link generation. If the given type directly has a registered
	 * repository, it is returned as-is. Otherwise the superclass hierarchy is walked until a type with a repository is
	 * found. This handles the case where a concrete JPA subclass is returned from a repository whose domain type is an
	 * abstract base class (e.g. JPA single-table or joined-table inheritance).
	 * <p>
	 * When {@link Repositories} is available it is used for the check (precise: only types with a direct repository
	 * match); otherwise {@link EntityLinks#supports(Class)} is used as a fallback.
	 *
	 * @param type the concrete domain object type, must not be {@literal null}.
	 * @return the nearest type in the hierarchy that has a registered repository, or the original type if none is found.
	 */
	private Class<?> resolveRepositoryType(Class<?> type) {

		Class<?> candidate = type;

		while (candidate != null && candidate != Object.class) {
			if (hasDirectRepository(candidate)) {
				return candidate;
			}
			candidate = candidate.getSuperclass();
		}

		return type;
	}

	/**
	 * Returns whether the given type is the exact domain type of a registered repository (not merely a subclass of
	 * one). When {@link Repositories} is available, this is determined by checking that the type is registered in the
	 * repository factory infos AND that the repository's declared domain type equals the given type exactly. This
	 * distinguishes e.g. {@code Meal} (the repository domain type) from {@code Dinner} (a JPA subclass that
	 * {@link Repositories#hasRepositoryFor} also returns {@code true} for via hierarchy lookup).
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if the type is the exact domain type of a registered repository.
	 */
	private boolean hasDirectRepository(Class<?> type) {

		if (repositories == null) {
			return entityLinks.supports(type);
		}

		if (!repositories.hasRepositoryFor(type)) {
			return false;
		}

		// hasRepositoryFor walks up the hierarchy, so we must verify the declared domain type matches exactly.
		return repositories.getRepositoryInformationFor(type)
				.map(info -> info.getDomainType().equals(type))
				.orElse(false);
	}

	/**
	 * Returns the identifier to be used to create the self link URI.
	 *
	 * @param reference
	 * @return can be {@literal null}.
	 */
	@Nullable
	private Object getResourceId(Class<?> type, @Nullable Object reference) {

		if (reference == null) {
			return null;
		}

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
