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
package org.springframework.data.rest.webmvc.json.patch;

import org.springframework.data.rest.webmvc.json.patch.SpelPath.UntypedSpelPath;

/**
 * Operation that removes the value at the given path. Will throw a {@link PatchException} if the given path isn't valid
 * or if the path is non-nullable.
 *
 * @author Craig Walls
 * @author Oliver Gierke
 */
class RemoveOperation extends PatchOperation {

	/**
	 * Constructs the remove operation
	 *
	 * @param path The path of the value to be removed. (e.g., '/foo/bar/4')
	 */
	private RemoveOperation(UntypedSpelPath path) {
		super("remove", path);
	}

	public static RemoveOperation valueAt(String path) {
		return new RemoveOperation(SpelPath.untyped(path));
	}

	@Override
	void perform(Object target, Class<?> type, BindContext context) {
		path.bindForWrite(type, context).removeFrom(target);
	}
}
