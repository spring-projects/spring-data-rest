/*
 * Copyright 2014 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * A strategy interface for producing {@link Patch} instances from a patch document representation (such as JSON Patch)
 * and rendering a Patch to a patch document representation. This decouples the {@link Patch} class from any specific
 * patch format or library that holds the representation.
 * </p>
 * <p>
 * For example, if the {@link Patch} is to be represented as JSON Patch, the representation type could be
 * {@link JsonNode} or some other JSON library's type that holds a JSON document.
 * </p>
 * 
 * @author Craig Walls
 * @param <T> A type holding a representation of the patch. For example, a JsonNode if working with JSON Patch.
 */
public interface PatchConverter<T> {

	/**
	 * Convert a patch document representation to a {@link Patch}.
	 * 
	 * @param patchRepresentation the representation of a patch.
	 * @return the {@link Patch} object that the document represents.
	 */
	Patch convert(T patchRepresentation);

	/**
	 * Convert a {@link Patch} to a representation object.
	 * 
	 * @param patch the {@link Patch} to convert.
	 * @return the patch representation object.
	 */
	T convert(Patch patch);

}
