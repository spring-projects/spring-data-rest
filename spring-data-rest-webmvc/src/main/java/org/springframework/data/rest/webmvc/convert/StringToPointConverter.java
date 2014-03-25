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
package org.springframework.data.rest.webmvc.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.geo.Point;

/**
 * Converter to parse two comma-separated doubles into a {@link Point}.
 * 
 * @author Oliver Gierke
 */
public enum StringToPointConverter implements Converter<String, Point> {

	INSTANCE;

	public static final ConvertiblePair CONVERTIBLE = new ConvertiblePair(String.class, Point.class);

	private static final String INVALID_FORMAT = "Expected two doubles separated by a semicolon but got '%s'!";

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public Point convert(String source) {

		String[] parts = source.split(",");

		if (parts.length != 2) {
			throw new IllegalArgumentException(String.format(INVALID_FORMAT, source));
		}

		try {

			double latitude = Double.parseDouble(parts[0]);
			double longitude = Double.parseDouble(parts[1]);

			return new Point(longitude, latitude);

		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_FORMAT, source), o_O);
		}
	}
}
