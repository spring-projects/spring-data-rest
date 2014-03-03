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

import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.util.Assert;

/**
 * A value object to for {@link Link}s representing an association.
 * 
 * @author Oliver Gierke
 * @since 2.1
 */
public class AssociationLinks {

	private final ResourceMappings mappings;
	private final PropertyMappings propertyMappings;

	/**
	 * Creates a new {@link AssociationLinks} using the given {@link ResourceMappings}.
	 * 
	 * @param mappings must not be {@literal null}.
	 */
	public AssociationLinks(ResourceMappings mappings) {

		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.propertyMappings = new PropertyMappings(mappings);
		this.mappings = mappings;
	}

	/**
	 * Returns the links to render for the given {@link Association}.
	 * 
	 * @param association must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public List<Link> getLinksFor(Association<? extends PersistentProperty<?>> association, Path path) {

		Assert.notNull(association, "Association must not be null!");
		Assert.notNull(path, "Base path must not be null!");

		PersistentProperty<?> property = association.getInverse();

		if (isLinkableAssociation(property)) {

			ResourceMapping propertyMapping = propertyMappings.getMappingFor(property);

			String href = path.slash(propertyMapping.getPath()).toString();
			String rel = propertyMapping.getRel();

			return Collections.singletonList(new Link(href, rel));
		}

		return Collections.emptyList();
	}

	/**
	 * Returns whether the given property is an association that is linkable.
	 * 
	 * @param property can be {@literal null}.
	 * @return
	 */
	public boolean isLinkableAssociation(PersistentProperty<?> property) {

		if (property == null || !property.isAssociation()) {
			return false;
		}

		ResourceMetadata metadata = mappings.getMappingFor(property.getOwner().getType());

		if (metadata != null && !metadata.isExported(property)) {
			return false;
		}

		metadata = mappings.getMappingFor(property.getActualType());
		return metadata == null ? false : metadata.isExported();
	}
}
