/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Operation to copy a value from the given "from" path to the given "path". Will throw a {@link PatchException} if
 * either path is invalid or if the object at the from path is not assignable to the given path.
 * </p>
 * <p>
 * NOTE: When dealing with lists, the copy operation may yield undesirable results. If a list is produced from a
 * database query, it's likely that the list contains items with a unique ID. Copying an item in the list will result
 * with a list that has a duplicate object, with the same ID. The best case post-patch scenario is that each copy of the
 * item will be saved, unchanged, to the database and later queries for the list will not include the duplicate. The
 * worst case post-patch scenario is that a following operation changes some properties of the copy, but does not change
 * the ID. When saved, both the original and the copy will be saved, but the last one saved will overwrite the first.
 * Effectively only one copy will survive post-save.
 * </p>
 * <p>
 * In light of this, it's probably a good idea to perform a "replace" after a "copy" to set the ID property (which may
 * or may not be "id").
 * </p>
 * 
 * @author Craig Walls
 * @author Oliver Gierke
 */
class CopyOperation extends PatchOperation {

	private final SpelPath from;

	/**
	 * Constructs the copy operation
	 * 
	 * @param path The path to copy the source value to. (e.g., '/foo/bar/4')
	 * @param from The source path from which a value will be copied. (e.g., '/foo/bar/5')
	 */
	public CopyOperation(SpelPath path, SpelPath from) {

		super("copy", path);

		this.from = from;
	}

	public static CopyOperationBuilder from(String from) {
		return new CopyOperationBuilder(from);
	}

	@RequiredArgsConstructor
	static class CopyOperationBuilder {

		private final String from;

		CopyOperation to(String to) {
			return new CopyOperation(SpelPath.of(to), SpelPath.of(from));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.PatchOperation#perform(java.lang.Object, java.lang.Class)
	 */
	@Override
	void perform(Object target, Class<?> type) {
		path.bindTo(type).copyFrom(from, target);
	}
}
