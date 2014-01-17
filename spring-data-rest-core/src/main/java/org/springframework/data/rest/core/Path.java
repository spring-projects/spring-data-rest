/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.core;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Simple value object to build up (URI) paths. Allows easy concatenation of {@link String}s and will take care of
 * removal of whitespace and reducing slashes to single ones.
 * 
 * @author Oliver Gierke
 */
public class Path {

	private static final String SLASH = "/";
	private static final String MATCH_PATTERN = "/?%s";

	private final String path;

	/**
	 * Creates a new {@link Path} from the given {@link String}.
	 * 
	 * @param path
	 */
	public Path(String path) {
		this(path, true);
	}

	/**
	 * Creates a new {@link Path} from the given string and potentially bypasses the cleanup.
	 * 
	 * @param path
	 * @param cleanUp
	 */
	private Path(String path, boolean cleanUp) {
		this.path = cleanUp ? cleanUp(path) : path;
	}

	/**
	 * Returns whether the given reference String matches the current {@link Path}.
	 * 
	 * @param reference
	 * @return
	 */
	public boolean matches(String reference) {
		return reference == null ? false : this.path.matches(String.format(MATCH_PATTERN, Pattern.quote(reference)));
	}

	/**
	 * Appends the given {@link String} to the current {@link Path}.
	 * 
	 * @param path
	 * @return
	 */
	public Path slash(String path) {
		return new Path(this.path + cleanUp(path), false);
	}

	public Path slash(Path path) {
		return slash(path.toString());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return path.hashCode();
	}

	private static String cleanUp(String path) {

		if (!StringUtils.hasText(path)) {
			return "";
		}

		String trimmed = path.trim().replaceAll(" ", "");

		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}

		trimmed = trimmed.substring(getFirstNoneSlashIndex(trimmed));

		return trimmed.contains("://") ? trimmed : SLASH + trimmed;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return path;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Path)) {
			return false;
		}

		Path that = (Path) obj;
		return this.path.equals(that.path);
	}

	private static int getFirstNoneSlashIndex(String input) {

		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) != '/') {
				return i;
			}
		}

		return input.length();
	}
}
