/*
 * Copyright 2013 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.data.rest.core.support.ResourceStringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Ensures proper detection and removal of leading slash in strings.
 * 
 * @author Florent Biville
 */
@RunWith(Parameterized.class)
public class ResourceStringUtilsTests {

	final String actual;
	final String expected;
	final boolean hasText;

	public ResourceStringUtilsTests(String testDescription, String actual, String expected, boolean hasText) {

		this.actual = actual;
		this.expected = expected;
		this.hasText = hasText;
	}

	@Parameters(name = "{0}")
	public static Collection<?> parameters() {
		return Arrays.asList(new Object[][] {
				{ "empty string has no text and should remain empty", "", "", false },
				{ "blank string has no text and should remain as is", "  ", "  ", false },
				{ "string made of only a leading slash has no text and should be returned empty", "/", "", false },
				{ "blank string with only slashes has no text and should be returned as is", "   /   ", "   /   ", false },
				{ "normal string has text and should be returned as such", "hello", "hello", true },
				{ "normal string with leading slash has text and should be returned without leading slash", "/hello", "hello",
						true }, });
	}

	@Test
	public void shouldDetectTextPresence() {
		assertThat(ResourceStringUtils.hasTextExceptSlash(actual), is(hasText));
	}

	@Test
	public void shouldRemoveLeadingSlashIfAny() {
		assertThat(ResourceStringUtils.removeLeadingSlash(actual), is(expected));
	}
}
