/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BaseUri}.
 *
 * @author Oliver Gierke
 */
class BaseUriUnitTests {

	@Test // DATAREST-276
	void doesNotMatchNonOverlap() {

		assertThat(new BaseUri(URI.create("foo")).getRepositoryLookupPath("/bar")).isNull();
		assertThat(new BaseUri(URI.create("http://localhost:8080/foo/")).getRepositoryLookupPath("/bar")).isNull();
	}

	@Test // DATAREST-276
	void matchesSimpleBaseUri() {

		BaseUri uri = new BaseUri(URI.create("foo"));

		assertThat(uri.getRepositoryLookupPath("/foo")).isEmpty();
	}

	@Test // DATAREST-276
	void ignoresTrailingSlash() {

		BaseUri uri = new BaseUri(URI.create("foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo")).isEmpty();
		assertThat(uri.getRepositoryLookupPath("/foo/")).isEmpty();
	}

	@Test // DATAREST-276
	void ignoresLeadingSlash() {

		BaseUri uri = new BaseUri(URI.create("/foo"));

		assertThat(uri.getRepositoryLookupPath("/foo")).isEmpty();
		assertThat(uri.getRepositoryLookupPath("/foo/")).isEmpty();
	}

	@Test // DATAREST-276
	void matchesAbsoluteBaseUriOnOverlap() {

		BaseUri uri = new BaseUri(URI.create("http://localhost:8080/foo/"));

		assertThat(uri.getRepositoryLookupPath("/foo")).isEmpty();
		assertThat(uri.getRepositoryLookupPath("/foo/")).isEmpty();
		assertThat(uri.getRepositoryLookupPath("/foo/people")).isEqualTo("/people");
		assertThat(uri.getRepositoryLookupPath("/foo/people/")).isEqualTo("/people");
	}

	@Test // DATAREST-674, SPR-13455
	void repositoryLookupPathHandlesDoubleSlashes() {
		assertThat(BaseUri.NONE.getRepositoryLookupPath("/books//1")).isEqualTo("/books/1");
	}
}
