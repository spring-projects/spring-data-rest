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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.util.StringUtils;

/**
 * Converter to create {@link Distance} instances from {@link String} representations. The supported format is a decimal
 * followed by whitespace and a metric abbreviation. We currently support the following abbreviations:
 * {@value #SUPPORTED_METRICS}.
 * 
 * @author Oliver Gierke
 */
public enum StringToDistanceConverter implements Converter<String, Distance> {

	INSTANCE;

	private static final Map<String, Metric> SUPPORTED_METRICS;
	private static final String INVALID_DISTANCE = "Expected double amount optionally followed by a metrics abbreviation (%s) but got '%s'!";

	static {

		Map<String, Metric> metrics = new LinkedHashMap<String, Metric>();
		metrics.put("km", Metrics.KILOMETERS);
		metrics.put("miles", Metrics.MILES);
		metrics.put("mile", Metrics.MILES);

		SUPPORTED_METRICS = Collections.unmodifiableMap(metrics);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public Distance convert(String source) {

		source = source.trim();

		for (Entry<String, Metric> metric : SUPPORTED_METRICS.entrySet()) {
			if (source.endsWith(metric.getKey())) {
				return fromString(source, metric);
			}
		}

		try {
			return new Distance(Double.parseDouble(source));
		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_DISTANCE,
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_METRICS.keySet()), source));
		}
	}

	private Distance fromString(String source, Entry<String, Metric> metric) {

		String amountString = source.substring(0, source.indexOf(metric.getKey()));

		try {
			return new Distance(Double.parseDouble(amountString), metric.getValue());
		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_DISTANCE,
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_METRICS.keySet()), source));
		}
	}
}
