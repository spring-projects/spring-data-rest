/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.springframework.data.rest.webmvc.json.patch.PathToSpEL.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelEvaluationException;

/**
 * Abstract base class representing and providing support methods for patch operations.
 * 
 * @author Craig Walls
 * @author Mathias Düsterhöft
 * @author Oliver Gierke
 */
public abstract class PatchOperation {

	private static final String INVALID_PATH_REFERENCE = "Invalid path reference %s on type %s (from source %s)!";

	protected final String op;
	protected final String path;
	protected final Object value;
	protected final Expression spelExpression;

	/**
	 * Constructs the operation.
	 * 
	 * @param op the operation name. (e.g., 'move')
	 * @param path the path to perform the operation on. (e.g., '/1/description')
	 */
	public PatchOperation(String op, String path) {
		this(op, path, null);
	}

	/**
	 * Constructs the operation.
	 * 
	 * @param op the operation name. (e.g., 'move')
	 * @param path the path to perform the operation on. (e.g., '/1/description')
	 * @param value the value to apply in the operation. Could be an actual value or an implementation of
	 *          {@link LateObjectEvaluator}.
	 */
	public PatchOperation(String op, String path, Object value) {

		this.op = op;
		this.path = path;
		this.value = value;
		this.spelExpression = pathToExpression(path);
	}

	/**
	 * @return the operation name
	 */
	public String getOp() {
		return op;
	}

	/**
	 * @return the operation path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the operation's value (or {@link LateObjectEvaluator})
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Pops a value from the given path.
	 * 
	 * @param target the target from which to pop a value.
	 * @param removePath the path from which to pop a value. Must be a list.
	 * @return the value popped from the list
	 */
	protected Object popValueAtPath(Object target, String removePath) {

		Integer listIndex = targetListIndex(removePath);
		Expression expression = pathToExpression(removePath);
		Object value = expression.getValue(target);

		if (listIndex == null) {

			try {
				expression.setValue(target, null);
				return value;
			} catch (SpelEvaluationException o_O) {
				throw new PatchException("Path '" + removePath + "' is not nullable.", o_O);
			}

		} else {

			Expression parentExpression = pathToParentExpression(removePath);
			List<?> list = (List<?>) parentExpression.getValue(target);
			list.remove(listIndex >= 0 ? listIndex.intValue() : list.size() - 1);
			return value;
		}
	}

	/**
	 * Adds a value to the operation's path. If the path references a list index, the value is added to the list at the
	 * given index. If the path references an object property, the property is set to the value.
	 * 
	 * @param target The target object.
	 * @param value The value to add.
	 */
	@SuppressWarnings({ "unchecked", "null" })
	protected void addValue(Object target, Object value) {

		Expression parentExpression = pathToParentExpression(path);
		Object parent = parentExpression != null ? parentExpression.getValue(target) : null;
		Integer listIndex = targetListIndex(path);

		if (parent == null || !(parent instanceof List) || listIndex == null) {

			TypeDescriptor descriptor = parentExpression.getValueTypeDescriptor(target);

			// Set as new collection if necessary
			if (descriptor.isCollection() && !Collection.class.isInstance(value)) {

				Collection<Object> collection = CollectionFactory.createCollection(descriptor.getType(), 1);
				collection.add(value);

				parentExpression.setValue(target, collection);

			} else {
				spelExpression.setValue(target, value);
			}

		} else {

			List<Object> list = (List<Object>) parentExpression.getValue(target);
			list.add(listIndex >= 0 ? listIndex.intValue() : list.size(), value);
		}
	}

	/**
	 * Sets a value to the operation's path.
	 * 
	 * @param target The target object.
	 * @param value The value to set.
	 */
	protected void setValueOnTarget(Object target, Object value) {
		spelExpression.setValue(target, value);
	}

	/**
	 * Retrieves a value from the operation's path.
	 * 
	 * @param target the target object.
	 * @return the value at the path on the given target object.
	 */
	protected Object getValueFromTarget(Object target) {

		try {
			return spelExpression.getValue(target);
		} catch (ExpressionException e) {
			throw new PatchException("Unable to get value from target", e);
		}
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
	protected <T> Object evaluateValueFromTarget(Object targetObject, Class<T> entityType) {

		verifyPath(entityType);

		return evaluate(spelExpression.getValueType(targetObject));
	}

	protected final <T> Object evaluate(Class<T> type) {
		return value instanceof LateObjectEvaluator ? ((LateObjectEvaluator) value).evaluate(type) : value;
	}

	/**
	 * Verifies that the current path is available on the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return the {@link PropertyPath} representing the path. Empty if the path only consists of index lookups or append
	 *         characters.
	 */
	protected final Optional<PropertyPath> verifyPath(Class<?> type) {

		String pathSource = Arrays.stream(path.split("/"))//
				.filter(it -> !it.matches("\\d")) // no digits
				.filter(it -> !it.equals("-")) // no "last element"s
				.filter(it -> !it.isEmpty()) //
				.collect(Collectors.joining("."));

		if (pathSource.isEmpty()) {
			return Optional.empty();
		}

		try {
			return Optional.of(PropertyPath.from(pathSource, type));
		} catch (PropertyReferenceException o_O) {
			throw new PatchException(String.format(INVALID_PATH_REFERENCE, pathSource, type, path), o_O);
		}
	}

	/**
	 * Perform the operation.
	 * 
	 * @param target the target of the operation.
	 */
	abstract <T> void perform(Object target, Class<T> type);

	private Integer targetListIndex(String path) {

		String[] pathNodes = path.split("\\/");
		String lastNode = pathNodes[pathNodes.length - 1];

		if (APPEND_CHARACTERS.contains(lastNode)) {
			return -1;
		}

		try {
			return Integer.parseInt(lastNode);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
