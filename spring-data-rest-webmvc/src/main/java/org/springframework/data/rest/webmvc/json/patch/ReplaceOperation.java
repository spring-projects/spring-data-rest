/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.data.rest.webmvc.json.patch.SpelPath.UntypedSpelPath;

/**
 * Operation that replaces the value at the given path with a new value.
 *
 * @author Craig Walls
 * @author Oliver Gierke
 */
class ReplaceOperation extends PatchOperation {

	/**
	 * Constructs the replace operation
	 *
	 * @param path The path whose value is to be replaced. (e.g., '/foo/bar/4')
	 * @param value The value that will replace the current path value.
	 */
	private ReplaceOperation(UntypedSpelPath path, Object value) {
		super("replace", path, value);
	}

	public static ReplaceOperationBuilder valueAt(String path) {
		return new ReplaceOperationBuilder(path);
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static class ReplaceOperationBuilder {

		private final String path;

		public ReplaceOperation with(Object value) {
			return new ReplaceOperation(SpelPath.untyped(path), value);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.PatchOperation#perform(java.lang.Object, java.lang.Class)
	 */
	@Override
	void perform(Object target, Class<?> type) {
		path.bindTo(type).setValue(target, evaluateValueFromTarget(target, type));
	}
}
