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

/**
 * Helper methods aiming at handling String representations of resources.
 * 
 * @author Florent Biville
 */
public class ResourceStringUtils {

	/**
	 * Checks whether the given input contains actual text (slash excluded). This is a specializing variant of
	 * {@link org.springframework.util.StringUtils )}#hasText.
	 * 
	 * @param input
	 */
	public static boolean hasTextExceptSlash(CharSequence input) {

		int strLen = input.length();

		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(input.charAt(i)) && !startsWithSlash(input.charAt(i))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a string without the leading slash, if any.
	 * 
	 * @param path
	 */
	public static String removeLeadingSlash(String path) {

		if (path.length() == 0) {
			return path;
		}

		boolean hasLeadingSlash = startsWithSlash(path);

		if (path.length() == 1) {
			return hasLeadingSlash ? "" : path;
		}

		return hasLeadingSlash ? path.substring(1) : path;
	}

	private static boolean startsWithSlash(String path) {
		return path.charAt(0) == '/';
	}

	private static boolean startsWithSlash(char c) {
		return c == '/';
	}
}
