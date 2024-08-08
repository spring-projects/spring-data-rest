/*
 * Copyright 2022-2024 the original author or authors.
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
import java.util.function.BiFunction;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Drotbohm
 */
class JsonPointerMapping {

	private final BiFunction<String, Class<?>, Optional<String>> reader, writer;

	public JsonPointerMapping(BindContext context) {

		this.reader = context::getReadableProperty;
		this.writer = context::getWritableProperty;
	}

	public JsonPointerMapping(BiFunction<String, Class<?>, Optional<String>> reader,
			BiFunction<String, Class<?>, Optional<String>> writer) {
		this.reader = reader;
		this.writer = writer;
	}

	/**
	 * Maps the given JSON Pointer to the given type to ultimately read the attribute pointed to.
	 *
	 * @param pointer must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return a JSON Pointer with the segments translated into the matching property references.
	 */
	public String forRead(String pointer, Class<?> type) {
		return verify(pointer, type, reader, "readable");
	}

	/**
	 * Maps the given JSON Pointer to the given type to ultimately write the attribute pointed to.
	 *
	 * @param pointer must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return a JSON Pointer with the segments translated into the matching property references.
	 */
	public String forWrite(String pointer, Class<?> type) {
		return verify(pointer, type, writer, "writable");
	}

	private String verify(String pointer, Class<?> type, BiFunction<String, Class<?>, Optional<String>> filter,
			String qualifier) {

		String[] strings = pointer.split("/");

		if (strings.length == 0) {
			return pointer;
		}

		StringBuilder result = new StringBuilder();
		TypeInformation<?> currentType = ClassTypeInformation.from(type);

		for (int i = 0; i < strings.length; i++) {

			String segment = strings[i];

			if (!StringUtils.hasText(segment)) {
				continue;
			}

			if (currentType != null && currentType.isMap()) {

				result.append("/").append(segment);
				currentType = currentType.getActualType();

				continue;
			}

			if (segment.equals("-") || segment.matches("\\d+")) {
				result.append("/").append(segment);
				currentType = currentType.getActualType();
				continue;
			}

			TypeInformation<?> rejectType = currentType;

			// Use given filter for final segment, reader otherwise
			String property = (i == strings.length - 1 ? filter : reader) //
					.apply(segment, currentType.getType()) //
					.orElseThrow(() -> reject(segment, rejectType, pointer, qualifier));

			try {
				currentType = PropertyPath.from(property, currentType).getTypeInformation();
			} catch (PropertyReferenceException o_O) {
				throw reject(segment, rejectType, pointer, qualifier);
			}

			result.append("/").append(property);
		}

		return result.toString();
	}

	private static PatchException reject(String segment, TypeInformation<?> type, String pointer, String qualifier) {

		return new PatchException(
				String.format("Couldn't find %s property for pointer segment %s on %s in %s", qualifier, segment,
						type.getType(), pointer));
	}
}
