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

/**
 * A custom resource mapping for collection resources.
 * 
 * @author Oliver Gierke
 */
public interface CollectionResourceMapping extends ResourceMapping {

	/**
	 * Returns the relation type pointing to the item resource within a collection.
	 * 
	 * @return
	 */
	String getItemResourceRel();

	/**
	 * Returns the {@link ResourceDescription} for the item resource.
	 * 
	 * @return
	 */
	ResourceDescription getItemResourceDescription();

	/**
	 * Returns the projection type to be used when embedding item resources into collections and related resources. If
	 * {@literal null} is returned this will mean full rendering for collections and no rendering for related resources.
	 * 
	 * @return
	 */
	Class<?> getExcerptProjection();
}
