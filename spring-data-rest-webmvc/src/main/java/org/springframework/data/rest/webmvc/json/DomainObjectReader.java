/*
 * Copyright 2014 the original author or authors.
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

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Component to apply an {@link ObjectNode} to an existing domain object. This is effectively a best-effort workaround
 * for Jacksons inability to apply a (partial) JSON document to an existing object in a deeply nestes way. We manually
 * detect nested objects, lookup the original value and apply the merge recursively.
 * 
 * @author Oliver Gierke
 * @since 2.2
 */
public class DomainObjectReader {

	private final PersistentEntities entities;
	private final AssociationLinks associationLinks;
	private final ClassIntrospector introspector;

	/**
	 * Creates a new {@link DomainObjectReader} using the given {@link PersistentEntities} and {@link ResourceMappings}.
	 * 
	 * @param entities must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	public DomainObjectReader(PersistentEntities entities, ResourceMappings mappings) {

		Assert.notNull(entities, "PersistentEntites must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.entities = entities;
		this.associationLinks = new AssociationLinks(mappings);
		this.introspector = new BasicClassIntrospector();
	}

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
			return merge((ObjectNode) mapper.readTree(source), target, mapper);
		} catch (Exception e) {
			throw new HttpMessageNotReadableException("Could not read payload!", e);
		}
	}

	/**
	 * Merges the given {@link ObjectNode} onto the given object.
	 * 
	 * @param root must not be {@literal null}.
	 * @param existingObject
	 * @param mapper
	 * @return
	 * @throws Exception
	 */
	public <T> T merge(ObjectNode root, T existingObject, ObjectMapper mapper) throws Exception {

		Assert.notNull(root, "Root ObjectNode must not be null!");
		Assert.notNull(existingObject, "Existing object instance must not be null!");
		Assert.notNull(mapper, "ObjectMapper must not be null!");

		for (Iterator<Entry<String, JsonNode>> i = root.fields(); i.hasNext();) {

			Entry<String, JsonNode> entry = i.next();
			JsonNode child = entry.getValue();

			if (child.isArray()) {
				// We ignore arrays so they get instantiated fresh every time
			} else if (child.isObject()) {

				PersistentProperty<?> property = findProperty(existingObject, entry.getKey(), mapper);

				if (property == null || associationLinks.isLinkableAssociation(property)) {
					continue;
				}

				BeanWrapper<T> wrapper = BeanWrapper.create(existingObject, null);
				Object nested = wrapper.getProperty(property);

				if (nested != null) {

					// Only remove the JsonNode if the object already exists. Otherwise it will be instantiated when the parent
					// gets deserialized.

					i.remove();
					merge((ObjectNode) child, nested, mapper);
				}
			}
		}

		ObjectReader jsonReader = mapper.readerForUpdating(existingObject);
		jsonReader.readValue(root);

		return existingObject;
	}

	/**
	 * Finds the {@link PersistentProperty} for the JSON field of the given name on the given object.
	 * 
	 * @param object must not be {@literal null}.
	 * @param fieldName must not be {@literal null} or empty.
	 * @param mapper must not be {@literal null}.
	 * @return the {@link PersistentProperty} for the JSON field of the given name on the given object or {@literal null}
	 *         if either the given source object is no persistent entity or the property cannot be found.
	 */
	private PersistentProperty<?> findProperty(Object object, String fieldName, ObjectMapper mapper) {

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(object.getClass());

		if (entity == null) {
			return null;
		}

		BeanDescription description = introspector.forDeserialization(mapper.getDeserializationConfig(),
				mapper.constructType(object.getClass()), mapper.getDeserializationConfig());

		for (BeanPropertyDefinition definition : description.findProperties()) {
			if (definition.getName().equals(fieldName)) {
				return entity.getPersistentProperty(definition.getInternalName());
			}
		}

		return null;
	}
}
