/*
 * Copyright 2013-2024 the original author or authors.
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
package org.springframework.data.rest.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Path}.
 *
 * @author Oliver Gierke
 */
class PathUnitTests {

	@Test
	void combinesSimplePaths() {

		Path builder = new Path("foo").slash("bar");
		assertThat(builder.toString()).isEqualTo("/foo/bar");
	}

	@Test
	void removesLeadingAndTrailingSlashes() {

		Path builder = new Path("foo/").slash("/bar").slash("//foobar///");
		assertThat(builder.toString()).isEqualTo("/foo/bar/foobar");
	}

	@Test
	void removesWhitespace() {

		Path builder = new Path("foo/ ").slash("/ b a r").slash("  //foobar///   ");
		assertThat(builder.toString()).isEqualTo("/foo/bar/foobar");
	}

	@Test
	void matchesWithLeadingSlash() {
		assertThat(new Path("/foobar").matches("/foobar")).isTrue();
	}

	@Test
	void matchesWithoutLeadingSlash() {
		assertThat(new Path("/foobar").matches("foobar")).isTrue();
	}

	@Test
	void doesNotMatchIfDifferent() {
		assertThat(new Path("/foobar").matches("barfoo")).isFalse();
	}

	@Test
	void doesNotPrefixAbsoluteUris() {
		assertThat(new Path("http://localhost").toString()).isEqualTo("http://localhost");
	}

	@Test // DATAREST-222
	void doesNotMatchIfReferenceContainsReservedCharacters() {
		assertThat(new Path("/foobar").matches("barfoo{?foo}")).isFalse();
	}

	@Test // DATAREST-222
	void doesNotMatchNullReference() {
		assertThat(new Path("/foobar").matches(null)).isFalse();
	}
}
