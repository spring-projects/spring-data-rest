/*
 * Copyright 2014-2025 the original author or authors.
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
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

import java.util.Iterator;
import java.util.List;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.mapping.AnnotationBasedResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.TypedResourceDescription;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * Value object to abstract Jackson based bean metadata of a given type.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 5.0
 */
public class Jackson3Metadata implements Iterable<BeanPropertyDefinition> {

	private final ObjectMapper mapper;
	private final List<BeanPropertyDefinition> definitions;
	private final List<BeanPropertyDefinition> deserializationDefinitions;
	private final boolean isValue;

	/**
	 * Creates a new {@link Jackson3Metadata} instance for the given {@link ObjectMapper} and type.
	 *
	 * @param mapper must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 */
	public Jackson3Metadata(ObjectMapper mapper, Class<?> type) {

		Assert.notNull(mapper, "ObjectMapper must not be null");
		Assert.notNull(type, "Type must not be null");

		this.mapper = mapper;

		SerializationConfig serializationConfig = mapper.serializationConfig();
		JavaType javaType = serializationConfig.constructType(type);
		AnnotatedClass annotatedClass = serializationConfig.classIntrospectorInstance()
				.introspectClassAnnotations(javaType);
		BeanDescription description = serializationConfig.classIntrospectorInstance().introspectForSerialization(javaType,
				annotatedClass);

		this.definitions = description.findProperties();
		this.isValue = description.findJsonValueAccessor() != null;

		DeserializationConfig deserializationConfig = mapper.deserializationConfig();
		JavaType deserializationType = deserializationConfig.constructType(type);

		this.deserializationDefinitions = deserializationConfig.classIntrospectorInstance()
				.introspectForDeserialization(deserializationType, annotatedClass).findProperties();
	}

	/**
	 * Returns the {@link BeanPropertyDefinition} for the given {@link PersistentProperty}.
	 *
	 * @param property must not be {@literal null}.
	 * @return can be {@literal null} in case there's no Jackson property to be exposed for the given
	 *         {@link PersistentProperty}.
	 */
	public BeanPropertyDefinition getDefinitionFor(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null");

		return getDefinitionFor(property, definitions);
	}

	/**
	 * Returns the fallback {@link ResourceDescription} to be used for the given {@link BeanPropertyDefinition}.
	 *
	 * @param ownerMetadata must not be {@literal null}.
	 * @param definition must not be {@literal null}.
	 * @return
	 */
	public ResourceDescription getFallbackDescription(ResourceMetadata ownerMetadata, BeanPropertyDefinition definition) {

		Assert.notNull(ownerMetadata, "Owner's resource metadata must not be null");
		Assert.notNull(definition, "BeanPropertyDefinition must not be null");

		AnnotatedMember member = definition.getPrimaryMember();
		Description description = member.getAnnotation(Description.class);
		ResourceDescription fallback = TypedResourceDescription.defaultFor(ownerMetadata.getItemResourceRel(),
				definition.getInternalName(), definition.getPrimaryMember().getRawType());

		return description == null ? fallback : new AnnotationBasedResourceDescription(description, fallback);
	}

	/**
	 * Check if a given property for a type is available to be exported, i.e. serialized via Jackson.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isExported(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null");

		return getDefinitionFor(property) != null;
	}

	/**
	 * Returns whether the backing type is considered a Jackson value type.
	 *
	 * @return the isValue
	 */
	public boolean isValueType() {
		return isValue;
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is considered read-only by Jackson.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isReadOnly(PersistentProperty<?> property) {

		BeanPropertyDefinition definition = getDefinitionFor(property, deserializationDefinitions);
		return definition == null ? false : !definition.couldDeserialize();
	}

	/**
	 * Returns the {@link JsonSerializer} for the given type, or {@literal null} if none available.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public JsonSerializer<?> getTypeSerializer(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		try {

			SerializationConfig provider = mapper.serializationConfig();

			if (!(provider instanceof DefaultSerializationContext)) {
				return null;
			}

			provider = ((DefaultSerializationContext) provider).createInstance(mapper.getSerializationConfig(),
					mapper.getSerializerFactory());

			return provider.findValueSerializer(type);

		} catch (JsonMappingException o_O) {
			return null;
		}
	}

	@Override
	public Iterator<BeanPropertyDefinition> iterator() {
		return definitions.iterator();
	}

	/**
	 * Finds the {@link BeanPropertyDefinition} for the given {@link PersistentProperty} within the given definitions.
	 *
	 * @param property must not be {@literal null}.
	 * @param definitions must not be {@literal null}.
	 * @return
	 */
	private static BeanPropertyDefinition getDefinitionFor(PersistentProperty<?> property,
			Iterable<BeanPropertyDefinition> definitions) {

		for (BeanPropertyDefinition definition : definitions) {
			if (definition.getInternalName().equals(property.getName())) {
				return definition;
			}
		}

		return null;
	}
}
