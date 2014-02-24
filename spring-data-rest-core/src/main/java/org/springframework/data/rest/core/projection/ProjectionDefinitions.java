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
package org.springframework.data.rest.core.projection;

/**
 * Interface to allow the lookup of a projection interface by source type and name. This allows the definition of
 * projections with the same name for different source types.
 * 
 * @author Oliver Gierke
 */
public interface ProjectionDefinitions {

	/**
	 * Returns the projection type for the given source type and name.
	 * 
	 * @param sourceType must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	Class<?> getProjectionType(Class<?> sourceType, String name);

	/**
	 * Returns whether we have a projection registered for the given source type.
	 * 
	 * @param sourceType must not be {@literal null}.
	 * @return
	 */
	boolean hasProjectionFor(Class<?> sourceType);

	/**
	 * Returns the request parameter to be used to expose the projection to the web.
	 * 
	 * @return the parameterName will never be {@literal null} or empty.
	 */
	String getParameterName();
}
