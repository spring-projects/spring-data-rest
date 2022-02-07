/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.SupportedHttpMethods.NoSupportedMethods;
import org.springframework.util.Assert;

/**
 * {@link RootResourceMetadata} based on a {@link PersistentEntity}.
 *
 * @author Oliver Gierke
 * @since 2.1
 */
class MappingResourceMetadata extends TypeBasedCollectionResourceMapping implements ResourceMetadata {

	private final PersistentEntity<?, ?> entity;
	private final PropertyMappings propertyMappings;
	private final boolean explicitlyExported;

	/**
	 * Creates a new {@link MappingResourceMetadata} for the given {@link PersistentEntity}.
	 *
	 * @param entity must not be {@literal null}.
	 */
	public MappingResourceMetadata(PersistentEntity<?, ?> entity, ResourceMappings resourceMappings) {

		super(entity.getType());

		this.propertyMappings = new PropertyMappings(resourceMappings);

		this.entity = entity;
		this.entity.doWithAssociations(propertyMappings);
		this.entity.doWithProperties(propertyMappings);

		this.explicitlyExported = Optional.ofNullable(entity.findAnnotation(RestResource.class))//
				.map(it -> it.exported())//
				.orElse(false);
	}

	@Override
	public Class<?> getDomainType() {
		return entity.getType();
	}

	@Override
	public boolean isExported(PersistentProperty<?> property) {
		return getMappingFor(property).isExported();
	}

	@Override
	public ResourceMapping getMappingFor(PersistentProperty<?> property) {
		return propertyMappings.getMappingFor(property);
	}

	@Override
	public SearchResourceMappings getSearchResourceMappings() {
		return new SearchResourceMappings(Collections.<MethodResourceMapping> emptyList());
	}

	@Override
	public SupportedHttpMethods getSupportedHttpMethods() {
		return NoSupportedMethods.INSTANCE;
	}

	@Override
	public PropertyAwareResourceMapping getProperty(String mappedPath) {
		return propertyMappings.getMappingFor(mappedPath);
	}

	@Override
	public boolean isExported() {
		return explicitlyExported;
	}

	/**
	 * Value object for {@link ResourceMapping}s for {@link PersistentProperty} instances.
	 *
	 * @author Oliver Gierke
	 * @see 2.1
	 */
	private static class PropertyMappings implements SimpleAssociationHandler, SimplePropertyHandler {

		private final ResourceMappings resourceMappings;
		private final Map<PersistentProperty<?>, PropertyAwareResourceMapping> propertyMappings;

		/**
		 * Creates a new {@link PropertyMappings} instance for the given {@link ResourceMappings}.
		 *
		 * @param resourceMappings
		 */
		public PropertyMappings(ResourceMappings resourceMappings) {

			Assert.notNull(resourceMappings, "ResourceMappings must not be null!");

			this.resourceMappings = resourceMappings;
			this.propertyMappings = new HashMap<PersistentProperty<?>, PropertyAwareResourceMapping>();
		}

		@Override
		public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {
			doWithPersistentProperty(association.getInverse());
		}

		@Override
		public void doWithPersistentProperty(PersistentProperty<?> property) {

			Assert.notNull(property, "PersistentProperty must not be null!");

			this.propertyMappings.put(property, new PersistentPropertyResourceMapping(property, resourceMappings));

		}

		/**
		 * Returns the {@link PropertyAwareResourceMapping} for the given mapped path.
		 *
		 * @param mappedPath must not be {@literal null} or empty.
		 * @return the {@link PropertyAwareResourceMapping} if found, {@literal null} otherwise.
		 */
		public PropertyAwareResourceMapping getMappingFor(String mappedPath) {

			Assert.hasText(mappedPath, "Mapped path must not be null or empty!");

			for (PropertyAwareResourceMapping mapping : propertyMappings.values()) {
				if (mapping.getPath().matches(mappedPath)) {
					return mapping;
				}
			}

			return null;
		}

		/**
		 * Returns the {@link ResourceMapping} for the given {@link PersistentProperty}.
		 *
		 * @param property must not be {@literal null}.
		 * @return
		 */
		public ResourceMapping getMappingFor(PersistentProperty<?> property) {
			return propertyMappings.get(property);
		}
	}
}
