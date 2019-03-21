/*
 * Copyright 2014-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Abstract base class representing and providing support methods for patch operations.
 *
 * @author Craig Walls
 * @author Mathias Düsterhöft
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public abstract class PatchOperation {

	protected final @NonNull String op;
	protected final @NonNull SpelPath path;
	protected final Object value;

	/**
	 * Constructs the operation.
	 *
	 * @param op the operation name. (e.g., 'move')
	 * @param path the path to perform the operation on. (e.g., '/1/description')
	 */
	public PatchOperation(String op, SpelPath path) {
		this(op, path, null);
	}

	/**
	 * Performs late-value evaluation on the operation value if the value is a {@link LateObjectEvaluator}.
	 *
	 * @param targetObject the target object, used as assistance in determining the evaluated object's type.
	 * @param entityType the entityType
	 * @param <T> the entity type
	 * @return the result of late-value evaluation if the value is a {@link LateObjectEvaluator}; the value itself
	 *         otherwise.
	 */
	protected Object evaluateValueFromTarget(Object targetObject, Class<?> entityType) {
		return evaluate(path.bindTo(entityType).getType(targetObject));
	}

	protected final Object evaluate(Class<?> type) {
		return value instanceof LateObjectEvaluator ? ((LateObjectEvaluator) value).evaluate(type) : value;
	}

	/**
	 * Perform the operation in the given target object.
	 *
	 * @param target the target of the operation, must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 */
	abstract void perform(Object target, Class<?> type);
}
