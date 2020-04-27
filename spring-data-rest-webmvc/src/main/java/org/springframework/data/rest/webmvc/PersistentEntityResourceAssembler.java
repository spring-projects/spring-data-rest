/*
 * Copyright 2013-2020 the original author or authors.
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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResource.Builder;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.util.Assert;

/**
 * {@link ResourceAssembler} to create {@link PersistentEntityResource}s for arbitrary domain objects.
 *
 * @author Oliver Gierke
 */
public class PersistentEntityResourceAssembler
		implements RepresentationModelAssembler<Object, PersistentEntityResource> {

	private final PersistentEntities entities;
	private final Projector projector;
	private final SelfLinkProvider linkProvider;
	private final EmbeddedResourcesAssembler embeddedAssembler;

	/**
	 * Creates a new {@link PersistentEntityResourceAssembler} for the given {@link PersistentEntities},
	 * {@link Projector}, {@link Associations} and {@link SelfLinkProvider}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param projector must not be {@literal null}.
	 * @param associations must not be {@literal null}.
	 * @param linkProvider must not be {@literal null}.
	 */
	public PersistentEntityResourceAssembler(PersistentEntities entities, Projector projector, Associations associations,
			SelfLinkProvider linkProvider) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(projector, "Projector must not be null!");
		Assert.notNull(associations, "Associations must not be null!");
		Assert.notNull(linkProvider, "SelfLinkProvider must not be null!");

		this.entities = entities;
		this.projector = projector;
		this.linkProvider = linkProvider;
		this.embeddedAssembler = new EmbeddedResourcesAssembler(entities, associations, projector);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.RepresentationModelAssembler#toModel(java.lang.Object)
	 */
	@Override
	public PersistentEntityResource toModel(Object instance) {

		Assert.notNull(instance, "Entity instance must not be null!");
		return wrap(projector.projectExcerpt(instance), instance).build();
	}

	/**
	 * Returns the full object as {@link PersistentEntityResource} using the underlying {@link Projector}.
	 *
	 * @param instance must not be {@literal null}.
	 * @return
	 */
	public PersistentEntityResource toFullResource(Object instance) {

		Assert.notNull(instance, "Entity instance must not be null!");
		return wrap(projector.project(instance), instance).build();
	}

	private Builder wrap(Object instance, Object source) {

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(source.getClass());

		return PersistentEntityResource.build(instance, entity).//
				withEmbedded(getEmbeddedResources(source)).//
				withLink(getExpandedSelfLink(source)).//
				withLink(linkProvider.createSelfLinkFor(source));
	}

	/**
	 * Returns the embedded resources to render. This will add an {@link RelatedResource} for linkable associations if
	 * they have an excerpt projection registered.
	 *
	 * @param instance must not be {@literal null}.
	 * @return
	 */
	private Iterable<EmbeddedWrapper> getEmbeddedResources(Object instance) {
		return embeddedAssembler.getEmbeddedResources(instance);
	}

	/**
	 * Creates the self link for the given domain instance, with no templated parameters.
	 *
	 * @param instance must be a managed entity, not {@literal null}.
	 * @return
	 */
	Link getExpandedSelfLink(Object instance) {
		return linkProvider.createSelfLinkFor(instance).withSelfRel().expand();
	}
}
