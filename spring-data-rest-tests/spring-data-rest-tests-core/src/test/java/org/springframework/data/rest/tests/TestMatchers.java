/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.rest.tests;

import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link Matcher} implementations useful in tests
 *
 * @author Oliver Drotbohm
 */
public class TestMatchers {

	/**
	 * Asserts that a {@link String} consists of a given number of lines.
	 *
	 * @param number
	 * @return
	 */
	public static Matcher<String> hasNumberOfLines(int number) {

		return new TypeSafeMatcher<String>(String.class) {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains " + number + " lines");
			}

			@Override
			protected void describeMismatchSafely(String item, Description mismatchDescription) {

				mismatchDescription.appendText(item) //
						.appendText(" contains ") //
						.appendValue(numberOfLines(item)) //
						.appendText(" lines");
			}

			@Override
			protected boolean matchesSafely(String item) {
				return numberOfLines(item) == number;
			}

			private long numberOfLines(String source) {

				return Arrays.stream(source.split("\n")) //
						.filter(StringUtils::hasText) //
						.count();
			}
		};
	}
}
