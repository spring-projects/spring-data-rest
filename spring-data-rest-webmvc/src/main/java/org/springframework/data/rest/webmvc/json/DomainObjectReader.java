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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
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

		final PersistentEntity<?, ?> entity = entities.getPersistentEntity(target.getClass());
		final Collection<String> properties = getJacksonProperties(entity, mapper);

		entity.doWithProperties(new SimplePropertyHandler() {
			/*

			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimplePropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
			 */
			@Override
			public void doWithPersistentProperty(PersistentProperty<?> property) {

				boolean isMappedProperty = properties.contains(property.getName());
				boolean noValueInSource = !source.has(property.getName());

				if (isMappedProperty && noValueInSource) {
					source.putNull(property.getName());
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
		Collection<String> mappedProperties = getJacksonProperties(entity, mapper);

		for (Iterator<Entry<String, JsonNode>> i = root.fields(); i.hasNext();) {

			Entry<String, JsonNode> entry = i.next();
			JsonNode child = entry.getValue();

			if (child.isArray()) {
				continue;
			}

			PersistentProperty<?> property = entity.getPersistentProperty(entry.getKey());

			if (property == null || !mappedProperties.contains(property.getName())) {
				i.remove();
				continue;
			}

			if (child.isObject()) {

				if (associationLinks.isLinkableAssociation(property)) {
					continue;
				}

				BeanWrapper<T> wrapper = BeanWrapper.create(target, null);
				Object nested = wrapper.getProperty(property);

				if (nested != null) {
					doMerge((ObjectNode) child, nested, mapper);
				}

			}
		}

		return mapper.readerForUpdating(target).readValue(root);
	}

	/**
	 * Returns the names of all mapped properties for the given {@link PersistentEntity}.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return the collection of mapped properties.
	 */
	private Collection<String> getJacksonProperties(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		BeanDescription description = introspector.forDeserialization(mapper.getDeserializationConfig(),
				mapper.constructType(entity.getType()), mapper.getDeserializationConfig());

		Set<String> properties = new HashSet<String>();

		for (BeanPropertyDefinition property : description.findProperties()) {
			properties.add(property.getInternalName());
		}

		return properties;
	}
}
