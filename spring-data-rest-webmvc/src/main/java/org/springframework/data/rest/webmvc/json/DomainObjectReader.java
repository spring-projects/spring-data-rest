/*
 * Copyright 2014-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;

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
	public <T> T readPut(final ObjectNode source, T target, final ObjectMapper mapper) {

		Assert.notNull(source, "ObjectNode must not be null!");
		Assert.notNull(target, "Existing object instance must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		Class<? extends Object> type = target.getClass();

		final PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);

		Assert.notNull(entity, "No PersistentEntity found for ".concat(type.getName()).concat("!"));

		final MappedProperties properties = MappedProperties.fromJacksonProperties(entity, mapper);

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

				String mappedName = properties.getMappedName(property);

				boolean isMappedProperty = mappedName != null;
				boolean noValueInSource = !source.has(mappedName);

				if (isMappedProperty && noValueInSource) {
					source.putNull(mappedName);
				}
			}
		});

		return merge(source, target, mapper);
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
	private <T> T doMerge(ObjectNode root, T target, ObjectMapper mapper) throws Exception {

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
				i.remove();
				continue;
			}

			PersistentProperty<?> property = mappedProperties.getPersistentProperty(fieldName);
			PersistentPropertyAccessor accessor = entity.getPropertyAccessor(target);
			Object rawValue = accessor.getProperty(property);

			if (child.isArray()) {

				boolean nestedObjectFound = handleArrayNode((ArrayNode) child, asCollection(rawValue), mapper);

				if (nestedObjectFound) {
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

					doMergeNestedMap((Map<String, Object>) rawValue, objectNode, mapper);

					// Remove potentially emptied Map as values have been handled recursively
					if (!objectNode.fieldNames().hasNext()) {
						i.remove();
					}

					continue;
				}

				if (rawValue != null && property.isEntity()) {
					doMerge(objectNode, rawValue, mapper);
				}
			}
		}

		return mapper.readerForUpdating(target).readValue(root);
	}

	/**
	 * Applies the diff handling to {@link ArrayNode}s, potentially recursing into nested ones.
	 * 
	 * @param array the source {@link ArrayNode}m, must not be {@literal null}.
	 * @param collection the actual collection values, must not be {@literal null}.
	 * @param mapper the {@link ObjectMapper} to use, must not be {@literal null}.
	 * @return whether an object merge has been applied to the {@link ArrayNode}.
	 */
	private boolean handleArrayNode(ArrayNode array, Collection<Object> collection, ObjectMapper mapper)
			throws Exception {

		Assert.notNull(array, "ArrayNode must not be null!");
		Assert.notNull(collection, "Source collection must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		Iterator<Object> value = collection.iterator();
		boolean nestedObjectFound = false;

		for (JsonNode jsonNode : array) {

			if (!value.hasNext()) {
				return nestedObjectFound;
			}

			Object next = value.next();

			if (ArrayNode.class.isInstance(jsonNode)) {
				return handleArrayNode(array, asCollection(next), mapper);
			}

			if (ObjectNode.class.isInstance(jsonNode)) {

				nestedObjectFound = true;
				doMerge((ObjectNode) jsonNode, next, mapper);
			}
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
	private void doMergeNestedMap(Map<String, Object> source, ObjectNode node, ObjectMapper mapper) throws Exception {

		if (source == null) {
			return;
		}

		Iterator<Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {

			Entry<String, JsonNode> entry = fields.next();
			JsonNode child = entry.getValue();
			Object sourceValue = source.get(entry.getKey());

			if (child instanceof ObjectNode && sourceValue != null) {
				doMerge((ObjectNode) child, sourceValue, mapper);
			} else if (child instanceof ArrayNode && sourceValue != null) {
				handleArrayNode((ArrayNode) child, asCollection(sourceValue), mapper);
			} else {

				Class<?> target = sourceValue == null ? Object.class : sourceValue.getClass();
				source.put(entry.getKey(), mapper.treeToValue(child, target));
			}

			fields.remove();
		}
	}

	/**
	 * Returns the given source instance as {@link Collection}.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Collection<Object> asCollection(Object source) {

		if (source == null) {
			return Collections.emptyList();
		}

		if (source instanceof Collection) {
			return (Collection<Object>) source;
		}

		if (source.getClass().isArray()) {
			return Arrays.asList((Object[]) source);
		}

		return Collections.singleton(source);
	}
}
