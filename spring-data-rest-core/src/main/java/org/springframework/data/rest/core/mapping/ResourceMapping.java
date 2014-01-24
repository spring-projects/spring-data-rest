/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.data.rest.core.Path;

/**
 * Mapping information for components to be exported as REST resources.
 * 
 * @author Oliver Gierke
 */
public interface ResourceMapping {

	/**
	 * Returns whether the component shall be exported at all.
	 * 
	 * @return will never be {@literal null}.
	 */
	boolean isExported();

	/**
	 * Returns the relation for the resource exported.
	 * 
	 * @return will never be {@literal null}.
	 */
	String getRel();

	/**
	 * Returns the path the resource is exposed under.
	 * 
	 * @return will never be {@literal null}.
	 */
	Path getPath();

	/**
	 * Returns whether the resource is paging one.
	 * 
	 * @return
	 */
	boolean isPagingResource();

	/**
	 * Returns the resource's description.
	 * 
	 * @return
	 */
	ResourceDescription getDescription();
}
