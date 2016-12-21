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
package org.springframework.data.rest.webmvc.json;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
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
 * @since 2.2
 */
@RequiredArgsConstructor
public class DomainObjectReader {

	private final @NonNull PersistentEntities entities;
	private final @NonNull Associations associationLinks;

	/**
	 * Reads the given input stream into an {@link ObjectNode} and applies that to the given existing instance.
	 * 
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public <T> T read(InputStream source, T target, ObjectMapper mapper) {

		Assert.notNull(target, "Target object must not be null!");
		Assert.notNull(source, "InputStream must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		try {
			return doMerge((ObjectNode) mapper.readTree(source), target, mapper);
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException("Could not read payload!", o_O);
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
	public <T> T readPut(final ObjectNode source, T target, final ObjectMapper mapper) {

		Assert.notNull(source, "ObjectNode must not be null!");
		Assert.notNull(target, "Existing object instance must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		Class<? extends Object> type = target.getClass();

		final PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);

		Assert.notNull(entity, "No PersistentEntity found for ".concat(type.getName()).concat("!"));

		try {

			Object intermediate = mapper.readerFor(target.getClass()).readValue(source);
			return (T) mergeForPut(intermediate, target, mapper);

		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException("Could not read payload!", o_O);
		}
	}

	/**
	 * Merges the state of given source object onto the target one preserving PUT semantics.
	 * 
	 * @param source can be {@literal null}.
	 * @param target can be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	private <T> T mergeForPut(T source, T target, final ObjectMapper mapper) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");

		if (target == null || source == null) {
			return source;
		}

		Class<? extends Object> type = target.getClass();

		final PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);

		if (entity == null) {
			return source;
		}

		Assert.notNull(entity, "No PersistentEntity found for ".concat(type.getName()).concat("!"));

		final MappedProperties properties = MappedProperties.fromJacksonProperties(entity, mapper);

		ConversionService conversionService = new DefaultConversionService();
		final PersistentPropertyAccessor targetAccessor = entity.getPropertyAccessor(target);
		final ConvertingPropertyAccessor convertingAccessor = new ConvertingPropertyAccessor(targetAccessor,
				conversionService);
		final PersistentPropertyAccessor sourceAccessor = entity.getPropertyAccessor(source);

		entity.doWithProperties(new SimplePropertyHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimplePropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
			 */
			@Override
			public void doWithPersistentProperty(PersistentProperty<?> property) {

				if (property.isIdProperty() || property.isVersionProperty() || !property.isWritable()) {
					return;
				}

				if (!properties.isMappedProperty(property)) {
					return;
				}

				Object sourceValue = sourceAccessor.getProperty(property);
				Object targetValue = targetAccessor.getProperty(property);
				Object result = null;

				if (property.isMap()) {
					result = mergeMaps(property, sourceValue, targetValue, mapper);
				} else if (property.isCollectionLike()) {
					result = mergeCollections(property, sourceValue, targetValue, mapper);
				} else if (property.isEntity()) {
					result = mergeForPut(sourceValue, targetValue, mapper);
				} else {
					result = sourceValue;
				}

				convertingAccessor.setProperty(property, result);
			}
		});

		// Need to copy unmapped properties as the PersistentProperty model currently does not contain any transient
		// properties
		copyRemainingProperties(properties, source, target);

		return target;
	}

	/**
	 * Copies the unmapped properties of the given {@link MappedProperties} from the source object to the target instance.
	 * 
	 * @param properties must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 */
	private static void copyRemainingProperties(MappedProperties properties, Object source, Object target) {

		PropertyAccessor sourceAccessor = PropertyAccessorFactory.forDirectFieldAccess(source);
		PropertyAccessor targetAccessor = PropertyAccessorFactory.forDirectFieldAccess(target);

		for (String property : properties.getSpringDataUnmappedProperties()) {
			targetAccessor.setPropertyValue(property, sourceAccessor.getPropertyValue(property));
		}
	}

	public <T> T merge(ObjectNode source, T target, ObjectMapper mapper) {

		try {
			return doMerge(source, target, mapper);
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException("Could not read payload!", o_O);
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

		Assert.notNull(root, "Root ObjectNode must not be null!");
		Assert.notNull(target, "Target object instance must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(target.getClass());

		if (entity == null) {
			return mapper.readerForUpdating(target).readValue(root);
		}

		MappedProperties mappedProperties = MappedProperties.fromJacksonProperties(entity, mapper);

		for (Iterator<Entry<String, JsonNode>> i = root.fields(); i.hasNext();) {

			Entry<String, JsonNode> entry = i.next();
			JsonNode child = entry.getValue();
			String fieldName = entry.getKey();

			if (!mappedProperties.hasPersistentPropertyForField(fieldName)) {
				continue;
			}

			PersistentProperty<?> property = mappedProperties.getPersistentProperty(fieldName);
			PersistentPropertyAccessor accessor = entity.getPropertyAccessor(target);
			Object rawValue = accessor.getProperty(property);

			if (rawValue == null) {
				continue;
			}

			if (child.isArray()) {

				if (handleArray(child, rawValue, mapper, property.getTypeInformation())) {
					i.remove();
				}

				continue;
			}

			if (child.isObject()) {

				if (associationLinks.isLinkableAssociation(property)) {
					continue;
				}

				ObjectNode objectNode = (ObjectNode) child;

				if (property.isMap()) {

					// Keep empty Map to wipe it as expected
					if (!objectNode.fieldNames().hasNext()) {
						continue;
					}

					doMergeNestedMap((Map<Object, Object>) rawValue, objectNode, mapper, property.getTypeInformation());

					// Remove potentially emptied Map as values have been handled recursively
					if (!objectNode.fieldNames().hasNext()) {
						i.remove();
					}

					continue;
				}

				if (property.isEntity()) {
					i.remove();
					doMerge(objectNode, rawValue, mapper);
				}
			}
		}

		return mapper.readerForUpdating(target).readValue(root);
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
	private boolean handleArray(JsonNode node, Object source, ObjectMapper mapper, TypeInformation<?> collectionType)
			throws Exception {

		Collection<Object> collection = ifCollection(source);

		if (collection == null) {
			return false;
		}

		Iterator<Object> iterator = collection.iterator();
		TypeInformation<?> componentType = iterator.hasNext() ? //
				ClassTypeInformation.from(iterator.next().getClass()) : //
				collectionType.getComponentType();

		return handleArrayNode((ArrayNode) node, collection, mapper, componentType);
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
			TypeInformation<?> componentType) throws Exception {

		Assert.notNull(array, "ArrayNode must not be null!");
		Assert.notNull(collection, "Source collection must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		// We need an iterator for the original collection.
		// We might modify it but we want to keep iterating over the original collection.
		Iterator<Object> value = new ArrayList<Object>(collection).iterator();
		boolean nestedObjectFound = false;

		for (JsonNode jsonNode : array) {

			if (!value.hasNext()) {

				Class<?> type = componentType == null ? Object.class : componentType.getType();
				collection.add(mapper.treeToValue(jsonNode, type));

				continue;
			}

			Object next = value.next();

			if (ArrayNode.class.isInstance(jsonNode)) {
				return handleArray(jsonNode, next, mapper, componentType);
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
		Class<?> valueType = typeOrObject(type.getMapValueType());

		while (fields.hasNext()) {

			Entry<String, JsonNode> entry = fields.next();
			JsonNode value = entry.getValue();
			String key = entry.getKey();

			Object mappedKey = mapper.readValue(quote(key), keyType);
			Object sourceValue = source.get(mappedKey);

			if (value instanceof ObjectNode && sourceValue != null) {

				doMerge((ObjectNode) value, sourceValue, mapper);

			} else if (value instanceof ArrayNode && sourceValue != null) {

				handleArray(value, sourceValue, mapper, type);

			} else {

				Class<?> typeToRead = sourceValue != null ? sourceValue.getClass() : valueType;
				source.put(mappedKey, mapper.treeToValue(value, typeToRead));
			}

			fields.remove();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<Object, Object> mergeMaps(PersistentProperty<?> property, Object source, Object target,
			ObjectMapper mapper) {

		Map<Object, Object> sourceMap = (Map<Object, Object>) source;

		if (sourceMap == null) {
			return null;
		}

		Map<Object, Object> targetMap = (Map<Object, Object>) target;
		Map<Object, Object> result = targetMap == null ? CollectionFactory.createMap(Map.class, sourceMap.size())
				: CollectionFactory.createApproximateMap(targetMap, sourceMap.size());

		for (Entry<Object, Object> entry : sourceMap.entrySet()) {

			Object targetValue = targetMap == null ? null : targetMap.get(entry.getKey());
			result.put(entry.getKey(), mergeForPut(entry.getValue(), targetValue, mapper));
		}

		return result;
	}

	private Collection<Object> mergeCollections(PersistentProperty<?> property, Object source, Object target,
			ObjectMapper mapper) {

		Collection<Object> sourceCollection = asCollection(source);

		if (sourceCollection == null) {
			return null;
		}

		Collection<Object> targetCollection = asCollection(target);
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

		return result;
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

		Assert.notNull(source, "Source instance must not be null!");

		if (source instanceof Collection) {
			return (Collection<Object>) source;
		}

		if (source.getClass().isArray()) {
			return Arrays.asList((Object[]) source);
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
}
