/*
 * Copyright 2025 the original author or authors.
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

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;

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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Simple value object to capture a mapping of Jackson mapped field names and {@link PersistentProperty} instances.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Mathias Düsterhöft
 * @since 5.0
 */
class MappedJackson3Properties {

	private final Map<PersistentProperty<?>, BeanPropertyDefinition> propertyToFieldName;
	private final Map<String, PersistentProperty<?>> fieldNameToProperty;
	private final Set<BeanPropertyDefinition> unmappedProperties;
	private final Set<String> ignoredPropertyNames;
	private final boolean anySetterFound;

	private MappedJackson3Properties(Map<PersistentProperty<?>, BeanPropertyDefinition> propertyToFieldName,
			Map<String, PersistentProperty<?>> fieldNameToProperty, Set<BeanPropertyDefinition> unmappedProperties,
			Set<String> ignoredPropertyNames, boolean anySetterFound) {

		this.propertyToFieldName = propertyToFieldName;
		this.fieldNameToProperty = fieldNameToProperty;
		this.unmappedProperties = unmappedProperties;
		this.ignoredPropertyNames = ignoredPropertyNames;
		this.anySetterFound = anySetterFound;
	}

	/**
	 * Creates a new {@link MappedJackson3Properties} instance for the given {@link PersistentEntity} and
	 * {@link BeanDescription}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param description must not be {@literal null}.
	 */
	private MappedJackson3Properties(PersistentEntity<?, ? extends PersistentProperty<?>> entity,
			BeanDescription description) {

		Assert.notNull(entity, "Entity must not be null");
		Assert.notNull(description, "BeanDescription must not be null");

		this.propertyToFieldName = new HashMap<>();
		this.fieldNameToProperty = new HashMap<>();
		this.unmappedProperties = new HashSet<>();

		this.anySetterFound = description.findAnySetterAccessor() != null;

		// We need to call this method after findAnySetterAccessor above as that triggers the
		// collection of ignored properties in the first place. See
		// https://github.com/FasterXML/jackson-databind/issues/2531

		this.ignoredPropertyNames = new HashSet<>(description.getIgnoredPropertyNames());

		JsonIgnoreProperties annotation = entity.findAnnotation(JsonIgnoreProperties.class);

		if (annotation != null) {
			for (String property : annotation.value()) {
				ignoredPropertyNames.add(property);
			}
		}

		for (BeanPropertyDefinition property : description.findProperties()) {

			if (ignoredPropertyNames.contains(property.getName())) {
				continue;
			}

			Optional<? extends PersistentProperty<?>> persistentProperty = //
					Optional.ofNullable(entity.getPersistentProperty(property.getInternalName()));

			persistentProperty //
					.ifPresent(it -> {
						propertyToFieldName.put(it, property);
						fieldNameToProperty.put(property.getName(), it);
					});

			if (!persistentProperty.isPresent()) {
				unmappedProperties.add(property);
			}
		}
	}

	/**
	 * Creates {@link MappedJackson3Properties} for the given {@link PersistentEntity} for deserialization purposes. Will
	 * not include Jackson-read-only properties.
	 *
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static MappedJackson3Properties forDeserialization(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		DeserializationConfig config = mapper.deserializationConfig();
		ClassIntrospector introspector = config.classIntrospectorInstance();
		JavaType javaType = mapper.constructType(entity.getType());
		BeanDescription description = introspector.forOperation(config).introspectForDeserialization(javaType,
				introspector.introspectClassAnnotations(javaType));

		return new MappedJackson3Properties(entity, description);
	}

	/**
	 * Creates {@link MappedJackson3Properties} for the given {@link PersistentEntity} for serialization purposes.
	 * Includes Jackson-read-only properties.
	 *
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static MappedJackson3Properties forSerialization(PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		SerializationConfig config = mapper.serializationConfig();
		ClassIntrospector introspector = config.classIntrospectorInstance();
		JavaType type = mapper.constructType(entity.getType());
		BeanDescription description = introspector.forOperation(config).introspectForSerialization(type,
				introspector.introspectClassAnnotations(type));

		return new MappedJackson3Properties(entity, description);
	}

	public static MappedJackson3Properties forDescription(PersistentEntity<?, ?> entity, BeanDescription description) {
		return new MappedJackson3Properties(entity, description);
	}

	public static MappedJackson3Properties none() {
		return new MappedJackson3Properties(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(),
				Collections.emptySet(), false);
	}

	/**
	 * @param property must not be {@literal null}
	 * @return the mapped name for the {@link PersistentProperty}
	 */
	public String getMappedName(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null");

		return propertyToFieldName.get(property).getName();
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return {@literal true} if the field name resolves to a {@literal PersistentProperty}.
	 */
	public boolean hasPersistentPropertyForField(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty");

		return fieldNameToProperty.containsKey(fieldName);
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return the {@link PersistentProperty} backing the field with the field name.
	 */
	@Nullable
	public PersistentProperty<?> getPersistentProperty(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty");

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
	 * Returns all property names of ignored properties.
	 *
	 * @return will never be {@literal null}.
	 * @since 3.5.11, 3.6.4
	 */
	public Iterable<String> getIgnoredProperties() {
		return ignoredPropertyNames;
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is mapped, i.e. known to both Jackson and Spring Data.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isMappedProperty(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null");

		return propertyToFieldName.containsKey(property);
	}

	/**
	 * Returns whether the property is actually writable. I.e. whether there's a non-read-only property on the target type
	 * or there's a catch all method annotated with {@link JsonAnySetter}.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public boolean isWritableField(String name) {

		Assert.hasText(name, "Property name must not be null or empty");

		if (ignoredPropertyNames.contains(name)) {
			return false;
		}

		PersistentProperty<?> property = fieldNameToProperty.get(name);

		return property != null ? property.isWritable() : anySetterFound;
	}

	public boolean isReadableField(String name) {

		Assert.hasText(name, "Property name must not be null or empty");

		if (ignoredPropertyNames.contains(name)) {
			return false;
		}

		return fieldNameToProperty.get(name) != null;
	}

	public boolean isExposedProperty(String name) {

		Assert.hasText(name, "Property name must not be null or empty");

		if (ignoredPropertyNames.contains(name)) {
			return false;
		}

		PersistentProperty<?> property = fieldNameToProperty.get(name);

		return property != null ? property.isWritable() : anySetterFound;
	}
}
