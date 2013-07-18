/*
 * Copyright 2013 the original author or authors.
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
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
public class PersistentEntityResourceAssembler<T> implements ResourceAssembler<T, PersistentEntityResource<T>> {

	private final Repositories repositories;
	private final EntityLinks entityLinks;

	/**
	 * Creates a new {@link PersistentEntityResourceAssembler}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 */
	public PersistentEntityResourceAssembler(Repositories repositories, EntityLinks entityLinks) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(entityLinks, "EntityLinks must not be null!");

		this.repositories = repositories;
		this.entityLinks = entityLinks;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.ResourceAssembler#toResource(java.lang.Object)
	 */
	@Override
	public PersistentEntityResource<T> toResource(T instance) {

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(instance.getClass());

		PersistentEntityResource<T> resource = PersistentEntityResource.wrap(entity, instance);
		resource.add(getSelfLinkFor(instance));
		return resource;
	}

	public Link getSelfLinkFor(Object instance) {

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(instance.getClass());

		BeanWrapper<?, Object> wrapper = BeanWrapper.create(instance, null);
		Object id = wrapper.getProperty(entity.getIdProperty());

		return entityLinks.linkForSingleResource(entity.getType(), id).withSelfRel();
	}
}
