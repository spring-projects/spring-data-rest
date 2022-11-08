/*
 * Copyright 2017-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Value object to represent a SpEL-backed patch path.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Greg Turnquist
 */
class SpelPath {

	private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();
	private static final String APPEND_CHARACTER = "-";
	private static final Map<String, UntypedSpelPath> UNTYPED_PATHS = new ConcurrentReferenceHashMap<>(32);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	protected final String path;

	private SpelPath(String path) {

		Assert.notNull(path, "Path must not be null");

		this.path = path;
	}

	/**
	 * Returns a {@link UntypedSpelPath} for the given source.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public static UntypedSpelPath untyped(String source) {
		return UNTYPED_PATHS.computeIfAbsent(source, UntypedSpelPath::new);
	}

	/**
	 * Returns whether the current path represents an append path, i.e. is supposed to append to collection.
	 *
	 * @return
	 */
	public boolean isAppend() {
		return path.endsWith("-");
	}

	@Override
	public String toString() {
		return path;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!SpelPath.class.isInstance(obj)) {
			return false;
		}

		SpelPath that = (SpelPath) obj;

		return this.path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	static class UntypedSpelPath extends SpelPath {

		private static final Map<CacheKey, TypedSpelPath> READ_PATHS = new ConcurrentReferenceHashMap<>(256);
		private static final Map<CacheKey, TypedSpelPath> WRITE_PATHS = new ConcurrentReferenceHashMap<>(256);

		private UntypedSpelPath(String path) {
			super(path);
		}

		public ReadingOperations bindForRead(Class<?> type, BindContext context) {

			Assert.notNull(path, "Path must not be null");
			Assert.notNull(type, "Type must not be null");

			return READ_PATHS.computeIfAbsent(CacheKey.of(type, this, context),
					key -> {
						String mapped = new JsonPointerMapping(context).forRead(key.path.path, type);
						return new TypedSpelPath(mapped, key.type);
					});
		}

		/**
		 * Returns a {@link TypedSpelPath} binding the expression to the given type.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public WritingOperations bindForWrite(Class<?> type, BindContext context) {

			Assert.notNull(path, "Path must not be null");
			Assert.notNull(type, "Type must not be null");

			return WRITE_PATHS.computeIfAbsent(CacheKey.of(type, this, context),
					key -> {
						String mapped = new JsonPointerMapping(context).forWrite(key.path.path, type);
						return new TypedSpelPath(mapped, key.type);
					});
		}

		private static final class CacheKey {

			private final Class<?> type;
			private final UntypedSpelPath path;
			private final BindContext context;

			private CacheKey(Class<?> type, UntypedSpelPath path, BindContext context) {

				Assert.notNull(type, "Type must not be null");
				Assert.notNull(path, "UntypedSpelPath must not be null");

				this.type = type;
				this.path = path;
				this.context = context;
			}

			public static CacheKey of(Class<?> type, UntypedSpelPath path, BindContext context) {
				return new CacheKey(type, path, context);
			}

			@Override
			public boolean equals(@Nullable Object o) {

				if (o == this) {
					return true;
				}

				if (!(o instanceof CacheKey)) {
					return false;
				}

				CacheKey that = (CacheKey) o;

				return Objects.equals(type, that.type) //
						&& Objects.equals(path, that.path) //
						&& Objects.equals(context, that.context);
			}

			@Override
			public int hashCode() {
				return Objects.hash(type, path, context);
			}
		}
	}

	interface CommonOperations {

		String getExpressionString();
	}

	interface ReadingOperations extends CommonOperations {

		<T> T getValue(Object target);

		Class<?> getType(Object root);
	}

	interface WritingOperations extends CommonOperations {

		Class<?> getLeafType();

		Object removeFrom(Object target);

		void addValue(Object target, Object value);

		void setValue(Object target, @Nullable Object value);

		void copyFrom(UntypedSpelPath path, Object source, BindContext context);

		void moveFrom(UntypedSpelPath path, Object source, BindContext context);
	}

	/**
	 * A {@link SpelPath} that has typing information tied to it.
	 *
	 * @author Oliver Gierke
	 */
	static class TypedSpelPath extends SpelPath implements ReadingOperations, WritingOperations {

		private static final String INVALID_PATH_REFERENCE = "Invalid path reference %s on type %s";
		private static final String INVALID_COLLECTION_INDEX = "Invalid collection index %s for collection of size %s; Use '…/-' or the collection's actual size as index to append to it";
		private static final EvaluationContext CONTEXT = SimpleEvaluationContext.forReadWriteDataBinding().build();

		private final Expression expression;
		private final Class<?> type;

		private TypedSpelPath(String path, Class<?> type) {

			super(path);

			this.type = type;
			this.expression = toSpel(path, type);
		}

		/**
		 * Returns the value pointed to by the current path with the given target object.
		 *
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@SuppressWarnings("unchecked")
		public <T> T getValue(Object target) {

			Assert.notNull(target, "Target must not be null");

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
		public void setValue(Object target, @Nullable Object value) {

			Assert.notNull(target, "Target must not be null");

			expression.setValue(CONTEXT, target, value);
		}

		/**
		 * Returns the type of the leaf property of the path.
		 *
		 * @return will never be {@literal null}.
		 */
		public Class<?> getLeafType() {

			return TypedSpelPath.verifyPath(path, type) //
					.map(PropertyPath::getLeafProperty) //
					.<Class<?>> map(PropertyPath::getType) //
					.orElse(type);
		}

		public String getExpressionString() {
			return expression.getExpressionString();
		}

		/**
		 * Returns the type of the expression target based on the given root.
		 *
		 * @param root must not be {@literal null}.
		 * @return
		 */
		public Class<?> getType(Object root) {

			Assert.notNull(root, "Root object must not be null");

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

			throw new IllegalArgumentException(String.format("Cannot obtain type for path %s on %s", path, root));
		}

		/**
		 * Copies the value pointed to by the given path within the given source object to the current expression target.
		 *
		 * @param path the {@link SpelPath} to look the value up from, must not be {@literal null}.
		 * @param source the source object to look the value up from, must not be {@literal null}.
		 * @return
		 */
		public void copyFrom(UntypedSpelPath path, Object source, BindContext context) {

			Assert.notNull(path, "Source path must not be null");
			Assert.notNull(source, "Source value must not be null");

			addValue(source, path.bindForRead(type, context).getValue(source));
		}

		/**
		 * Moves the value pointed to by the given path within the given source object to the current expression target and
		 * removes the value from its original position.
		 *
		 * @param path the {@link SpelPath} to look the value up from, must not be {@literal null}.
		 * @param source the source object to look the value up from, must not be {@literal null}.
		 * @return
		 */
		public void moveFrom(UntypedSpelPath path, Object source, BindContext context) {

			Assert.notNull(path, "Source path must not be null");
			Assert.notNull(source, "Source value must not be null");

			// Verify we are allowed to read the source
			path.bindForRead(type, context);

			addValue(source, path.bindForWrite(type, context).removeFrom(source));
		}

		/**
		 * Removes the value pointed to by the current path within the given target.
		 *
		 * @param target must not be {@literal null}.
		 * @return the original value that was just removed.
		 */
		public Object removeFrom(Object target) {

			Assert.notNull(target, "Target must not be null");

			Integer listIndex = getTargetListIndex();
			Object value = getValue(target);

			if (listIndex == null) {

				try {
					setValue(target, null);
					return value;
				} catch (SpelEvaluationException o_O) {
					throw new PatchException("Path '" + path + "' is not nullable", o_O);
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

		@Override
		public String toString() {
			return String.format("%s on %s -> %s", path, type.getName(), getExpressionString());
		}

		private TypedSpelPath getParent() {
			return new TypedSpelPath(path.substring(0, path.lastIndexOf('/')), type);
		}

		private TypeDescriptor getTypeDescriptor(Object target) {
			return expression.getValueTypeDescriptor(CONTEXT, target);
		}

		private Integer getTargetListIndex() {

			String lastNode = path.substring(path.lastIndexOf('/') + 1);

			if (APPEND_CHARACTER.equals(lastNode)) {
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
		private static Optional<PropertyPath> verifyPath(String path, Class<?> type) {

			Assert.notNull(path, "Path must not be null");
			Assert.notNull(type, "Type must not be null");

			// Remove leading digits
			String segmentSource = path.replaceAll("^/\\d+", "");

			Stream<String> segments = Arrays.stream(segmentSource.split("/"))//
					.filter(it -> !it.equals("-")) // no "last element"s
					.filter(it -> !it.isEmpty());

			try {

				return segments.reduce(Optional.<SkippedPropertyPath> empty(), //
						(current, next) -> Optional.of(createOrSkip(current, next, type)), //
						(l, r) -> r) //
						.map(SkippedPropertyPath::getPath);

			} catch (PropertyReferenceException o_O) {
				throw new PatchException(String.format(INVALID_PATH_REFERENCE, o_O.getPropertyName(), type), o_O);
			}
		}

		private static final class SkippedPropertyPath {

			private final PropertyPath path;
			private final boolean skipped;

			private SkippedPropertyPath(PropertyPath path, boolean skipped) {

				Assert.notNull(path, "PropertyPath must not be null");

				this.path = path;
				this.skipped = skipped;
			}

			public static SkippedPropertyPath of(String segment, Class<?> type) {
				return of(PropertyPath.from(segment, type), false);
			}

			private static SkippedPropertyPath of(PropertyPath path, boolean skipped) {
				return new SkippedPropertyPath(path, skipped);
			}

			public PropertyPath getPath() {
				return this.path;
			}

			public SkippedPropertyPath nested(String segment) {

				if (skipped) {
					return SkippedPropertyPath.of(path.nested(segment), false);
				}

				TypeInformation<?> typeInformation = path.getTypeInformation();

				return typeInformation.isMap() || typeInformation.isCollectionLike() //
						? SkippedPropertyPath.of(path, true) //
						: SkippedPropertyPath.of(path.nested(segment), false);
			}

			@Override
			public boolean equals(@Nullable Object o) {

				if (o == this) {
					return true;
				}

				if (!(o instanceof SkippedPropertyPath)) {
					return false;
				}
				SkippedPropertyPath other = (SkippedPropertyPath) o;

				return Objects.equals(path, other.path) //
						&& skipped == other.skipped;
			}

			@Override
			public int hashCode() {
				return Objects.hash(path, skipped);
			}

			@Override
			public java.lang.String toString() {
				return "SpelPath.TypedSpelPath.SkippedPropertyPath(path=" + path + ", skipped=" + skipped + ")";
			}
		}

		private static Expression toSpel(String path, Class<?> type) {

			String expression = Arrays.stream(path.split("/"))//
					.filter(it -> !it.isEmpty()) //
					.reduce(Optional.<SpelExpressionBuilder> empty(), //
							(current, next) -> Optional.of(nextOrCreate(current, next, type)), //
							(l, r) -> r) //
					.map(it -> it.getExpression()) //
					.orElse("#this");

			return SPEL_EXPRESSION_PARSER.parseExpression(expression);
		}

		private static SpelExpressionBuilder nextOrCreate(Optional<SpelExpressionBuilder> current, String next,
				Class<?> type) {

			return current //
					.map(it -> it.next(next)) //
					.orElseGet(() -> SpelExpressionBuilder.of(type).next(next));
		}

		private static SkippedPropertyPath createOrSkip(Optional<SkippedPropertyPath> current, String next, Class<?> type) {

			return current //
					.map(it -> it.nested(next)) //
					.orElseGet(() -> SkippedPropertyPath.of(next, type));
		}

		private static final class SpelExpressionBuilder {

			private static final TypeInformation<String> STRING_TYPE = ClassTypeInformation.from(String.class);

			private final @Nullable PropertyPath basePath;
			private final Class<?> type;
			private final String spelSegment;
			private final boolean skipped;

			public SpelExpressionBuilder(@Nullable PropertyPath basePath, Class<?> type, String spelSegment,
					boolean skipped) {

				Assert.notNull(type, "Type must not be null");
				Assert.notNull(spelSegment, "SpEL segment must not be null");

				this.basePath = basePath;
				this.type = type;
				this.spelSegment = spelSegment;
				this.skipped = skipped;
			}

			@Nullable
			public PropertyPath getBasePath() {
				return this.basePath;
			}

			public Class<?> getType() {
				return this.type;
			}

			public String getSpelSegment() {
				return this.spelSegment;
			}

			public boolean isSkipped() {
				return this.skipped;
			}

			public String getExpression() {
				return StringUtils.hasText(spelSegment) ? spelSegment : null;
			}

			public static SpelExpressionBuilder of(Class<?> type) {
				return new SpelExpressionBuilder(null, type, "", false);
			}

			private SpelExpressionBuilder skipWith(String segment) {
				return new SpelExpressionBuilder(basePath, type, spelSegment.concat(segment), true);
			}

			private SpelExpressionBuilder nested(String segment) {

				String segmentBase = StringUtils.hasText(spelSegment) //
						? spelSegment.concat(".") //
						: spelSegment;

				Class<?> currentType = basePath == null ? type : basePath.getLeafType();

				try {

					PropertyPath path = basePath == null //
							? PropertyPath.from(segment, type) //
							: basePath.nested(segment);

					return new SpelExpressionBuilder(path, type, segmentBase.concat(segment), false);

				} catch (PropertyReferenceException o_O) {
					throw new PatchException(String.format(INVALID_PATH_REFERENCE, o_O.getPropertyName(), type), o_O);
				}
			}

			public SpelExpressionBuilder next(String segment) {

				if (basePath == null) {

					if (APPEND_CHARACTER.equals(segment)) {
						return skipWith("$[true]");
					}

					if (segment.matches("\\d+")) {
						return skipWith(String.format("[%s]", segment));
					}

					return nested(segment);
				}

				if (skipped) {
					return nested(segment);
				}

				TypeInformation<?> typeInformation = basePath.getLeafProperty().getTypeInformation();

				if (typeInformation.isMap()) {

					TypeInformation<?> componentType = typeInformation.getComponentType();
					String keyExpression = STRING_TYPE.equals(componentType) ? String.format("'%s'", segment) : segment;

					return skipWith(String.format("[%s]", keyExpression));
				}

				if (typeInformation.isCollectionLike()) {
					return skipWith(APPEND_CHARACTER.equals(segment) ? "$[true]" : String.format("[%s]", segment));
				}

				return nested(segment);
			}

			@Override
			public boolean equals(@Nullable Object o) {

				if (o == this) {
					return true;
				}
				if (!(o instanceof SpelExpressionBuilder)) {
					return false;
				}

				SpelExpressionBuilder that = (SpelExpressionBuilder) o;

				return Objects.equals(basePath, that.basePath) //
						&& Objects.equals(type, that.type) //
						&& Objects.equals(spelSegment, that.spelSegment) //
						&& skipped == that.skipped;
			}

			@Override
			public int hashCode() {
				return Objects.hash(basePath, type, spelSegment, skipped);
			}

			@Override
			public java.lang.String toString() {
				return "SpelPath.TypedSpelPath.SpelExpressionBuilder(basePath=" + this.getBasePath() + ", type="
						+ this.getType() + ", spelSegment=" + this.getSpelSegment() + ", skipped=" + this.isSkipped() + ")";
			}
		}

		@Override
		public boolean equals(@Nullable final java.lang.Object o) {

			if (o == this) {
				return true;
			}

			if (!(o instanceof TypedSpelPath) || !super.equals(o)) {
				return false;
			}

			TypedSpelPath that = (TypedSpelPath) o;

			return Objects.equals(expression, that.expression) //
					&& Objects.equals(type, that.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(expression, type);
		}
	}
}
