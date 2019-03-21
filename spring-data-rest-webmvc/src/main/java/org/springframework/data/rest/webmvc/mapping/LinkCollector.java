/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.mapping;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.util.Assert;

/**
 * A service to collect all standard links that need to be added to a certain object.
 *
 * @author Oliver Gierke
 */
public class LinkCollector {

	private final PersistentEntities entities;
	private final Associations associationLinks;
	private final SelfLinkProvider links;

	/**
	 * Creates a new {@link PersistentEntities}, {@link SelfLinkProvider} and {@link Associations}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param linkProvider must not be {@literal null}.
	 * @param associationLinks must not be {@literal null}.
	 */
	public LinkCollector(PersistentEntities entities, SelfLinkProvider linkProvider, Associations associationLinks) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(linkProvider, "SelfLinkProvider must not be null!");
		Assert.notNull(associationLinks, "AssociationLinks must not be null!");

		this.links = linkProvider;
		this.entities = entities;
		this.associationLinks = associationLinks;
	}

	/**
	 * Returns all {@link Links} for the given object.
	 *
	 * @param object must not be {@literal null}.
	 * @return
	 */
	public Links getLinksFor(Object object) {
		return getLinksFor(object, Collections.<Link> emptyList());
	}

	/**
	 * Returns all {@link Links} for the given object and already existing {@link Link}.
	 *
	 * @param object must not be {@literal null}.
	 * @param existingLinks must not be {@literal null}.
	 * @return
	 */
	public Links getLinksFor(Object object, List<Link> existingLinks) {

		Assert.notNull(object, "Object must not be null!");
		Assert.notNull(existingLinks, "Existing links must not be null!");

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(object.getClass());

		Links links = new Links(existingLinks);
		Link selfLink = createSelfLink(object, links);

		if (selfLink == null) {
			return links;
		}

		Path path = new Path(selfLink.expand().getHref());

		LinkCollectingAssociationHandler handler = new LinkCollectingAssociationHandler(entities, path, associationLinks);
		entity.doWithAssociations(handler);

		List<Link> result = new ArrayList<Link>(existingLinks);
		result.addAll(handler.getLinks());

		return addSelfLinkIfNecessary(object, result);
	}

	public Links getLinksForNested(Object object, List<Link> existing) {

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(object.getClass());

		NestedLinkCollectingAssociationHandler handler = new NestedLinkCollectingAssociationHandler(links,
				entity.getPropertyAccessor(object), associationLinks);
		entity.doWithAssociations(handler);

		List<Link> links = new ArrayList<Link>();
		links.addAll(existing);
		links.addAll(handler.getLinks());

		return new Links(links);
	}

	private Links addSelfLinkIfNecessary(Object object, List<Link> existing) {

		Links result = new Links(existing);

		if (result.hasLink(Link.REL_SELF)) {
			return result;
		}

		List<Link> list = new ArrayList<Link>();
		list.add(createSelfLink(object, result));
		list.addAll(existing);

		return new Links(list);
	}

	private Link createSelfLink(Object object, Links existing) {

		if (existing.hasLink(Link.REL_SELF)) {
			return existing.getLink(Link.REL_SELF);
		}

		return links.createSelfLinkFor(object).withSelfRel();
	}

	/**
	 * {@link SimpleAssociationHandler} that will collect {@link Link}s for all linkable associations.
	 *
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	@RequiredArgsConstructor
	private static class LinkCollectingAssociationHandler implements SimpleAssociationHandler {

		private static final String AMBIGUOUS_ASSOCIATIONS = "Detected multiple association links with same relation type! Disambiguate association %s using @RestResource!";

		private final @NonNull PersistentEntities entities;
		private final @NonNull Path basePath;
		private final @NonNull Associations associationLinks;
		private final @NonNull List<Link> links = new ArrayList<Link>();

		/**
		 * Returns the links collected after the {@link Association} has been traversed.
		 *
		 * @return the links
		 */
		public List<Link> getLinks() {
			return links;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
		 */
		@Override
		public void doWithAssociation(final Association<? extends PersistentProperty<?>> association) {

			if (associationLinks.isLinkableAssociation(association)) {

				PersistentProperty<?> property = association.getInverse();
				Links existingLinks = new Links(links);

				for (Link link : associationLinks.getLinksFor(association, basePath)) {
					if (existingLinks.hasLink(link.getRel())) {
						throw new MappingException(String.format(AMBIGUOUS_ASSOCIATIONS, property.toString()));
					} else {
						links.add(link);
					}
				}
			}
		}
	}

	@RequiredArgsConstructor
	private static class NestedLinkCollectingAssociationHandler implements SimpleAssociationHandler {

		private final SelfLinkProvider selfLinks;
		private final PersistentPropertyAccessor accessor;
		private final Associations associations;
		private final @Getter List<Link> links = new ArrayList<Link>();

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
		 */
		@Override
		public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

			if (!associations.isLinkableAssociation(association)) {
				return;
			}

			PersistentProperty<?> property = association.getInverse();
			Object value = accessor.getProperty(property);

			if (value == null) {
				return;
			}

			ResourceMetadata metadata = associations.getMappings().getMetadataFor(property.getOwner().getType());
			ResourceMapping propertyMapping = metadata.getMappingFor(property);

			for (Object element : asCollection(value)) {
				if (element != null)
					links.add(getLinkFor(element, propertyMapping));
			}
		}

		/**
		 * Returns a link pointing to the given entity using the given {@link ResourceMapping} to detect the link relation.
		 *
		 * @param entity must not be {@literal null}.
		 * @param mapping must not be {@literal null}.
		 * @return
		 */
		private Link getLinkFor(Object entity, ResourceMapping mapping) {
			return selfLinks.createSelfLinkFor(entity).withRel(mapping.getRel());
		}

		/**
		 * Returns the given object as {@link Collection}, i.e. the object as is if it's a collection already or wrapped
		 * into a single-element collection otherwise.
		 *
		 * @param object can be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private static Collection<Object> asCollection(Object object) {

			if (object instanceof Collection) {
				return (Collection<Object>) object;
			}

			return Collections.singleton(object);
		}
	}
}
