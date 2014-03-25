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

import java.util.Arrays;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * {@link SimpleResourceDescription} that additionally captures a type to be able to potentially create a reasonable
 * default message. The implementation will do so for enum types by rendering the available values as default message
 * and also provide them as arguments for message resolution.
 * 
 * @author Oliver Gierke
 */
public class TypedResourceDescription extends SimpleResourceDescription {

	private final Class<?> type;

	/**
	 * Creates a new {@link TypedResourceDescription} for the given message, {@link MediaType} and type.
	 * 
	 * @param message must not be {@literal null} or empty.
	 * @param mediaType must not be {@literal null} or empty.
	 * @param type can be {@literal null}, defaults to {@link Object}.
	 */
	private TypedResourceDescription(String message, MediaType mediaType, Class<?> type) {

		super(message, mediaType);

		this.type = type == null ? Object.class : type;
	}

	public static ResourceDescription defaultFor(String rel, PersistentProperty<?> property) {

		String message = String.format("%s.%s.%s", DEFAULT_KEY_PREFIX, rel, property.getName());
		return new TypedResourceDescription(message, DEFAULT_MEDIA_TYPE, property.getType());
	}

	public static ResourceDescription defaultFor(String rel, Class<?> type) {

		String message = String.format("%s.%s", DEFAULT_KEY_PREFIX, rel);
		return new TypedResourceDescription(message, DEFAULT_MEDIA_TYPE, type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResolvableResourceDescriptionSupport#getArguments()
	 */
	@Override
	public Object[] getArguments() {
		return type.isEnum() ? new Object[] { getEnumValues(type) } : new Object[0];
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResolvableResourceDescriptionSupport#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return type.isEnum() ? getEnumValues(type) : null;
	}

	private String getEnumValues(Class<?> type) {
		return StringUtils.collectionToDelimitedString(Arrays.asList(type.getEnumConstants()), ", ");
	}
}
