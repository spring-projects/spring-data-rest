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
package org.springframework.data.rest.convert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin
 */
public class ISO8601DateConverter implements ConditionalGenericConverter, Converter<String[], Date> {

	public static final ConditionalGenericConverter INSTANCE = new ISO8601DateConverter();

	private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS = new HashSet<ConvertiblePair>();

	static {
		CONVERTIBLE_PAIRS.add(new ConvertiblePair(String.class, Date.class));
		CONVERTIBLE_PAIRS.add(new ConvertiblePair(Date.class, String.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (String.class.isAssignableFrom(sourceType.getType())) {
			return Date.class.isAssignableFrom(targetType.getType());
		}

		return Date.class.isAssignableFrom(sourceType.getType()) && String.class.isAssignableFrom(targetType.getType());
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

		DateFormat dateFmt = iso8601DateFormat();

		if (String.class.isAssignableFrom(sourceType.getType())) {
			return dateFmt.format(source);
		}

		try {
			return dateFmt.parse(source.toString());
		} catch (ParseException e) {
			throw new ConversionFailedException(sourceType, targetType, source, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public Date convert(String[] source) {

		if (source.length == 0) {
			return null;
		}

		try {
			return iso8601DateFormat().parse(source[0]);
		} catch (ParseException e) {
			throw new ConversionFailedException(TypeDescriptor.valueOf(String[].class), TypeDescriptor.valueOf(Date.class),
					source[0], new IllegalArgumentException(
							"Source does not conform to ISO8601 date format (YYYY-MM-DDTHH:MM:SS-0000"));
		}
	}

	private DateFormat iso8601DateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}
}
