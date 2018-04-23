/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.core.GenericTypeResolver;

/**
 * {@link EntityLookup} implementation base class to derive the supported domain type from the generics signature.
 *
 * @author Oliver Gierke
 * @since 2.5
 * @soundtrack Elephants Crossing - The New (Live at Stadtfest Dresden -
 *             https://soundcloud.com/elephants-crossing/sets/live-at-stadtfest-dresden)
 */
public abstract class EntityLookupSupport<T> implements EntityLookup<T> {

	private final Class<?> domainType;

	/**
	 * Creates a new {@link EntityLookupSupport} instance discovering the supported type from the generics signature.
	 */
	public EntityLookupSupport() {
		this.domainType = GenericTypeResolver.resolveTypeArgument(getClass(), EntityLookup.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Class<?> delimiter) {
		return domainType.isAssignableFrom(delimiter);
	}
}
