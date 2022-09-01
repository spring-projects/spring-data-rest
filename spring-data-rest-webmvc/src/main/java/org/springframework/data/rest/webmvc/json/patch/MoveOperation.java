/*
 * Copyright 2014-2022 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * <p>
 * Operation that moves a value from the given "from" path to the given "path". Will throw a {@link PatchException} if
 * either path is invalid or if the from path is non-nullable.
 * </p>
 * <p>
 * NOTE: When dealing with lists, the move operation may effectively be a no-op. That's because the order of a list is
 * probably dictated by a database query that produced the list. Moving things around in the list will have no bearing
 * on the values of each item in the list. When the same list resource is retrieved again later, the order will again be
 * decided by the query, effectively undoing any previous move operation.
 * </p>
 *
 * @author Craig Walls
 * @author Oliver Gierke
 */
class MoveOperation extends PatchOperation {

	private final UntypedSpelPath from;

	/**
	 * Constructs the move operation.
	 *
	 * @param path The path to move the source value to. (e.g., '/foo/bar/4')
	 * @param from The source path from which a value will be moved. (e.g., '/foo/bar/5')
	 */
	private MoveOperation(UntypedSpelPath path, UntypedSpelPath from) {

		super("move", path);

		this.from = from;
	}

	static MoveOperationBuilder from(String from) {
		return new MoveOperationBuilder(from);
	}

	static class MoveOperationBuilder {

		private final String from;

		private MoveOperationBuilder(String from) {

			Assert.hasText(from, "From must not be null or empty");

			this.from = from;
		}

		public MoveOperation to(String to) {
			return new MoveOperation(SpelPath.untyped(to), SpelPath.untyped(from));
		}
	}

	@Override
	void perform(Object target, Class<?> type, BindContext context) {
		path.bindForWrite(type, context).moveFrom(from, target, context);
	}
}
