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
import static org.springframework.data.rest.webmvc.convert.StringToPointConverter.*;

import org.junit.Test;
import org.springframework.data.geo.Point;

/**
 * Unit tests for {@link StringToPointConverter}.
 * 
 * @author Oliver Gierke
 */
public class StringToPointConverterUnitTests {

	/**
	 * @see DATAREST-279
	 */
	@Test
	public void parsesPointFromString() {

		Point reference = new Point(20.9, 10.8);

		assertThat(INSTANCE.convert("10.8,20.9"), is(reference));
		assertThat(INSTANCE.convert(" 10.8,20.9 "), is(reference));
		assertThat(INSTANCE.convert(" 10.8 ,20.9"), is(reference));
		assertThat(INSTANCE.convert(" 10.8 , 20.9 "), is(reference));
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
	public void rejectsMoreThanTwoCoordinates() {
		INSTANCE.convert("10.8,20.9,30.10");
	}

	/**
	 * @see DATAREST-279
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidCoordinate() {
		INSTANCE.convert("10.8,foo");
	}
}
