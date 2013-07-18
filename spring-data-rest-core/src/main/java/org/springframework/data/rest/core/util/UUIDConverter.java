/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.core.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * For converting a {@link UUID} into a {@link String}.
 * 
 * @author Jon Brisbin
 */
public class UUIDConverter implements ConditionalGenericConverter {

	public static final UUIDConverter INSTANCE = new UUIDConverter();
	private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS = new HashSet<ConvertiblePair>();

	static {
		CONVERTIBLE_PAIRS.add(new ConvertiblePair(String.class, UUID.class));
		CONVERTIBLE_PAIRS.add(new ConvertiblePair(UUID.class, String.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (String.class.isAssignableFrom(sourceType.getType())) {
			return UUID.class.isAssignableFrom(targetType.getType());
		}

		return UUID.class.isAssignableFrom(sourceType.getType()) && String.class.isAssignableFrom(targetType.getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_PAIRS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (String.class.isAssignableFrom(sourceType.getType())) {
			return UUID.fromString(source.toString());
		} else {
			return source.toString();
		}
	}
}
