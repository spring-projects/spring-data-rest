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
 * Operation to add a new value to the given "path". Will throw a {@link PatchException} if the path is invalid or if
 * the given value is not assignable to the given path.
 *
 * @author Craig Walls
 * @author Oliver Gierke
 */
class AddOperation extends PatchOperation {

	/**
	 * Constructs the add operation
	 *
	 * @param path The path where the value will be added. (e.g., '/foo/bar/4')
	 * @param value The value to add.
	 */
	private AddOperation(UntypedSpelPath path, Object value) {
		super("add", path, value);
	}

	public static AddOperation of(String path, Object value) {
		return new AddOperation(SpelPath.untyped(path), value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.PatchOperation#perform(java.lang.Object, java.lang.Class, org.springframework.data.rest.webmvc.json.PropertyFilter)
	 */
	@Override
	void perform(Object target, Class<?> type, BindContext context) {

		path.bindForWrite(type, context).addValue(target, evaluateValueFromTarget(target, type, context));
	}

	@Override
	protected Object evaluateValueFromTarget(Object targetObject, Class<?> entityType, BindContext context) {

		if (!path.isAppend()) {
			return super.evaluateValueFromTarget(targetObject, entityType, context);
		}

		return evaluate(path.bindForWrite(entityType, context).getLeafType());
	}
}
