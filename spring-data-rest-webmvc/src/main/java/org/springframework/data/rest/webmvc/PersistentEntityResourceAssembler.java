/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.util.Assert;

/**
 * {@link ResourceAssembler} to create {@link PersistentEntityResource}s for arbitrary domain objects.
 * 
 * @author Oliver Gierke
 */
public class PersistentEntityResourceAssembler implements ResourceAssembler<Object, PersistentEntityResource<Object>> {

	private final Repositories repositories;
	private final EntityLinks entityLinks;
	private final Projector projector;

	/**
	 * Creates a new {@link PersistentEntityResourceAssembler}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param projections must not be {@literal null}.
	 */
	public PersistentEntityResourceAssembler(Repositories repositories, EntityLinks entityLinks, Projector projector) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(projector, "PersistentEntityProjector must not be be null!");

		this.repositories = repositories;
		this.entityLinks = entityLinks;
		this.projector = projector;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.ResourceAssembler#toResource(java.lang.Object)
	 */
	@Override
	public PersistentEntityResource<Object> toResource(Object instance) {

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(instance.getClass());
		return PersistentEntityResource.wrap(entity, projector.project(instance), getSelfLinkFor(instance));
	}

	/**
	 * Creates the self link for the given domain instance.
	 * 
	 * @param instance must be a managed entity, not {@literal null}.
	 * @return
	 */
	public Link getSelfLinkFor(Object instance) {

		Assert.notNull(instance, "Domain object must not be null!");

		Class<? extends Object> instanceType = instance.getClass();
		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(instanceType);

		if (entity == null) {
			throw new IllegalArgumentException(String.format("Cannot create self link for %s! No persistent entity found!",
					instanceType));
		}

		BeanWrapper<Object> wrapper = BeanWrapper.create(instance, null);
		Object id = wrapper.getProperty(entity.getIdProperty());

		Link resourceLink = entityLinks.linkToSingleResource(entity.getType(), id);
		return new Link(resourceLink.getHref(), Link.REL_SELF);
	}
}
