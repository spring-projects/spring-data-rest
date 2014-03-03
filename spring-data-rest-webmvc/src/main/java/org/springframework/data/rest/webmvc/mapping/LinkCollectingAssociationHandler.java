/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.mapping;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.rest.core.Path;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.util.Assert;

/**
 * {@link SimpleAssociationHandler} that will collect {@link Link}s for all linkable associations.
 * 
 * @author Oliver Gierke
 * @since 2.1
 */
public class LinkCollectingAssociationHandler implements SimpleAssociationHandler {

	private static final String AMBIGUOUS_ASSOCIATIONS = "Detected multiple association links with same relation type! Disambiguate association %s using @RestResource!";

	private final PersistentEntities entities;
	private final AssociationLinks associationLinks;
	private final Path basePath;

	private final List<Link> links;

	/**
	 * Creates a new {@link LinkCollectingAssociationHandler} for the given {@link PersistentEntities}, {@link Path} and
	 * {@link AssociationLinks}.
	 * 
	 * @param entities must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @param associationLinks must not be {@literal null}.
	 */
	public LinkCollectingAssociationHandler(PersistentEntities entities, Path path, AssociationLinks associationLinks) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(associationLinks, "AssociationLinks must not be null!");

		this.entities = entities;
		this.associationLinks = associationLinks;
		this.basePath = path;

		this.links = new ArrayList<Link>();
	}

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

		PersistentProperty<?> property = association.getInverse();

		if (associationLinks.isLinkableAssociation(property)) {

			Links existingLinks = new Links(links);

			for (Link link : associationLinks.getLinksFor(association, basePath)) {
				if (existingLinks.hasLink(link.getRel())) {
					throw new MappingException(String.format(AMBIGUOUS_ASSOCIATIONS, property.toString()));
				} else {
					links.add(link);
				}
			}

		} else {
			PersistentEntity<?, ?> associationEntity = entities.getPersistentEntity(property.getActualType());
			associationEntity.doWithAssociations(this);
		}
	}
}
