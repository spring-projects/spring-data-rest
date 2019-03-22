/*
 * Copyright 2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;

/**
 * Simple value object to capture a mapping of Jackson mapped field names and {@link PersistentProperty} instances.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class MappedProperties {

	private static final ClassIntrospector INTROSPECTOR = new BasicClassIntrospector();

	private final Map<PersistentProperty<?>, String> propertyToFieldName;
	private final Map<String, PersistentProperty<?>> fieldNameToProperty;

	/**
	 * Creates a new {@link MappedProperties} instance for the given {@link PersistentEntity} and {@link BeanDescription}.
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
