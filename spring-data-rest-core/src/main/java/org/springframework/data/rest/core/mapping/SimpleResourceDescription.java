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
package org.springframework.data.rest.core.mapping;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class SimpleResourceDescription extends ResolvableResourceDescriptionSupport {

	private static final String DEFAULT_KEY_PREFIX = "rest.description";
	private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN;

	private String message;
	private MediaType type;

	private SimpleResourceDescription(String message, MediaType mediaType) {
		this.message = message;
		this.type = mediaType;
	}

	public static ResourceDescription defaultFor(PersistentProperty<?> property, String rel) {

		String message = String.format("%s.%s.%s", DEFAULT_KEY_PREFIX, rel, property.getName());
		return new SimpleResourceDescription(message, DEFAULT_MEDIA_TYPE);
	}

	public static ResourceDescription defaultFor(String rel) {

		String message = String.format("%s.%s", DEFAULT_KEY_PREFIX, rel);
		return new SimpleResourceDescription(message, DEFAULT_MEDIA_TYPE);
	}

	public static ResourceDescription defaultForCollection(Class<?> type) {
		return null;
	}

	public static ResourceDescription defaultForMethod(RepositoryMethodResourceMapping mapping) {
		return null;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	public MediaType getType() {
		return type;
	}

	public boolean isDefault() {
		return StringUtils.hasText(message) && message.startsWith(DEFAULT_KEY_PREFIX);
	}
}
