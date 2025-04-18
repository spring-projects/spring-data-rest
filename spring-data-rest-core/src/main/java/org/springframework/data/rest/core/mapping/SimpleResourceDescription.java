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

import org.springframework.hateoas.LinkRelation;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class SimpleResourceDescription extends ResolvableResourceDescriptionSupport {

	public static final String DEFAULT_KEY_PREFIX = "rest.description";
	protected static final MediaType DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN;

	private final String message;
	private final MediaType mediaType;

	/**
	 * Creates a new {@link SimpleResourceDescription} with the given message and {@link MediaType}.
	 *
	 * @param message must not be {@literal null} or empty.
	 * @param mediaType must not be {@literal null} or empty.
	 */
	protected SimpleResourceDescription(String message, MediaType mediaType) {

		Assert.hasText(message, "Message must not be null or empty");
		Assert.notNull(mediaType, "MediaType must not be null");

		this.message = message;
		this.mediaType = mediaType;
	}

	public static ResourceDescription defaultFor(LinkRelation rel) {
		return new SimpleResourceDescription(String.format("%s.%s", DEFAULT_KEY_PREFIX, rel.value()), DEFAULT_MEDIA_TYPE);
	}

	public String getMessage() {
		return message;
	}

	public MediaType getType() {
		return mediaType;
	}

	public boolean isDefault() {
		return StringUtils.hasText(message) && message.startsWith(DEFAULT_KEY_PREFIX);
	}

	@Override
	public String[] getCodes() {
		return new String[] { message };
	}
}
