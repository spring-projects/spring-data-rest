/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import java.util.Optional;

/**
 * Contextual mapping for he translation of JSON Pointer segments into property references on persistent types.
 *
 * @author Oliver Drotbohm
 */
public interface BindContext {

	/**
	 * Returns the name of the writable property for the given JSON pointer segment.
	 *
	 * @param segment must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Optional<String> getWritableProperty(String segment, Class<?> type);

	/**
	 * Return the name of the readable property for the given JSON pointer segment.
	 *
	 * @param segment must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Optional<String> getReadableProperty(String segment, Class<?> type);
}
