/*
 * Copyright 2017-2019 the original author or authors.
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * Value object to represent a SpEL-backed patch path.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class SpelPath {

	private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();
	private static final List<String> APPEND_CHARACTERS = Arrays.asList("-", "~");
	private static final Map<String, SpelPath> PATHS = new ConcurrentReferenceHashMap<String, SpelPath>(32);

	protected final @Getter String path;
	protected final Expression expression;

	private SpelPath(String path) {

		Assert.notNull(path, "Path must not be null!");

		this.path = path;
		this.expression = SPEL_EXPRESSION_PARSER.parseExpression(pathToSpEL(path));
	}

	/**
	 * Returns a {@link SpelPath} for the given source.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public static SpelPath of(String source) {

		SpelPath path = PATHS.get(source);

		if (path == null) {

			path = new SpelPath(source);
			PATHS.put(source, path);
		}

		return path;
	}

	/**
	 * Returns a {@link TypedSpelPath} binding the expression to the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public TypedSpelPath bindTo(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return TypedSpelPath.of(this, type);
	}

	/**
	 * Returns the leaf type of the underlying expression or the given type
	 * 
	 * @param type
	 * @return
	 */
	public Class<?> getLeafType(Class<?> type) {

		PropertyPath verifiedPath = TypedSpelPath.verifyPath(path, type);

		return verifiedPath == null ? type : verifiedPath.getType();
	}

	/**
	 * Returns whether the current path represents an append path, i.e. is supposed to append to collection.
	 * 
	 * @return
	 */
	public boolean isAppend() {
		return path.endsWith("-");
	}

	private SpelPath getParent() {
		return SpelPath.of(path.substring(0, path.lastIndexOf('/')));
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

		if (!(SpelPath.class.isInstance(obj))) {
			return false;
		}

		SpelPath that = (SpelPath) obj;

		return this.path.equals(that.path);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return path.hashCode();
	}

	private static String pathToSpEL(String path) {
		return pathNodesToSpEL(path.split("\\/"));
	}

	private static String pathNodesToSpEL(String[] pathNodes) {

		StringBuilder spelBuilder = new StringBuilder();

		for (int i = 0; i < pathNodes.length; i++) {

			String pathNode = pathNodes[i];

			if (pathNode.length() == 0) {
				continue;
			}

			if (APPEND_CHARACTERS.contains(pathNode)) {

				if (spelBuilder.length() > 0) {
					spelBuilder.append(".");
				}

				spelBuilder.append("$[true]");
				continue;
			}

			try {

				int index = Integer.parseInt(pathNode);
				spelBuilder.append('[').append(index).append(']');

			} catch (NumberFormatException e) {

				if (spelBuilder.length() > 0) {
					spelBuilder.append('.');
				}

				spelBuilder.append(pathNode);
			}
		}

		String spel = spelBuilder.toString();

		if (spel.length() == 0) {
			spel = "#this";
		}

		return spel;
	}

	/**
	 * A {@link SpelPath} that has typing information tied to it.
	 *
	 * @author Oliver Gierke
	 */
	@EqualsAndHashCode(callSuper = true)
	static class TypedSpelPath extends SpelPath {

		private static final String INVALID_PATH_REFERENCE = "Invalid path reference %s on type %s (from source %s)!";
		private static final String INVALID_COLLECTION_INDEX = "Invalid collection index %s for collection of size %s. Use '…/-' or the collection's actual size as index to append to it!";
		private static final Map<CacheKey, TypedSpelPath> TYPED_PATHS = new ConcurrentReferenceHashMap<CacheKey, TypedSpelPath>(
				32);
		private static final EvaluationContext CONTEXT = SimpleEvaluationContext.forReadWriteDataBinding().build();

		private final Class<?> type;

		@Value(staticConstructor = "of")
		private static class CacheKey {
			Class<?> type;
			SpelPath path;
		}

		private TypedSpelPath(SpelPath path, Class<?> type) {

			super(path.path, path.expression);

			verifyPath(path.path, type);

			this.type = type;
		}

		/**
		 * Returns the {@link TypedSpelPath} for the given {@link SpelPath} and type.
		 * 
		 * @param path must not be {@literal null}.
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public static TypedSpelPath of(SpelPath path, Class<?> type) {

			Assert.notNull(path, "Path must not be null!");
			Assert.notNull(type, "Type must not be null!");

			CacheKey key = CacheKey.of(type, path);
			TypedSpelPath result = TYPED_PATHS.get(key);

			if (result == null) {

				result = new TypedSpelPath(path, type);
				TYPED_PATHS.put(key, result);
			}

			return result;
		}

		/**
		 * Returns the value pointed to by the current path with the given target object.
		 * 
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@SuppressWarnings("unchecked")
		public <T> T getValue(Object target) {

			Assert.notNull(target, "Target must not be null!");

			try {
				return (T) expression.getValue(CONTEXT, target);
			} catch (ExpressionException o_O) {
				throw new PatchException("Unable to get value from target", o_O);
			}
		}

		/**
		 * Sets the given value on the given target object.
		 * 
		 * @param target must not be {@literal null}.
		 * @param value can be {@literal null}.
		 */
		public void setValue(Object target, Object value) {

			Assert.notNull(target, "Target must not be null!");

			expression.setValue(CONTEXT, target, value);
		}

		/**
		 * Returns the type of the expression target based on the given root.
		 * 
		 * @param root must not be {@literal null}.
		 * @return
		 */
		public Class<?> getType(Object root) {

			Assert.notNull(root, "Root object must not be null!");

			try {

				return expression.getValueType(CONTEXT, root);

			} catch (SpelEvaluationException o_O) {

				if (!SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS.equals(o_O.getMessageCode())) {
					throw o_O;
				}

				Object collectionOrArray = getParent().getValue(root);

				if (Collection.class.isInstance(collectionOrArray)) {
					return CollectionUtils.findCommonElementType(Collection.class.cast(collectionOrArray));
				}
			}

			throw new IllegalArgumentException(String.format("Cannot obtain type for path %s on %s!", path, root));
		}

		/**
		 * Copies the value pointed to by the given path within the given source object to the current expression target.
		 * 
		 * @param path the {@link SpelPath} to look the value up from, must not be {@literal null}.
		 * @param source the source object to look the value up from, must not be {@literal null}.
		 * @return
		 */

		public void copyFrom(SpelPath path, Object source) {

			Assert.notNull(path, "Source path must not be null!");
			Assert.notNull(source, "Source value must not be null!");

			addValue(source, path.bindTo(type).getValue(source));
		}

		/**
		 * Moves the value pointed to by the given path within the given source object to the current expression target and
		 * removes the value from its original position.
		 * 
		 * @param path the {@link SpelPath} to look the value up from, must not be {@literal null}.
		 * @param source the source object to look the value up from, must not be {@literal null}.
		 * @return
		 */
		public void moveFrom(SpelPath path, Object source) {

			Assert.notNull(path, "Source path must not be null!");
			Assert.notNull(source, "Source value must not be null!");

			addValue(source, path.bindTo(type).removeFrom(source));
		}

		/**
		 * Removes the value pointed to by the current path within the given target.
		 * 
		 * @param target must not be {@literal null}.
		 * @return the original value that was just removed.
		 */
		public Object removeFrom(Object target) {

			Assert.notNull(target, "Target must not be null!");

			Integer listIndex = getTargetListIndex();
			Object value = getValue(target);

			if (listIndex == null) {

				try {
					setValue(target, null);
					return value;
				} catch (SpelEvaluationException o_O) {
					throw new PatchException("Path '" + path + "' is not nullable.", o_O);
				}

			} else {

				List<?> list = getParent().getValue(target);
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
		public void addValue(Object target, Object value) {

			TypedSpelPath parentPath = getParent();
			Object parent = parentPath.getValue(target);

			Integer listIndex = getTargetListIndex();

			if (parent == null || !(parent instanceof List) || listIndex == null) {

				TypeDescriptor descriptor = parentPath.getTypeDescriptor(target);

				// Set as new collection if necessary
				if (descriptor.isCollection() && !Collection.class.isInstance(value)) {

					Collection<Object> collection = CollectionFactory.createCollection(descriptor.getType(), 1);
					collection.add(value);

					parentPath.setValue(target, collection);

				} else {
					setValue(target, value);
				}

			} else {

				List<Object> list = parentPath.getValue(target);

				if (listIndex > list.size()) {
					throw new PatchException(String.format(INVALID_COLLECTION_INDEX, listIndex, list.size()));
				}

				list.add(listIndex >= 0 ? listIndex.intValue() : list.size(), value);
			}
		}

		private TypedSpelPath getParent() {
			return TypedSpelPath.of(super.getParent(), type);
		}

		private TypeDescriptor getTypeDescriptor(Object target) {
			return expression.getValueTypeDescriptor(CONTEXT, target);
		}

		private Integer getTargetListIndex() {

			String lastNode = path.substring(path.lastIndexOf('/') + 1);

			if (APPEND_CHARACTERS.contains(lastNode)) {
				return -1;
			}

			try {
				return Integer.parseInt(lastNode);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		/**
		 * Verifies that the given path exists on the given type. Skips collection index parts and append characters.
		 * 
		 * @param path must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 * @return the {@link PropertyPath} if the path could be resolved or {@link Optional#empty()} in case an empty path
		 *         is given.
		 */
		private static PropertyPath verifyPath(String path, Class<?> type) {

			Assert.notNull(path, "Path must not be null!");
			Assert.notNull(type, "Type must not be null!");

			String[] parts = path.split("/");
			List<String> result = new ArrayList<String>();

			for (String part : parts) {

				if (part.matches("\\d+") || APPEND_CHARACTERS.contains(part) || part.isEmpty()) {
					continue;
				}

				result.add(part);
			}

			String pathSource = StringUtils.collectionToDelimitedString(result, ".");

			if (pathSource.isEmpty()) {
				return null;
			}

			try {
				return PropertyPath.from(pathSource, type);
			} catch (PropertyReferenceException o_O) {
				throw new PatchException(String.format(INVALID_PATH_REFERENCE, pathSource, type, path), o_O);
			}
		}
	}
}
