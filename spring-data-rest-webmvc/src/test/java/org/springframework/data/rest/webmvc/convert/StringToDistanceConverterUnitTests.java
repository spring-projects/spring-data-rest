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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.webmvc.convert.StringToDistanceConverter.*;

import org.junit.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;

/**
 * Unit tests for {@link StringToDistanceConverter}.
 * 
 * @author Oliver Gierke
 */
public class StringToDistanceConverterUnitTests {

	/**
	 * @see DATAREST-279
	 */
	@Test
	public void parsesDistanceFromString() {

		Distance reference = new Distance(10.8, Metrics.KILOMETERS);

		assertThat(INSTANCE.convert("10.8km"), is(reference));
		assertThat(INSTANCE.convert(" 10.8km"), is(reference));
		assertThat(INSTANCE.convert(" 10.8 km"), is(reference));
		assertThat(INSTANCE.convert(" 10.8 km "), is(reference));
	}

	/**
	 * @see DATAREST-279
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsArbitraryNonsense() {
		INSTANCE.convert("foo");
	}

	/**
	 * @see DATAREST-279
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnsupportedMetric() {
		INSTANCE.convert("10.8cm");
	}
}
