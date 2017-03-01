/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.core.support;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.plugin.core.Plugin;

/**
 * SPI to customize which property of an entity is used as unique identifier and how the entity instance is looked up
 * from the backend. Prefer to extend {@link EntityLookupSupport} to let the generics declaration be used for the
 * {@link #supports(Object)} method automatically.
 * 
 * @author Oliver Gierke
 * @see EntityLookupSupport
 * @see DefaultSelfLinkProvider
 * @since 2.5
 * @soundtrack Elephants Crossing - Echo (Live at Stadtfest Dresden -
 *             https://soundcloud.com/elephants-crossing/sets/live-at-stadtfest-dresden)
 */
public interface EntityLookup<T> extends Plugin<Class<?>> {

	/**
	 * Returns the property of the given entity that shall be used to uniquely identify it. If no {@link EntityLookup} is
	 * defined for a particular type, a standard identifier lookup mechanism (i.e. the datastore identifier) will be used
	 * to eventually create an identifying URI.
	 * 
	 * @param entity will never be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	Serializable getResourceIdentifier(T entity);

	/**
	 * Returns the entity instance to be used if an entity with the given identifier value is requested. Implementations
	 * will usually forward the call to a repository method explicitly and can assume the given value be basically the
	 * value they returned in {@link #getResourceIdentifier(Object)}.
	 * <p>
	 * Implementations are free to return {@literal null} to indicate absence of a value or wrap the result into any
	 * generally supported {@code Optional} type.
	 * 
	 * @param id will never be {@literal null}.
	 * @return can be {@literal null}.
	 */
	Optional<Object> lookupEntity(Serializable id);
}
