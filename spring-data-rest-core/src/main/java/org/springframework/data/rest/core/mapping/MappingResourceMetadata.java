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
package org.springframework.data.rest.core.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * {@link ResourceMetadata} based on a {@link PersistentEntity}.
 * 
 * @author Oliver Gierke
 * @since 2.1
 */
public class MappingResourceMetadata extends TypeBasedCollectionResourceMapping implements ResourceMetadata {

	private final PersistentEntity<?, ?> entity;
	private final Map<PersistentProperty<?>, ResourceMapping> propertyMappings;

	/**
	 * Creates a new {@link MappingResourceMetadata} for the given {@link PersistentEntity}.
	 * 
	 * @param entity must not be {@literal null}.
	 */
	public MappingResourceMetadata(PersistentEntity<?, ?> entity) {

		super(entity.getType());

		this.entity = entity;
		this.propertyMappings = new HashMap<PersistentProperty<?>, ResourceMapping>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getDomainType()
	 */
	@Override
	public Class<?> getDomainType() {
		return entity.getType();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#isManagedResource(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public boolean isManagedResource(PersistentProperty<?> property) {
		return property.isAssociation();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#isExported(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public boolean isExported(PersistentProperty<?> property) {
		return getMappingFor(property).isExported();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getMappingFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public ResourceMapping getMappingFor(PersistentProperty<?> property) {

		ResourceMapping propertyMapping = propertyMappings.get(property);

		if (propertyMapping != null) {
			return propertyMapping;
		}

		propertyMapping = new RepositoryResourceMappings.PersistentPropertyResourceMapping(property, this, this);
		propertyMappings.put(property, propertyMapping);

		return propertyMapping;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getSearchResourceMappings()
	 */
	@Override
	public SearchResourceMappings getSearchResourceMappings() {
		return new SearchResourceMappings(Collections.<MethodResourceMapping> emptyList());
	}
}
