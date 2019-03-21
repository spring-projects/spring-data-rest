/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonMappingException;

@EqualsAndHashCode
@RequiredArgsConstructor(staticName = "of")
public class DeserializationErrors implements Iterable<DeserializationErrors.DeserializationError> {

	static final DeserializationErrors NONE = new DeserializationErrors(null,
			Collections.<DeserializationError> emptyList());

	private final String propertyBase;
	private final List<DeserializationError> errors;

	/**
	 * Returns whether the {@link DeserializationErrors} contain an error for the given path.
	 * 
	 * @param path must not be {@literal null} or empty.
	 * @return
	 */
	boolean hasErrorForPath(String path) {

		Assert.hasText(path, "Path must not be null or empty!");

		for (DeserializationErrors.DeserializationError candidate : errors) {
			if (candidate.getPath().startsWith(path)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a new {@link DeserializationErrors} with the given {@link DeserializationError} added to the currently
	 * captured ones.
	 * 
	 * @param error must not be {@literal null}.
	 * @return
	 */
	DeserializationErrors and(DeserializationError error) {

		Assert.notNull(error, "DeserializationError must not be null!");

		List<DeserializationError> errors = new ArrayList<DeserializationError>(this.errors.size() + 1);
		errors.addAll(this.errors);
		errors.add(error);

		return new DeserializationErrors(propertyBase, errors);
	}

	DeserializationErrors and(DeserializationErrors errors) {

		DeserializationErrors result = this;

		for (DeserializationError error : errors) {
			result = result.and(error);
		}

		return result;
	}

	public DeserializationErrors reject(String path, Object value, JsonMappingException exception) {

		String completePath = StringUtils.hasText(propertyBase) ? String.format("%s.%s", propertyBase, path) : path;

		return and(DeserializationError.of(completePath, value, exception));
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<DeserializationError> iterator() {
		return errors.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append("Deserialization errors:\n");

		for (DeserializationError error : errors) {
			builder.append("- ").append(error.toString()).append("\n");
		}

		return builder.toString();
	}

	@Value(staticConstructor = "of")
	public static class DeserializationError {

		String path;
		Object source;
		JsonMappingException exception;

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return String.format("Path: %s, Source: %s, Exception message: %s", //
					path, source, exception.getMessage().replaceAll("\n", ""));
		}
	}
}
