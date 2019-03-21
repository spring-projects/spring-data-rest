/*
 * Copyright 2013-2018 the original author or authors.
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

import org.junit.Test;

/**
 * Unit tests for {@link Path}.
 *
 * @author Oliver Gierke
 */
public class PathUnitTests {

	@Test
	public void combinesSimplePaths() {

		Path builder = new Path("foo").slash("bar");
		assertThat(builder.toString()).isEqualTo("/foo/bar");
	}

	@Test
	public void removesLeadingAndTrailingSlashes() {

		Path builder = new Path("foo/").slash("/bar").slash("//foobar///");
		assertThat(builder.toString()).isEqualTo("/foo/bar/foobar");
	}

	@Test
	public void removesWhitespace() {

		Path builder = new Path("foo/ ").slash("/ b a r").slash("  //foobar///   ");
		assertThat(builder.toString()).isEqualTo("/foo/bar/foobar");
	}

	@Test
	public void matchesWithLeadingSlash() {
		assertThat(new Path("/foobar").matches("/foobar")).isTrue();
	}

	@Test
	public void matchesWithoutLeadingSlash() {
		assertThat(new Path("/foobar").matches("foobar")).isTrue();
	}

	@Test
	public void doesNotMatchIfDifferent() {
		assertThat(new Path("/foobar").matches("barfoo")).isFalse();
	}

	@Test
	public void doesNotPrefixAbsoluteUris() {
		assertThat(new Path("http://localhost").toString()).isEqualTo("http://localhost");
	}

	@Test // DATAREST-222
	public void doesNotMatchIfReferenceContainsReservedCharacters() {
		assertThat(new Path("/foobar").matches("barfoo{?foo}")).isFalse();
	}

	@Test // DATAREST-222
	public void doesNotMatchNullReference() {
		assertThat(new Path("/foobar").matches(null)).isFalse();
	}
}
