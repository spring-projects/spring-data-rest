/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.util.Assert;

/**
 * A service to collect all standard links that need to be added to a certain object.
 *
 * @author Oliver Drotbohm
 * @since 3.6
 */
public class DefaultLinkCollector implements LinkCollector {

	private final PersistentEntities entities;
	private final Associations associationLinks;
	private final SelfLinkProvider links;

	/**
	 * Creates a new {@link DefaultLinkCollector} for the given {@link PersistentEntities}, {@link SelfLinkProvider} and
	 * {@link Associations}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param linkProvider must not be {@literal null}.
	 * @param associationLinks must not be {@literal null}.
	 */
	public DefaultLinkCollector(PersistentEntities entities, SelfLinkProvider linkProvider,
			Associations associationLinks) {

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
	@Override
	public Links getLinksFor(Object object) {
		return getLinksFor(object, Links.NONE);
	}

	/**
	 * Returns all {@link Links} for the given object and already existing {@link Link}.
	 *
	 * @param object must not be {@literal null}.
	 * @param existingLinks must not be {@literal null}.
	 * @return
	 */
	@Override
	public Links getLinksFor(Object object, Links existingLinks) {

		Assert.notNull(object, "Object must not be null!");
		Assert.notNull(existingLinks, "Existing links must not be null!");

		Link selfLink = createSelfLink(object, existingLinks);

		if (selfLink == null) {
			return existingLinks;
		}

		Path path = new Path(selfLink.expand().getHref());

		LinkCollectingAssociationHandler handler = new LinkCollectingAssociationHandler(path, associationLinks);
		entities.getRequiredPersistentEntity(object.getClass()).doWithAssociations(handler);

		return addSelfLinkIfNecessary(object, existingLinks.and(handler.getLinks()));
	}

	@Override
	public Links getLinksForNested(Object object, Links existing) {

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(object.getClass());

		NestedLinkCollectingAssociationHandler handler = new NestedLinkCollectingAssociationHandler(links,
				entity.getPropertyAccessor(object), associationLinks);
		entity.doWithAssociations(handler);

		return existing.and(handler.getLinks());
	}

	private Links addSelfLinkIfNecessary(Object object, Links existing) {
		return existing.andIf(!existing.hasLink(IanaLinkRelations.SELF),
				() -> links.createSelfLinkFor(object).withSelfRel());
	}

	private Link createSelfLink(Object object, Links existing) {

		return existing.getLink(IanaLinkRelations.SELF) //
				.orElseGet(() -> links.createSelfLinkFor(object).withSelfRel());
	}

	/**
	 * {@link SimpleAssociationHandler} that will collect {@link Link}s for all linkable associations.
	 *
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	private static class LinkCollectingAssociationHandler implements SimpleAssociationHandler {

		private static final String AMBIGUOUS_ASSOCIATIONS = "Detected multiple association links with same relation type! Disambiguate association %s using @RestResource!";

		private final Path basePath;
		private final Associations associationLinks;
		private final List<Link> links = new ArrayList<Link>();

		public LinkCollectingAssociationHandler(Path basePath, Associations associationLinks) {

			Assert.notNull(basePath, "Base Path must not be null!");
			Assert.notNull(associationLinks, "Associations must not be null!");

			this.basePath = basePath;
			this.associationLinks = associationLinks;
		}

		/**
		 * Returns the links collected after the {@link Association} has been traversed.
		 *
		 * @return the links
		 */
		public Links getLinks() {
			return Links.of(links);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
		 */
		@Override
		public void doWithAssociation(final Association<? extends PersistentProperty<?>> association) {

			if (associationLinks.isLinkableAssociation(association)) {

				PersistentProperty<?> property = association.getInverse();
				Links existingLinks = Links.of(links);

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

	private static class NestedLinkCollectingAssociationHandler implements SimpleAssociationHandler {

		private final SelfLinkProvider selfLinks;
		private final PersistentPropertyAccessor<?> accessor;
		private final Associations associations;
		private Links links = Links.NONE;

		public NestedLinkCollectingAssociationHandler(SelfLinkProvider selfLinks,
				PersistentPropertyAccessor<?> accessor, Associations associations) {

			Assert.notNull(selfLinks, "SelfLinkProvider must not be null!");
			Assert.notNull(accessor, "PersistentPropertyAccessor must not be null!");
			Assert.notNull(associations, "Associations must not be null!");

			this.selfLinks = selfLinks;
			this.accessor = accessor;
			this.associations = associations;
		}

		public List<Link> getLinks() {
			return this.links.toList();
		}

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

				links = links.andIf(element != null,
						() -> selfLinks.createSelfLinkFor(property.getAssociationTargetType(), element)
								.withRel(propertyMapping.getRel()));
			}
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

			return object instanceof Collection //
					? (Collection<Object>) object //
					: Collections.singleton(object);
		}
	}
}
