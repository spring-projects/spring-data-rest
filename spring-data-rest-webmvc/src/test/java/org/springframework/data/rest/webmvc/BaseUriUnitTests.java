/*
 * Copyright 2014-2019 the original author or authors.
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

/**
 * Unit tests for {@link BaseUri}.
 * 
 * @author Oliver Gierke
 */
public class BaseUriUnitTests {

	@Test // DATAREST-276
	public void doesNotMatchNonOverlap() {

		assertThat(new BaseUri(URI.create("foo")).getRepositoryLookupPath("/bar"), is(nullValue()));
		assertThat(new BaseUri(URI.create("http://localhost:8080/foo/")).getRepositoryLookupPath("/bar"), is(nullValue()));
	}

	@Test // DATAREST-276
	public void matchesSimpleBaseUri() {

		BaseUri uri = new BaseUri(URI.create("foo"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
	}

	@Test // DATAREST-276
	public void ignoresTrailingSlash() {

		BaseUri uri = new BaseUri(URI.create("foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
	}

	@Test // DATAREST-276
	public void ignoresLeadingSlash() {

		BaseUri uri = new BaseUri(URI.create("/foo"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
	}

	@Test // DATAREST-276
	public void matchesAbsoluteBaseUriOnOverlap() {

		BaseUri uri = new BaseUri(URI.create("http://localhost:8080/foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/"), isEmptyString());
		assertThat(uri.getRepositoryLookupPath("/foo/people"), is("/people"));
		assertThat(uri.getRepositoryLookupPath("/foo/people/"), is("/people"));
	}

	@Test // DATAREST-674, SPR-13455
	public void repositoryLookupPathHandlesDoubleSlashes() {
		assertThat(BaseUri.NONE.getRepositoryLookupPath("/books//1"), is("/books/1"));
	}
}
