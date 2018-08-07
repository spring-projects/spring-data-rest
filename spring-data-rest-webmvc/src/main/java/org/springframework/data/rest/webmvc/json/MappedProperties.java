/*
 * Copyright 2016 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;

/**
 * Simple value object to capture a mapping of Jackson mapped field names and {@link PersistentProperty} instances.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Mathias Düsterhöft
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class MappedProperties {

	private static final ClassIntrospector INTROSPECTOR = new BasicClassIntrospector();

	private final Map<PersistentProperty<?>, BeanPropertyDefinition> propertyToFieldName;
	private final Map<String, PersistentProperty<?>> fieldNameToProperty;
	private final Set<BeanPropertyDefinition> unmappedProperties;

	/**
	 * Creates a new {@link MappedProperties} instance for the given {@link PersistentEntity} and {@link BeanDescription}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param description must not be {@literal null}.
	 */
	private MappedProperties(PersistentEntity<?, ? extends PersistentProperty<?>> entity, BeanDescription description) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(description, "BeanDescription must not be null!");

		this.propertyToFieldName = new HashMap<PersistentProperty<?>, BeanPropertyDefinition>();
		this.fieldNameToProperty = new HashMap<String, PersistentProperty<?>>();
		this.unmappedProperties = new HashSet<BeanPropertyDefinition>();

		for (BeanPropertyDefinition property : description.findProperties()) {

			if (description.getIgnoredPropertyNames().contains(property.getName())) {
				continue;
			}

			Optional<? extends PersistentProperty<?>> persistentProperty = //
					Optional.ofNullable(entity.getPersistentProperty(property.getInternalName()));

			persistentProperty.ifPresent(it -> {
				propertyToFieldName.put(it, property);
				fieldNameToProperty.put(property.getName(), it);
			});

			if (!persistentProperty.isPresent()) {
				unmappedProperties.add(property);
			}
		}
	}

	/**
	 * Creates {@link MappedProperties} for the given {@link PersistentEntity} for deserialization purposes. Will not
	 * include Jackson-read-only properties.
	 *
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static MappedProperties forDeserialization(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		DeserializationConfig config = mapper.getDeserializationConfig();
		BeanDescription description = INTROSPECTOR.forDeserialization(config, mapper.constructType(entity.getType()),
				config);

		return new MappedProperties(entity, description);
	}

	/**
	 * Creates {@link MappedProperties} for the given {@link PersistentEntity} for serialization purposes. Includes
	 * Jackson-read-only properties.
	 *
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static MappedProperties forSerialization(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		SerializationConfig config = mapper.getSerializationConfig();
		BeanDescription description = INTROSPECTOR.forSerialization(config, mapper.constructType(entity.getType()), config);

		return new MappedProperties(entity, description);
	}

	public static MappedProperties none() {
		return new MappedProperties(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
	}

	/**
	 * @param property must not be {@literal null}
	 * @return the mapped name for the {@link PersistentProperty}
	 */
	public String getMappedName(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		return propertyToFieldName.get(property).getName();
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

	/**
	 * Returns all properties only known to Jackson.
	 * 
	 * @return the names of all properties that are not known to Spring Data but appear in the Jackson metamodel.
	 */
	public Iterable<String> getSpringDataUnmappedProperties() {

		if (unmappedProperties.isEmpty()) {
			return Collections.emptySet();
		}

		List<String> result = new ArrayList<String>(unmappedProperties.size());

		for (BeanPropertyDefinition definitions : unmappedProperties) {
			result.add(definitions.getInternalName());
		}

		return result;
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is mapped, i.e. known to both Jackson and Spring Data.
	 * 
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isMappedProperty(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		return propertyToFieldName.containsKey(property);
	}
}
