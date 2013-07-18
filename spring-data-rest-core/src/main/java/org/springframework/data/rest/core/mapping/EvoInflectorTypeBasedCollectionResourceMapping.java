/*
 * Copyright 2013 the original author or authors.
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

import org.atteo.evo.inflector.English;
import org.springframework.hateoas.RelProvider;

/**
 * {@link TypeBasedCollectionResourceMapping} extension to use Evo Inflector to pluralize the simple class name by as
 * default path.
 * 
 * @author Oliver Gierke
 */
class EvoInflectorTypeBasedCollectionResourceMapping extends TypeBasedCollectionResourceMapping {

	/**
	 * Creates a new {@link EvoInflectorTypeBasedCollectionResourceMapping} for the given type and {@link RelProvider}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public EvoInflectorTypeBasedCollectionResourceMapping(Class<?> type, RelProvider relProvider) {
		super(type, relProvider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.TypeBasedCollectionResourceMapping#getDefaultPathFor(java.lang.Class)
	 */
	@Override
	protected String getDefaultPathFor(Class<?> type) {
		return English.plural(super.getDefaultPathFor(type));
	}
}
