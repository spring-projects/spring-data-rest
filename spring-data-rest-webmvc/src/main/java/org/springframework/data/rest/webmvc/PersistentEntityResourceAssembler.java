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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource.Builder;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.EntityLinks;
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
public class PersistentEntityResourceAssembler implements ResourceAssembler<Object, PersistentEntityResource> {

	private final Repositories repositories;
	private final EntityLinks entityLinks;
	private final Projector projector;
	private final ResourceMappings mappings;
	private final EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	/**
	 * Creates a new {@link PersistentEntityResourceAssembler}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param projector must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	public PersistentEntityResourceAssembler(Repositories repositories, EntityLinks entityLinks, Projector projector,
			ResourceMappings mappings) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(projector, "PersistentEntityProjector must not be be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.repositories = repositories;
		this.entityLinks = entityLinks;
		this.projector = projector;
		this.mappings = mappings;
	}

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
		return wrap(projector.project(instance), instance).//
				renderAllAssociationLinks().build();
	}

	private Builder wrap(Object instance, Object source) {

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(source.getClass());

		return PersistentEntityResource.build(instance, entity).//
				withEmbedded(getEmbeddedResources(source)).//
				withLink(getSelfLinkFor(source));
	}

	/**
	 * Returns the embedded resources to render. This will add an {@link RelatedResource} for linkable associations if
	 * they have an excerpt projection registered.
	 * 
	 * @param instance must not be {@literal null}.
	 * @return
	 */
	private Iterable<EmbeddedWrapper> getEmbeddedResources(Object instance) {

		Assert.notNull(instance, "Entity instance must not be null!");

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(instance.getClass());

		final List<EmbeddedWrapper> associationProjections = new ArrayList<EmbeddedWrapper>();
		final BeanWrapper<Object> wrapper = BeanWrapper.create(instance, null);
		final AssociationLinks associationLinks = new AssociationLinks(mappings);
		final ResourceMetadata metadata = mappings.getMappingFor(instance.getClass());

		entity.doWithAssociations(new SimpleAssociationHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
			 */
			@Override
			public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

				PersistentProperty<?> property = association.getInverse();

				if (!associationLinks.isLinkableAssociation(property)) {
					return;
				}

				if (!projector.hasExcerptProjection(property.getActualType())) {
					return;
				}

				Object value = wrapper.getProperty(association.getInverse());

				if (value == null) {
					return;
				}

				String rel = metadata.getMappingFor(property).getRel();

				if (value instanceof Collection) {

					Collection<?> collection = (Collection<?>) value;

					if (collection.isEmpty()) {
						return;
					}

					List<Object> nestedCollection = new ArrayList<Object>();

					for (Object element : collection) {
						if (element != null) {
							nestedCollection.add(projector.projectExcerpt(element));
						}
					}

					associationProjections.add(wrappers.wrap(nestedCollection, rel));

				} else {
					associationProjections.add(wrappers.wrap(projector.projectExcerpt(value), rel));
				}
			}
		});

		return associationProjections;
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
