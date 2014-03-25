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

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class SimpleResourceDescription extends ResolvableResourceDescriptionSupport {

	protected static final String DEFAULT_KEY_PREFIX = "rest.description";
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

		Assert.hasText(message, "Message must not be null or empty!");
		Assert.notNull(mediaType, "MediaType must not be null!");

		this.message = message;
		this.mediaType = mediaType;
	}

	public static ResourceDescription defaultFor(String rel) {
		return new SimpleResourceDescription(String.format("%s.%s", DEFAULT_KEY_PREFIX, rel), DEFAULT_MEDIA_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceDescription#getMessage()
	 */
	public String getMessage() {
		return message;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceDescription#getType()
	 */
	public MediaType getType() {
		return mediaType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceDescription#isDefault()
	 */
	public boolean isDefault() {
		return StringUtils.hasText(message) && message.startsWith(DEFAULT_KEY_PREFIX);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.MessageSourceResolvable#getCodes()
	 */
	@Override
	public String[] getCodes() {
		return new String[] { message };
	}
}
