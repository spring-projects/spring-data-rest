/*
 * Copyright 2013-2016 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResource.Builder;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.util.Assert;

/**
 * {@link ResourceAssembler} to create {@link PersistentEntityResource}s for arbitrary domain objects.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class PersistentEntityResourceAssembler implements ResourceAssembler<Object, PersistentEntityResource> {

	private final @NonNull PersistentEntities entities;
	private final @NonNull Projector projector;
	private final @NonNull Associations associations;
	private final @NonNull SelfLinkProvider linkProvider;
	private final @NonNull EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.ResourceAssembler#toResource(java.lang.Object)
	 */
	@Override
	public PersistentEntityResource toResource(Object instance) {

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
				withLink(getSelfLinkFor(source)).//
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
		return new EmbeddedResourcesAssembler(entities, associations, projector).getEmbeddedResources(instance);
	}

	/**
	 * Creates the self link for the given domain instance.
	 * 
	 * @param instance must be a managed entity, not {@literal null}.
	 * @return
	 */
	public Link getSelfLinkFor(Object instance) {

		Link link = linkProvider.createSelfLinkFor(instance);
		return new Link(link.expand().getHref(), Link.REL_SELF);
	}
}
