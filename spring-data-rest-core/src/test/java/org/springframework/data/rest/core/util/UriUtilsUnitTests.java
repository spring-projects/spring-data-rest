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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.net.URI;

import org.junit.Test;

/**
 * Tests to verify that {@link UriUtils} can manipulate {@link URI}s.
 * 
 * @author Jon Brisbin
 */
public class UriUtilsUnitTests {

	private static final String BASE_URI_STR = "http://localhost:8080/data";
	private static final URI BASE_URI = URI.create(BASE_URI_STR);

	private static final String PERSON_2LVL_STR = BASE_URI_STR + "/person/1";
	private static final URI PERSON_2LVL_URI = URI.create(PERSON_2LVL_STR);

	@Test
	public void shouldBuildURIFromPathSegments() throws Exception {

		URI uri = UriUtils.buildUri(BASE_URI, "person", "1");
		assertThat(uri, is(PERSON_2LVL_URI));
	}
}
