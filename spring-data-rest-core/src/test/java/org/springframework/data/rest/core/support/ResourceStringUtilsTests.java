/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.rest.core.support;

import static org.assertj.core.api.Assertions.*;

import lombok.Value;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Ensures proper detection and removal of leading slash in strings.
 *
 * @author Florent Biville
 */
class ResourceStringUtilsTests {

	@TestFactory
	Stream<DynamicTest> shouldDetectTextPresence() {

		return DynamicTest.stream(fixtures(), Fixture::getName, it -> {
			assertThat(ResourceStringUtils.hasTextExceptSlash(it.getActual())).isEqualTo(it.hasText);
		});
	}

	@TestFactory
	Stream<DynamicTest> shouldRemoveLeadingSlashIfAny() {

		return DynamicTest.stream(fixtures(), Fixture::getName, it -> {
			assertThat(ResourceStringUtils.removeLeadingSlash(it.getActual())).isEqualTo(it.getExpected());
		});
	}

	static Stream<Fixture> fixtures() {

		return Stream.of(
				Fixture.of("empty string has no text and should remain empty", "", "", false),
				Fixture.of("blank string has no text and should remain as is", "  ", "  ", false),
				Fixture.of("string made of only a leading slash has no text and should be returned empty", "/", "", false),
				Fixture.of("blank string with only slashes has no text and should be returned as is", "   /   ", "   /   ",
						false),
				Fixture.of("normal string has text and should be returned as such", "hello", "hello", true),
				Fixture.of("normal string with leading slash has text and should be returned without leading slash", "/hello",
						"hello", true));
	}

	@Value(staticConstructor = "of")
	static class Fixture {

		String name, actual, expected;
		boolean hasText;
	}
}
