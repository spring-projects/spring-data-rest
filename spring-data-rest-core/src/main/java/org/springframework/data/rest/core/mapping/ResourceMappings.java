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


/**
 * @author Oliver Gierke
 */
public interface ResourceMappings extends Iterable<ResourceMetadata> {

	/**
	 * Returns a {@link ResourceMetadata} for the given type if available.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	ResourceMetadata getMappingFor(Class<?> type);

	/**
	 * Returns the {@link ResourceMapping}s for the search resources of the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	SearchResourceMappings getSearchResourceMappings(Class<?> type);

	/**
	 * Returns whether we have a {@link ResourceMapping} for the given type and it is exported.
	 * 
	 * @param type
	 * @return
	 */
	boolean exportsMappingFor(Class<?> type);

	/**
	 * Returns whether we export a top-level resource for the given path.
	 * 
	 * @param path must not be {@literal null} or empty.
	 * @return
	 */
	boolean exportsTopLevelResourceFor(String path);

	/**
	 * Returns whether we have a {@link ResourceMapping} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	boolean hasMappingFor(Class<?> type);
}
