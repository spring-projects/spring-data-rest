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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link BaseUri}.
 * 
 * @author Oliver Gierke
 */
public class BaseUriUnitTests {

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void doesNotMatchNonOverlap() {

		assertThat(new BaseUri(URI.create("foo")).getRepositoryLookupPath("/bar"), is(nullValue()));
		assertThat(new BaseUri(URI.create("http://localhost:8080/foo/")).getRepositoryLookupPath("/bar"), is(nullValue()));
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void matchesSimpleBaseUri() {

		BaseUri uri = new BaseUri(URI.create("foo"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void ignoresTrailingSlash() {

		BaseUri uri = new BaseUri(URI.create("foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void ignoresLeadingSlash() {

		BaseUri uri = new BaseUri(URI.create("/foo"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
	}

	/**
	 * @see DATAREST-276
	 */
	@Test
	public void matchesAbsoluteBaseUriOnOverlap() {

		BaseUri uri = new BaseUri(URI.create("http://localhost:8080/foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/people"), is("/people"));
		assertThat(uri.getRepositoryLookupPath("/foo/people/"), is("/people"));
	}

	/**
	 * @see DATAREST-300
	 */
	@Test
	public void stripsTemplateVariablesFromPath() {

		BaseUri uri = new BaseUri(URI.create("foo"));
		assertThat(uri.getRepositoryLookupPath("/foo/bar{?projection}"), is("/bar"));
	}

	/**
	 * @see DATAREST-318
	 */
	@Test
	public void stripsTemplateVariablesFromRequest() {

		BaseUri uri = new BaseUri(URI.create("foo"));

		ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/foo/bar{?projection}"));
		assertThat(uri.getRepositoryLookupPath(request), is("/bar"));
	}
}
