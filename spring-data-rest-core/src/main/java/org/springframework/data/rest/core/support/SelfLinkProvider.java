/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.core.support;

import org.springframework.hateoas.Link;

/**
 * Component to create self links for entity instances.
 *
 * @author Oliver Gierke
 * @since 2.5
 * @soundtrack Trio Rotation - Rotation
 */
public interface SelfLinkProvider {

	/**
	 * Returns the self link for the given entity instance.
	 *
	 * @param instance must never be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Link createSelfLinkFor(Object instance);
}
