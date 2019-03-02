/*
 * Copyright 2016-2019 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class EmbeddedResourcesAssembler {

	private final @NonNull PersistentEntities entities;
	private final @NonNull Associations associations;
	private final @NonNull ExcerptProjector projector;
	private final @NonNull EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	/**
	 * Returns the embedded resources to render. This will add an {@link RelatedResource} for linkable associations if
	 * they have an excerpt projection registered.
	 *
	 * @param instance must not be {@literal null}.
	 * @return
	 */
	public Iterable<EmbeddedWrapper> getEmbeddedResources(Object instance) {

		Assert.notNull(instance, "Entity instance must not be null!");

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(instance.getClass());

		final List<EmbeddedWrapper> associationProjections = new ArrayList<EmbeddedWrapper>();
		final PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);
		final ResourceMetadata metadata = associations.getMetadataFor(entity.getType());

		entity.doWithAssociations((SimpleAssociationHandler) association -> {

			PersistentProperty<?> property = association.getInverse();

			if (!associations.isLinkableAssociation(property)) {
				return;
			}

			if (!projector.hasExcerptProjection(property.getActualType())) {
				return;
			}

			Object value = accessor.getProperty(association.getInverse());

			if (value == null) {
				return;
			}

			LinkRelation rel = metadata.getMappingFor(property).getRel();

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
		});

		return associationProjections;
	}
}
