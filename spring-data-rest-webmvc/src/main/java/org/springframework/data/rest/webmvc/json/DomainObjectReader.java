/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.IntFunction;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.util.InputStreamHttpInputMessage;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Component to apply an {@link ObjectNode} to an existing domain object. This is effectively a best-effort workaround
 * for Jackson's inability to apply a (partial) JSON document to an existing object in a deeply nested way. We manually
 * detect nested objects, lookup the original value and apply the merge recursively.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Craig Andrews
 * @author Mathias Düsterhöft
 * @author Thomas Mrozinski
 * @author Lars Vierbergen
 * @since 2.2
 */
public class DomainObjectReader {

	private final PersistentEntities entities;
	private final Associations associationLinks;

	public DomainObjectReader(PersistentEntities entities, Associations associationLinks) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(associationLinks, "Associations must not be null");

		this.entities = entities;
		this.associationLinks = associationLinks;
	}

	/**
	 * Reads the given input stream into an {@link ObjectNode} and applies that to the given existing instance.
	 *
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public <T> T read(InputStream source, T target, ObjectMapper mapper) {

		Assert.notNull(target, "Target object must not be null");
		Assert.notNull(source, "InputStream must not be null");
		Assert.notNull(mapper, "ObjectMapper must not be null");

		try {
			return doMerge((ObjectNode) mapper.readTree(source), target, mapper);
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException("Could not read payload", o_O, InputStreamHttpInputMessage.of(source));
		}
	}

	/**
	 * Reads the given source node onto the given target object and applies PUT semantics, i.e. explicitly
	 *
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param mapper
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T readPut(final ObjectNode source, T target, final ObjectMapper mapper) throws Exception {

		Assert.notNull(source, "ObjectNode must not be null");
		Assert.notNull(target, "Existing object instance must not be null");
		Assert.notNull(mapper, "ObjectMapper must not be null");

		Object intermediate = mapper.readerFor(target.getClass()).readValue(source);
		return (T) mergeForPut(intermediate, target, mapper);
	}

	/**
	 * Merges the state of given source object onto the target one preserving PUT semantics.
	 *
	 * @param source can be {@literal null}.
	 * @param target can be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	@Nullable
	<T> T mergeForPut(T source, T target, final ObjectMapper mapper) {

		Assert.notNull(mapper, "ObjectMapper must not be null");

		if (target == null || source == null) {
			return source;
		}

		boolean isTypeChange = !source.getClass().isInstance(target);

		boolean immutableTarget = entities.getPersistentEntity(target.getClass())
				.map(PersistentEntity::isImmutable)
				.orElse(true); // Not a Spring Data managed type -> no detailed merging

		return entities.getPersistentEntity(isTypeChange ? source.getClass() : target.getClass()) //
				.map(it -> {

					MappedProperties properties = MappedProperties.forDeserialization(it, mapper);

					if (isTypeChange || immutableTarget || it.isImmutable()) {

						copyRemainingProperties(properties.getIgnoredProperties(), target, source);

						return source;
					}

					MergingPropertyHandler propertyHandler = new MergingPropertyHandler(source, target, it, mapper);

					it.doWithProperties(propertyHandler);
					it.doWithAssociations(new LinkedAssociationSkippingAssociationHandler(associationLinks, propertyHandler));

					// Need to copy unmapped properties as the PersistentProperty model currently does not contain any transient
					// properties
					copyRemainingProperties(properties.getSpringDataUnmappedProperties(), source, target);

					return target;

				}).orElse(source);
	}

	/**
	 * Copies the given properties from the source object to the target instance.
	 *
	 * @param propertyNames must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 */
	private static void copyRemainingProperties(Iterable<String> propertyNames, Object source, Object target) {

		PropertyAccessor sourceFieldAccessor = PropertyAccessorFactory.forDirectFieldAccess(source);
		PropertyAccessor sourcePropertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(source);
		PropertyAccessor targetFieldAccessor = PropertyAccessorFactory.forDirectFieldAccess(target);
		PropertyAccessor targetPropertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(target);

		for (String property : propertyNames) {

			// If there's a field we can just copy it.
			if (targetFieldAccessor.isWritableProperty(property)) {
				targetFieldAccessor.setPropertyValue(property, sourceFieldAccessor.getPropertyValue(property));
				continue;
			}

			// Otherwise only copy if there's both a getter and setter.
			if (targetPropertyAccessor.isWritableProperty(property) && sourcePropertyAccessor.isReadableProperty(property)) {
				targetPropertyAccessor.setPropertyValue(property, sourcePropertyAccessor.getPropertyValue(property));
			}
		}
	}

	/**
	 * Only for internal use. To be removed in 3.4.
	 *
	 * @param <T>
	 * @param source
	 * @param target
	 * @param mapper
	 * @return
	 */
	<T> T merge(ObjectNode source, T target, ObjectMapper mapper) {

		try {
			return doMerge(source, target, mapper);
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException("Could not read payload", o_O);
		}
	}

	/**
	 * Merges the given {@link ObjectNode} onto the given object.
	 *
	 * @param root must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	<T> T doMerge(ObjectNode root, T target, ObjectMapper mapper) throws Exception {

		Assert.notNull(root, "Root ObjectNode must not be null");
		Assert.notNull(target, "Target object instance must not be null");
		Assert.notNull(mapper, "ObjectMapper must not be null");

		Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> candidate = entities
				.getPersistentEntity(target.getClass());

		if (!candidate.isPresent()) {
			return mapper.readerForUpdating(target).readValue(root);
		}

		PersistentEntity<?, ?> entity = candidate.get();
		MappedProperties mappedProperties = MappedProperties.forDeserialization(entity, mapper);
		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(target);

		for (Iterator<Entry<String, JsonNode>> i = root.fields(); i.hasNext();) {

			Entry<String, JsonNode> entry = i.next();
			JsonNode child = entry.getValue();
			String fieldName = entry.getKey();

			if (!mappedProperties.isWritableField(fieldName)) {

				i.remove();
				continue;
			}

			PersistentProperty<?> property = mappedProperties.getPersistentProperty(fieldName);
			Optional<Object> rawValue = Optional.ofNullable(accessor.getProperty(property));

			if (!rawValue.isPresent() || associationLinks.isLinkableAssociation(property)) {
				continue;
			}

			rawValue.ifPresent(it -> {

				if (child.isArray()) {

					IntFunction<Object> rawValues = index -> readRawCollectionElement(property.getComponentType(), fieldName,
							index, root, mapper);

					if (handleArray(child, it, mapper, property.getTypeInformation(), rawValues)) {
						i.remove();
					}

					return;
				}

				if (child.isObject()) {

					ObjectNode objectNode = (ObjectNode) child;

					if (property.isMap()) {

						// Keep empty Map to wipe it as expected
						if (!objectNode.fieldNames().hasNext()) {
							return;
						}

						execute(
								() -> doMergeNestedMap((Map<Object, Object>) it, objectNode, mapper, property.getTypeInformation()));

						// Remove potentially emptied Map as values have been handled recursively
						if (!objectNode.fieldNames().hasNext()) {
							i.remove();
						}

						return;
					}

					if (property.isEntity()) {
						i.remove();
						execute(() -> doMerge(objectNode, it, mapper));
					}
				}
			});
		}

		return mapper.readerForUpdating(target).readValue(root);
	}

	private static Object readRawCollectionElement(Class<?> elementType, String fieldName, int index, JsonNode root,
			ObjectMapper mapper) {

		try {
			return mapper.readerFor(elementType).at("/" + fieldName + "/" + index).readValue(root);
		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	/**
	 * Handles the given {@link JsonNode} by treating it as {@link ArrayNode} and the given source value as
	 * {@link Collection}-like value. Looks up the actual type to handle from the potentially available first element,
	 * falling back to component type lookup on the given type.
	 *
	 * @param node must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @param collectionType must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	private boolean handleArray(JsonNode node, Object source, ObjectMapper mapper, TypeInformation<?> collectionType,
			IntFunction<Object> rawValues) {

		Collection<Object> collection = ifCollection(source);

		if (collection == null) {
			return false;
		}

		return execute(
				() -> handleArrayNode((ArrayNode) node, collection, mapper, collectionType.getComponentType(), rawValues));
	}

	/**
	 * Applies the diff handling to {@link ArrayNode}s, potentially recursing into nested ones.
	 *
	 * @param array the source {@link ArrayNode}m, must not be {@literal null}.
	 * @param collection the actual collection values, must not be {@literal null}.
	 * @param mapper the {@link ObjectMapper} to use, must not be {@literal null}.
	 * @param componentType the item type of the collection, can be {@literal null}.
	 * @return whether an object merge has been applied to the {@link ArrayNode}.
	 */
	private boolean handleArrayNode(ArrayNode array, Collection<Object> collection, ObjectMapper mapper,
			TypeInformation<?> componentType, IntFunction<Object> rawValues) throws Exception {

		Assert.notNull(array, "ArrayNode must not be null");
		Assert.notNull(collection, "Source collection must not be null");
		Assert.notNull(mapper, "ObjectMapper must not be null");

		// Empty collection? Primitive? Enum? No need to merge.
		if (array.isEmpty()
				|| collection.isEmpty()
				|| ClassUtils.isPrimitiveOrWrapper(componentType.getType())
				|| componentType.getType().isEnum()
				|| entities.getPersistentEntity(componentType.getType()).isEmpty()) {
			return false;
		}

		// We need an iterator for the original collection.
		// We might modify it but we want to keep iterating over the original collection.
		Iterator<Object> value = new ArrayList<Object>(collection).iterator();
		boolean nestedObjectFound = false;
		int i = 0;

		for (JsonNode jsonNode : array) {

			int current = i++;

			// We need to append new elements
			if (!value.hasNext()) {

				nestedObjectFound = true;

				// Use pre-read values if available. Deserialize node otherwise.
				collection.add(rawValues != null
						? rawValues.apply(current)
						: mapper.treeToValue(jsonNode, componentType.getType()));

				break;
			}

			Object next = value.next();

			if (ArrayNode.class.isInstance(jsonNode)) {
				return handleArray(jsonNode, next, mapper, getTypeToMap(value, componentType), null);
			}

			if (ObjectNode.class.isInstance(jsonNode)) {

				nestedObjectFound = true;
				doMerge((ObjectNode) jsonNode, next, mapper);
			}
		}

		// there are more items in the collection than contained in the JSON node - remove it.
		while (value.hasNext()) {
			collection.remove(value.next());
		}

		return nestedObjectFound;
	}

	/**
	 * Merges nested {@link Map} values for the given source {@link Map}, the {@link ObjectNode} and {@link ObjectMapper}.
	 *
	 * @param source can be {@literal null}.
	 * @param node must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @throws Exception
	 */
	private void doMergeNestedMap(Map<Object, Object> source, ObjectNode node, ObjectMapper mapper,
			TypeInformation<?> type) throws Exception {

		if (source == null) {
			return;
		}

		Iterator<Entry<String, JsonNode>> fields = node.fields();
		Class<?> keyType = typeOrObject(type.getComponentType());
		TypeInformation<?> valueType = type.getMapValueType();

		while (fields.hasNext()) {

			Entry<String, JsonNode> entry = fields.next();
			JsonNode value = entry.getValue();
			String key = entry.getKey();

			Object mappedKey = mapper.readValue(quote(key), keyType);
			Object sourceValue = source.get(mappedKey);
			TypeInformation<?> typeToMap = getTypeToMap(sourceValue, valueType);

			if (value instanceof ObjectNode && sourceValue != null) {

				doMerge((ObjectNode) value, sourceValue, mapper);

			} else if (value instanceof ArrayNode && sourceValue != null) {

				handleArray(value, sourceValue, mapper, getTypeToMap(sourceValue, typeToMap), null);

			} else {

				source.put(mappedKey, mapper.treeToValue(value, typeToMap.getType()));
			}

			fields.remove();
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<Map<Object, Object>> mergeMaps(PersistentProperty<?> property, Optional<Object> source,
			Optional<Object> target, ObjectMapper mapper) {

		return source.map(it -> {

			Map<Object, Object> sourceMap = (Map<Object, Object>) it;
			Map<Object, Object> targetMap = (Map<Object, Object>) target.orElse(null);

			Map<Object, Object> result = targetMap == null ? CollectionFactory.createMap(Map.class, sourceMap.size())
					: CollectionFactory.createApproximateMap(targetMap, sourceMap.size());

			for (Entry<Object, Object> entry : sourceMap.entrySet()) {

				Object targetValue = targetMap == null ? null : targetMap.get(entry.getKey());
				result.put(entry.getKey(), mergeForPut(entry.getValue(), targetValue, mapper));
			}

			if (targetMap == null) {
				return result;
			}

			try {

				targetMap.clear();
				targetMap.putAll(result);

				return targetMap;

			} catch (UnsupportedOperationException o_O) {
				return result;
			}
		});
	}

	private Optional<Collection<Object>> mergeCollections(PersistentProperty<?> property, Optional<Object> source,
			Optional<Object> target, ObjectMapper mapper) {

		return source.map(it -> {

			Collection<Object> sourceCollection = asCollection(it);
			Collection<Object> targetCollection = asCollection(target.orElse(null));
			Collection<Object> result = targetCollection == null
					? CollectionFactory.createCollection(Collection.class, sourceCollection.size())
					: CollectionFactory.createApproximateCollection(targetCollection, sourceCollection.size());

			Iterator<Object> sourceIterator = sourceCollection.iterator();
			Iterator<Object> targetIterator = targetCollection == null ? Collections.emptyIterator()
					: targetCollection.iterator();

			while (sourceIterator.hasNext()) {

				Object sourceElement = sourceIterator.next();
				Object targetElement = targetIterator.hasNext() ? targetIterator.next() : null;

				result.add(mergeForPut(sourceElement, targetElement, mapper));
			}

			if (targetCollection == null) {
				return result;
			}

			try {

				targetCollection.clear();
				targetCollection.addAll(result);

				return targetCollection;

			} catch (UnsupportedOperationException o_O) {
				return result;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static Collection<Object> asCollection(Object source) {

		if (source == null) {
			return null;
		} else if (source instanceof Collection) {
			return (Collection<Object>) source;
		} else if (source.getClass().isArray()) {
			return Arrays.asList(ObjectUtils.toObjectArray(source));
		} else {
			return Collections.singleton(source);
		}
	}

	/**
	 * Returns the given source instance as {@link Collection} or creates a new one for the given type.
	 *
	 * @param source can be {@literal null}.
	 * @param type must not be {@literal null} in case {@code source} is null.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Collection<Object> ifCollection(Object source) {

		Assert.notNull(source, "Source instance must not be null");

		if (source instanceof Collection) {
			return (Collection<Object>) source;
		}

		if (source.getClass().isArray()) {
			return new ArrayList<>(Arrays.asList((Object[]) source));
		}

		return null;
	}

	/**
	 * Surrounds the given source {@link String} with quotes so that they represent a valid JSON String.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	private static String quote(String source) {
		return source == null ? null : "\"".concat(source).concat("\"");
	}

	/**
	 * Returns the raw type of the given {@link TypeInformation} or {@link Object} as fallback.
	 *
	 * @param type can be {@literal null}.
	 * @return
	 */
	private static Class<?> typeOrObject(TypeInformation<?> type) {
		return type == null ? Object.class : type.getType();
	}

	/**
	 * Returns the type to read for the given value and default type. The type will be defaulted to {@link Object} if
	 * missing. If the given value's type is different from the given default (i.e. more concrete) the value's type will
	 * be used.
	 *
	 * @param value can be {@literal null}.
	 * @param type can be {@literal null}.
	 * @return
	 */
	private static TypeInformation<?> getTypeToMap(Object value, TypeInformation<?> type) {

		if (type == null) {
			return ClassTypeInformation.OBJECT;
		}

		if (value == null) {
			return type;
		}

		if (Enum.class.isInstance(value)) {
			return ClassTypeInformation.from(((Enum<?>) value).getDeclaringClass());
		}

		return value.getClass().equals(type.getType()) ? type : ClassTypeInformation.from(value.getClass());

	}

	/**
	 * {@link SimpleAssociationHandler} that skips linkable associations and forwards handling for all other ones to the
	 * delegate {@link SimplePropertyHandler}.
	 *
	 * @author Oliver Gierke
	 */
	private static final class LinkedAssociationSkippingAssociationHandler implements SimpleAssociationHandler {

		private final Associations associations;
		private final SimplePropertyHandler delegate;

		public LinkedAssociationSkippingAssociationHandler(Associations associations, SimplePropertyHandler delegate) {

			Assert.notNull(associations, "Associations must not be null");
			Assert.notNull(delegate, "Delegate SimplePropertyHandler must not be null");

			this.associations = associations;
			this.delegate = delegate;
		}

		@Override
		public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

			if (associations.isLinkableAssociation(association)) {
				return;
			}

			delegate.doWithPersistentProperty(association.getInverse());
		}
	}

	/**
	 * {@link SimplePropertyHandler} to merge the states of the given objects.
	 *
	 * @author Oliver Gierke
	 */
	private class MergingPropertyHandler implements SimplePropertyHandler {

		private final MappedProperties properties;
		private final PersistentPropertyAccessor<?> targetAccessor;
		private final PersistentPropertyAccessor<?> sourceAccessor;
		private final ObjectMapper mapper;

		/**
		 * Creates a new {@link MergingPropertyHandler} for the given source, target, {@link PersistentEntity} and
		 * {@link ObjectMapper}.
		 *
		 * @param source must not be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @param entity must not be {@literal null}.
		 * @param mapper must not be {@literal null}.
		 */
		public MergingPropertyHandler(Object source, Object target, PersistentEntity<?, ?> entity, ObjectMapper mapper) {

			Assert.notNull(source, "Source instance must not be null");
			Assert.notNull(target, "Target instance must not be null");
			Assert.notNull(entity, "PersistentEntity must not be null");
			Assert.notNull(mapper, "ObjectMapper must not be null");

			this.properties = MappedProperties.forDeserialization(entity, mapper);
			this.targetAccessor = new ConvertingPropertyAccessor<>(entity.getPropertyAccessor(target),
					new DefaultConversionService());
			this.sourceAccessor = entity.getPropertyAccessor(source);
			this.mapper = mapper;
		}

		public MappedProperties getProperties() {
			return this.properties;
		}

		@Override
		public void doWithPersistentProperty(PersistentProperty<?> property) {

			if (property.isIdProperty() || property.isVersionProperty() || !property.isWritable()) {
				return;
			}

			if (!properties.isMappedProperty(property)) {
				return;
			}

			Optional<Object> sourceValue = Optional.ofNullable(sourceAccessor.getProperty(property));

			if (property.isImmutable()) {
				targetAccessor.setProperty(property, sourceValue.orElse(null));
				return;
			}

			Optional<Object> targetValue = Optional.ofNullable(targetAccessor.getProperty(property));
			Optional<?> result = Optional.empty();

			if (property.isMap()) {
				result = mergeMaps(property, sourceValue, targetValue, mapper);
			} else if (property.isCollectionLike()) {
				result = mergeCollections(property, sourceValue, targetValue, mapper);
			} else if (property.isEntity()) {

				result = targetValue.isEmpty()
						? sourceValue
						: targetValue.flatMap(t -> sourceValue.map(s -> mergeForPut(s, t, mapper)));
			} else {
				result = sourceValue;
			}

			targetAccessor.setProperty(property, result.orElse(null));
		}
	}

	private static <T> T execute(SupplierWithException<T> block) {

		try {
			return block.execute();
		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private static void execute(RunnableWithException block) {

		try {
			block.execute();
		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	interface RunnableWithException {
		void execute() throws Exception;
	}

	interface SupplierWithException<T> {
		T execute() throws Exception;
	}
}
