/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.http.HttpMethod;

/**
 * Interface for metadata of resources exposed through the system.
 *
 * @author Oliver Gierke
 */
public interface ResourceMetadata extends CollectionResourceMapping {

	/**
	 * Returns the domain type that is exposed through the resource.
	 *
	 * @return
	 */
	Class<?> getDomainType();

	/**
	 * Returns whether the given {@link PersistentProperty} is a managed resource and in fact exported.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	boolean isExported(PersistentProperty<?> property);

	/**
	 * Returns the {@link PropertyAwareResourceMapping} for the given mapped path.
	 *
	 * @param mappedPath must not be {@literal null} or empty.
	 * @return the {@link PropertyAwareResourceMapping} for the given path or {@literal null} if none found.
	 */
	PropertyAwareResourceMapping getProperty(String mappedPath);

	/**
	 * Returns the {@link ResourceMapping} for the given {@link PersistentProperty} or {@literal null} if not managed.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	ResourceMapping getMappingFor(PersistentProperty<?> property);

	/**
	 * Returns the {@link SearchResourceMappings}, i.e. the mappings for the search resource exposed for the current
	 * resource.
	 *
	 * @return
	 */
	SearchResourceMappings getSearchResourceMappings();

	/**
	 * Returns the supported {@link HttpMethod}s for the given {@link ResourceType}.
	 *
	 * @param resourcType must not be {@literal null}.
	 * @return
	 */
	SupportedHttpMethods getSupportedHttpMethods();
}
