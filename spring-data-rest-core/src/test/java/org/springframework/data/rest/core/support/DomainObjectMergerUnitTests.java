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
package org.springframework.data.rest.core.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.core.support.DomainObjectMerger.*;

import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

/**
 * Unit tests for {@link DomainObjectMerger}.
 * 
 * @author Oliver Gierke
 */
public class DomainObjectMergerUnitTests {

	/**
	 * @see DATAREST-327
	 */
	@Test
	public void considersEmptyObjectsEmpty() {

		assertThat(isNullOrEmpty(null), is(true));
		assertThat(isNullOrEmpty(Collections.emptyList()), is(true));
		assertThat(isNullOrEmpty(new Object[0]), is(true));
		assertThat(isNullOrEmpty(new String[0]), is(true));
		assertThat(isNullOrEmpty(new MyIterable()), is(true));

		assertThat(isNullOrEmpty(new Object()), is(false));
		assertThat(isNullOrEmpty(Collections.singleton(new Object())), is(false));
		assertThat(isNullOrEmpty(new Object[] { "1" }), is(false));
		assertThat(isNullOrEmpty(new String[] { "1" }), is(false));
	}

	class MyIterable implements Iterable<Object> {

		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyList().iterator();
		}
	}
}
