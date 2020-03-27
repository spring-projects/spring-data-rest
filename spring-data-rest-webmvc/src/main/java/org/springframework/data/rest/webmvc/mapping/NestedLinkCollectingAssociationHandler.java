/*
 * Copyright 2016-2020 the original author or authors.
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
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class NestedLinkCollectingAssociationHandler implements SimpleAssociationHandler {

	private final EntityLinks entityLinks;
	private final PersistentEntities entities;
	private final PersistentPropertyAccessor<?> accessor;
	private final ResourceMappings mappings;

	private final @Getter List<Link> links = new ArrayList<Link>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
	 */
	@Override
	public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

		PersistentProperty<?> property = association.getInverse();
		Object propertyValue = accessor.getProperty(property);

		ResourceMetadata metadata = mappings.getMetadataFor(property.getOwner().getType());
		ResourceMapping propertyMapping = metadata.getMappingFor(property);

		if (property.isCollectionLike()) {

			for (Object element : (Collection<?>) propertyValue) {

				PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(element.getClass());
				IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(element);

				links.add(entityLinks.linkForItemResource(element.getClass(), identifierAccessor.getIdentifier())
						.withRel(propertyMapping.getRel()));
			}

		} else {

			PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(propertyValue.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(propertyValue);

			links.add(entityLinks.linkForItemResource(propertyValue.getClass(), identifierAccessor.getIdentifier())
					.withRel(propertyMapping.getRel()));
		}
	}
}
