/*
 * Copyright 2014-2016 the original author or authors.
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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.util.Assert;

/**
 * A value object to for {@link Link}s representing associations.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @since 2.1
 */
@RequiredArgsConstructor
public class Associations {

	private final @NonNull @Getter ResourceMappings mappings;
	private final @NonNull RepositoryRestConfiguration config;

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

		if (isLinkableAssociation(association)) {

			PersistentProperty<?> property = association.getInverse();
			ResourceMetadata metadata = mappings.getMetadataFor(property.getOwner().getType());
			ResourceMapping propertyMapping = metadata.getMappingFor(property);

			String href = path.slash(propertyMapping.getPath()).toString();
			String rel = propertyMapping.getRel();

			return Collections.singletonList(new Link(href, rel));
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the {@link ResourceMetadata} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public ResourceMetadata getMetadataFor(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return mappings.getMetadataFor(type);
	}

	/**
	 * Returns whether the type of the given {@link PersistentProperty} is configured as lookup type.
	 * 
	 * @param property can be {@literal null}.
	 * @return
	 */
	public boolean isLookupType(PersistentProperty<?> property) {
		return property == null ? false : config.isLookupType(property.getActualType());
	}

	public boolean isIdExposed(PersistentEntity<?, ?> entity) {
		return config.isIdExposedFor(entity.getType());
	}

	/**
	 * Returns whether the given {@link Association} is linkable.
	 * 
	 * @param association must not be {@literal null}.
	 * @return
	 */
	public boolean isLinkableAssociation(Association<? extends PersistentProperty<?>> association) {

		Assert.notNull(association, "Association must not be null!");

		return isLinkableAssociation(association.getInverse());
	}

	/**
	 * Returns whether the given property is an association that is linkable.
	 * 
	 * @param property can be {@literal null}.
	 * @return
	 */
	public boolean isLinkableAssociation(PersistentProperty<?> property) {

		if (property == null || !property.isAssociation() || config.isLookupType(property.getActualType())) {
			return false;
		}

		ResourceMetadata metadata = mappings.getMetadataFor(property.getOwner().getType());

		if (metadata != null && !metadata.isExported(property)) {
			return false;
		}

		metadata = mappings.getMetadataFor(property.getActualType());
		return metadata == null ? false : metadata.isExported();
	}
}
