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
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Component to apply an {@link ObjectNode} to an existing domain object. This is effectively a best-effort workaround
 * for Jackson's inability to apply a (partial) JSON document to an existing object in a deeply nested way. We manually
 * detect nested objects, lookup the original value and apply the merge recursively.
 * 
 * @author Oliver Gierke
 * @since 2.2
 */
@RequiredArgsConstructor
public class DomainObjectReader {

	private final @NonNull PersistentEntities entities;
	private final @NonNull Associations associationLinks;

	/**
	 * Reads the given input stream into an {@link ObjectNode} and applies that to the given existing instance.
	 * 
	 * @param request must not be {@literal null}.
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

			if (child.isArray()) {
				continue;
			}

			String fieldName = entry.getKey();

			if (!mappedProperties.hasPersistentPropertyForField(fieldName)) {
				i.remove();
				continue;
			}

			if (child.isObject()) {

				PersistentProperty<?> property = mappedProperties.getPersistentProperty(fieldName);

				if (associationLinks.isLinkableAssociation(property)) {
					continue;
				}

				PersistentPropertyAccessor accessor = entity.getPropertyAccessor(target);
				Object nested = accessor.getProperty(property);

				ObjectNode objectNode = (ObjectNode) child;

				if (property.isMap()) {

					// Keep empty Map to wipe it as expected
					if (!objectNode.fieldNames().hasNext()) {
						continue;
					}

					doMergeNestedMap((Map<String, Object>) nested, objectNode, mapper);

					// Remove potentially emptied Map as values have been handled recursively
					if (!objectNode.fieldNames().hasNext()) {
						i.remove();
					}

					continue;
				}

				if (nested != null && property.isEntity()) {
					doMerge(objectNode, nested, mapper);
				}
			}
		}

		return mapper.readerForUpdating(target).readValue(root);
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
				fields.remove();
			}
		}
	}

	/**
	 * Simple value object to capture a mapping of Jackson mapped field names and {@link PersistentProperty} instances.
	 *
	 * @author Oliver Gierke
	 */
	static class MappedProperties {

		private static final ClassIntrospector INTROSPECTOR = new BasicClassIntrospector();

		private final Map<PersistentProperty<?>, String> propertyToFieldName;
		private final Map<String, PersistentProperty<?>> fieldNameToProperty;

		/**
		 * Creates a new {@link MappedProperties} instance for the given {@link PersistentEntity} and
		 * {@link BeanDescription}.
		 *
		 * @param entity must not be {@literal null}.
		 * @param description must not be {@literal null}.
		 */
		private MappedProperties(PersistentEntity<?, ?> entity, BeanDescription description) {

			Assert.notNull(entity, "Entity must not be null!");
			Assert.notNull(description, "BeanDescription must not be null!");

			this.propertyToFieldName = new HashMap<PersistentProperty<?>, String>();
			this.fieldNameToProperty = new HashMap<String, PersistentProperty<?>>();

			for (BeanPropertyDefinition property : description.findProperties()) {

				PersistentProperty<?> persistentProperty = entity.getPersistentProperty(property.getInternalName());

				if (persistentProperty != null) {
					propertyToFieldName.put(persistentProperty, property.getName());
					fieldNameToProperty.put(property.getName(), persistentProperty);
				}
			}
		}

		/**
		 * Creates {@link MappedProperties} for the given {@link PersistentEntity}.
		 *
		 * @param entity must not be {@literal null}.
		 * @param mapper must not be {@literal null}.
		 * @return
		 */
		public static MappedProperties fromJacksonProperties(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

			BeanDescription description = INTROSPECTOR.forDeserialization(mapper.getDeserializationConfig(),
					mapper.constructType(entity.getType()), mapper.getDeserializationConfig());

			return new MappedProperties(entity, description);
		}

		/**
		 * @param property must not be {@literal null}
		 * @return the mapped name for the {@link PersistentProperty}
		 */
		public String getMappedName(PersistentProperty<?> property) {

			Assert.notNull(property, "PersistentProperty must not be null!");

			return propertyToFieldName.get(property);
		}

		/**
		 * @param fieldName must not be empty or {@literal null}.
		 * @return {@literal true} if the field name resolves to a {@literal PersistentProperty}.
		 */
		public boolean hasPersistentPropertyForField(String fieldName) {

			Assert.hasText(fieldName, "Field name must not be null or empty!");

			return fieldNameToProperty.containsKey(fieldName);
		}

		/**
		 * @param fieldName must not be empty or {@literal null}.
		 * @return the {@link PersistentProperty} backing the field with the field name.
		 */
		public PersistentProperty<?> getPersistentProperty(String fieldName) {

			Assert.hasText(fieldName, "Field name must not be null or empty!");

			return fieldNameToProperty.get(fieldName);
		}
	}
}
