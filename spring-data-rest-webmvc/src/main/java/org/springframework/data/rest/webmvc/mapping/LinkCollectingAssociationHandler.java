/*
 * Copyright 2014-2021 the original author or authors.
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
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
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
	private final Associations associations;
	private final Path basePath;
	private final boolean nested;

	private final List<Link> links;

	/**
	 * Creates a new {@link LinkCollectingAssociationHandler} for the given {@link PersistentEntities}, {@link Path} and
	 * {@link Associations}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @param associations must not be {@literal null}.
	 */
	public LinkCollectingAssociationHandler(PersistentEntities entities, Path path, Associations associations) {
		this(entities, path, associations, false);
	}

	private LinkCollectingAssociationHandler(PersistentEntities entities, Path path, Associations associations,
			boolean nested) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(associations, "AssociationLinks must not be null!");

		this.entities = entities;
		this.associations = associations;
		this.basePath = path;

		this.links = new ArrayList<Link>();
		this.nested = nested;
	}

	public LinkCollectingAssociationHandler nested() {
		return nested ? this : new LinkCollectingAssociationHandler(entities, basePath, associations, true);
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

		PersistentProperty<?> property = association.getInverse();

		if (associations.isLinkableAssociation(property)) {

			Links existingLinks = Links.of(links);

			for (Link link : associations.getLinksFor(association, basePath)) {
				if (existingLinks.hasLink(link.getRel())) {
					throw new MappingException(String.format(AMBIGUOUS_ASSOCIATIONS, property.toString()));
				} else {
					links.add(link);
				}
			}
		}
	}
}
