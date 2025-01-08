/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * An API to discover the {@link HttpMethod}s supported on a given {@link ResourceType}.
 *
 * @author Oliver Gierke
 */
public interface SupportedHttpMethods {

	/**
	 * Returns the supported {@link HttpMethod}s for the given {@link ResourceType}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	HttpMethods getMethodsFor(ResourceType type);

	/**
	 * Returns the supported {@link HttpMethod}s for the given {@link PersistentProperty}.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	HttpMethods getMethodsFor(PersistentProperty<?> property);

	/**
	 * Returns whether {@link HttpMethod#PUT} requests are supported for item resource creation.
	 *
	 * @return
	 */
	default boolean allowsPutForCreation() {
		return true;
	}

	/**
	 * Null object to abstract the absence of any support for any HTTP method.
	 *
	 * @author Oliver Gierke
	 */
	enum NoSupportedMethods implements SupportedHttpMethods {

		INSTANCE;

		@Override
		public HttpMethods getMethodsFor(ResourceType resourcType) {
			return HttpMethods.none();
		}

		@Override
		public HttpMethods getMethodsFor(PersistentProperty<?> property) {
			return HttpMethods.none();
		}

		@Override
		public boolean allowsPutForCreation() {
			return false;
		}
	}
}
