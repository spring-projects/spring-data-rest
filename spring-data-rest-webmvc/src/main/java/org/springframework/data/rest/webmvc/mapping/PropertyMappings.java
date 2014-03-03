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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.mapping.MappingResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;

/**
 * Value object for {@link ResourceMapping}s for {@link PersistentProperty} instances.
 * 
 * @author Oliver Gierke
 * @see 2.1
 */
public class PropertyMappings {

	private final ResourceMappings resourceMappings;
	private final Map<PersistentProperty<?>, ResourceMetadata> resourceMetadata;

	/**
	 * Creates a new {@link PropertyMappings} instance for the given {@link ResourceMappings}.
	 * 
	 * @param resourceMappings
	 */
	public PropertyMappings(ResourceMappings resourceMappings) {

		Assert.notNull(resourceMappings, "ResourceMappings must not be null!");

		this.resourceMappings = resourceMappings;
		this.resourceMetadata = new HashMap<PersistentProperty<?>, ResourceMetadata>();
	}

	/**
	 * Returns the {@link ResourceMapping} for the given {@link PersistentProperty}.
	 * 
	 * @param property can be {@literal null}.
	 * @return
	 */
	public ResourceMapping getMappingFor(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		ResourceMetadata metadata = resourceMetadata.get(property);

		if (metadata != null) {
			return metadata.getMappingFor(property);
		}

		metadata = resourceMappings.getMappingFor(property.getOwner().getType());

		if (metadata != null) {
			return cacheAndReturn(metadata, property);
		}

		return cacheAndReturn(new MappingResourceMetadata(property.getOwner()), property);
	}

	private ResourceMapping cacheAndReturn(ResourceMetadata metadata, PersistentProperty<?> property) {

		resourceMetadata.put(property, metadata);
		return metadata.getMappingFor(property);
	}
}
