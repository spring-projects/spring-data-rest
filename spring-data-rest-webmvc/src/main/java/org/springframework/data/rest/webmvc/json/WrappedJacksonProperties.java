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

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.util.NameTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.util.Optionals;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Simple value object to capture a mapping of Jackson mapped field names using {@link JsonUnwrapped} and
 * {@link PersistentProperty} instances.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class WrappedJacksonProperties {

	private static final AnnotationIntrospector ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

	private final Map<String, List<PersistentProperty<?>>> fieldNameToProperties;

	/**
	 * Creates a new {@link WrappedJacksonProperties} instance for the given {@code fieldNameToProperties}.
	 *
	 * @param fieldNameToProperties must not be {@literal null}.
	 */
	private WrappedJacksonProperties(Map<String, List<PersistentProperty<?>>> fieldNameToProperties) {
		this.fieldNameToProperties = new HashMap<String, List<PersistentProperty<?>>>(fieldNameToProperties);
	}

	/**
	 * Creates {@link WrappedJacksonProperties} for the given {@link PersistentEntities} and {@link PersistentEntity}.
	 *
	 * @param persistentEntities must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static WrappedJacksonProperties fromJacksonProperties(PersistentEntities persistentEntities,
			PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		Assert.notNull(entity, "PersistentEntity must not be null");

		JacksonUnwrappedPropertiesResolver resolver = new JacksonUnwrappedPropertiesResolver(persistentEntities, mapper);
		return new WrappedJacksonProperties(resolver.findUnwrappedPropertyPaths(entity.getType()));
	}

	public static WrappedJacksonProperties none() {
		return new WrappedJacksonProperties(Collections.emptyMap());
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return {@literal true} if the field name resolves to a {@literal PersistentProperty}.
	 */
	public boolean hasPersistentPropertiesForField(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty");

		return fieldNameToProperties.containsKey(fieldName);
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return
	 */
	public List<PersistentProperty<?>> getPersistentProperties(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty");

		return hasPersistentPropertiesForField(fieldName)
				? Collections.unmodifiableList(fieldNameToProperties.get(fieldName))
				: Collections.<PersistentProperty<?>> emptyList();
	}

	/**
	 * This class resolves {@code @JsonUnwrapped} field names to a list of involved {@link PersistentProperty properties}.
	 *
	 * @author Mark Paluch
	 */
	static class JacksonUnwrappedPropertiesResolver {

		private final PersistentEntities entities;
		private final ObjectMapper mapper;

		public JacksonUnwrappedPropertiesResolver(PersistentEntities entities, ObjectMapper mapper) {

			Assert.notNull(entities, "PersistentEntities must not be null");
			Assert.notNull(mapper, "ObjectMapper must not be null");

			this.entities = entities;
			this.mapper = mapper;
		}

		/**
		 * Resolve {@code @JsonUnwrapped} field names to a list of involved {@link PersistentProperty properties}.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public Map<String, List<PersistentProperty<?>>> findUnwrappedPropertyPaths(Class<?> type) {

			Assert.notNull(type, "Type must not be null");

			return findUnwrappedPropertyPaths(type, NameTransformer.NOP, false);
		}

		private Map<String, List<PersistentProperty<?>>> findUnwrappedPropertyPaths(Class<?> type,
				NameTransformer nameTransformer, boolean considerRegularProperties) {

			return entities.getPersistentEntity(type).map(entity -> {

				Map<String, List<PersistentProperty<?>>> mapping = new HashMap<String, List<PersistentProperty<?>>>();

				for (BeanPropertyDefinition property : getMappedProperties(entity)) {

					Optionals.ifAllPresent(Optional.ofNullable(entity.getPersistentProperty(property.getInternalName())), //
							findAnnotatedMember(property), //
							(prop, member) -> {

								if (isJsonUnwrapped(member)) {
									mapping.putAll(findUnwrappedPropertyPaths(nameTransformer, member, prop));
								} else if (considerRegularProperties) {
									mapping.put(nameTransformer.transform(property.getName()),
											Collections.<PersistentProperty<?>> singletonList(prop));
								}
							});
				}

				return mapping;

			}).orElse(Collections.emptyMap());
		}

		private Map<String, List<PersistentProperty<?>>> findUnwrappedPropertyPaths(NameTransformer nameTransformer,
				AnnotatedMember annotatedMember, PersistentProperty<?> persistentProperty) {

			Map<String, List<PersistentProperty<?>>> mapping = new HashMap<String, List<PersistentProperty<?>>>();

			NameTransformer propertyNameTransformer = NameTransformer.chainedTransformer(nameTransformer,
					ANNOTATION_INTROSPECTOR.findUnwrappingNameTransformer(mapper.serializationConfig(), annotatedMember));

			Map<String, List<PersistentProperty<?>>> nestedProperties = findUnwrappedPropertyPaths(
					annotatedMember.getRawType(), propertyNameTransformer, true);

			for (Entry<String, List<PersistentProperty<?>>> entry : nestedProperties.entrySet()) {

				List<PersistentProperty<?>> persistentProperties = new ArrayList<PersistentProperty<?>>();

				persistentProperties.add(persistentProperty);
				persistentProperties.addAll(entry.getValue());

				mapping.put(entry.getKey(), persistentProperties);
			}

			return mapping;
		}

		private List<BeanPropertyDefinition> getMappedProperties(PersistentEntity<?, ?> entity) {

			List<BeanPropertyDefinition> properties = getBeanDescription(entity.getType()).findProperties();
			List<BeanPropertyDefinition> withInternalName = new ArrayList<BeanPropertyDefinition>(properties.size());

			for (BeanPropertyDefinition property : properties) {

				Optionals.ifAllPresent(findAnnotatedMember(property), //
						Optional.ofNullable(entity.getPersistentProperty(property.getInternalName())), //
						(member, prop) -> withInternalName.add(property));
			}

			return withInternalName;
		}

		private BeanDescription getBeanDescription(Class<?> type) {

			SerializationConfig config = mapper.serializationConfig();
			JavaType javaType = mapper.constructType(type);
			ClassIntrospector introspector = config.classIntrospectorInstance();

			return introspector.introspectForSerialization(javaType, introspector.introspectClassAnnotations(javaType));
		}

		private static Optional<AnnotatedMember> findAnnotatedMember(BeanPropertyDefinition property) {

			if (property.getPrimaryMember() != null) {
				return Optional.of(property.getPrimaryMember());
			}

			if (property.getGetter() != null) {
				return Optional.of(property.getGetter());
			}

			if (property.getSetter() != null) {
				return Optional.of(property.getSetter());
			}

			return Optional.empty();
		}

		private static boolean isJsonUnwrapped(AnnotatedMember primaryMember) {
			return primaryMember.hasAnnotation(JsonUnwrapped.class)
					&& primaryMember.getAnnotation(JsonUnwrapped.class).enabled();
		}
	}
}
