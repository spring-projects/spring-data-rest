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
 * A factory to create projecting instances for other objects usually used to allow easy creation of representation
 * projections to define which properties of a domain objects shall be exported in which way.
 * 
 * @author Oliver Gierke
 */
public interface ProjectionFactory {

	/**
	 * Creates a projection of the given type for the given source object. The individual mapping strategy is defined by
	 * the implementations.
	 * 
	 * @param source the object to create a projection for, can be {@literal null}
	 * @param projectionType the type to create.
	 * @return
	 */
	<T> T createProjection(Object source, Class<T> projectionType);
}
