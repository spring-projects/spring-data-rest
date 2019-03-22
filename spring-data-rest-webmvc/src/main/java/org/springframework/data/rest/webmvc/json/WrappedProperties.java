/*
 * Copyright 2016-2019 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Simple value object to capture a mapping of Jackson mapped field names using
 * {@link com.fasterxml.jackson.annotation.JsonUnwrapped} and {@link PersistentProperty} instances.
 *
 * @author Mark Paluch
 * @since 2.6
 */
class WrappedProperties {

	private static final ClassIntrospector INTROSPECTOR = new BasicClassIntrospector();
	private static final AnnotationIntrospector ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

	private final Map<String, List<PersistentProperty<?>>> fieldNameToProperties;

	/**
	 * Creates a new {@link WrappedProperties} instance for the given {@code fieldNameToProperties}.
	 *
	 * @param fieldNameToProperties must not be {@literal null}.
	 */
	private WrappedProperties(Map<String, List<PersistentProperty<?>>> fieldNameToProperties) {
		this.fieldNameToProperties = new HashMap<String, List<PersistentProperty<?>>>(fieldNameToProperties);
	}

	/**
	 * Creates {@link WrappedProperties} for the given {@link PersistentEntities} and {@link PersistentEntity}.
	 *
	 * @param persistentEntities must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static WrappedProperties fromJacksonProperties(PersistentEntities persistentEntities,
			PersistentEntity<?, ?> entity, ObjectMapper mapper) {

		Assert.notNull(entity, "PersistentEntity must not be null!");

		JacksonUnwrappedPropertiesResolver resolver = new JacksonUnwrappedPropertiesResolver(persistentEntities, mapper);
		return new WrappedProperties(resolver.findUnwrappedPropertyPaths(entity.getType()));
	}

	public static WrappedProperties none() {
		return new WrappedProperties(Collections.emptyMap());
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return {@literal true} if the field name resolves to a {@literal PersistentProperty}.
	 */
	public boolean hasPersistentPropertiesForField(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty!");

		return fieldNameToProperties.containsKey(fieldName);
	}

	/**
	 * @param fieldName must not be empty or {@literal null}.
	 * @return
	 */
	public List<PersistentProperty<?>> getPersistentProperties(String fieldName) {

		Assert.hasText(fieldName, "Field name must not be null or empty!");

		return hasPersistentPropertiesForField(fieldName)
				? Collections.unmodifiableList(fieldNameToProperties.get(fieldName))
				: Collections.<PersistentProperty<?>> emptyList();
	}

	/**
	 * This class resolves {@code @JsonUnwrapped} field names to a list of involved {@link PersistentProperty properties}.
	 * 
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	static class JacksonUnwrappedPropertiesResolver {

		private final @NonNull PersistentEntities persistentEntities;
		private final @NonNull ObjectMapper mapper;

		/**
		 * Resolve {@code @JsonUnwrapped} field names to a list of involved {@link PersistentProperty properties}.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public Map<String, List<PersistentProperty<?>>> findUnwrappedPropertyPaths(Class<?> type) {

			Assert.notNull(type, "Type must not be null!");

			return findUnwrappedPropertyPaths(type, NameTransformer.NOP, false);
		}

		private Map<String, List<PersistentProperty<?>>> findUnwrappedPropertyPaths(Class<?> type,
				NameTransformer nameTransformer, boolean considerRegularProperties) {

			return persistentEntities.getPersistentEntity(type).map(entity -> {

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
					ANNOTATION_INTROSPECTOR.findUnwrappingNameTransformer(annotatedMember));

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

			SerializationConfig config = mapper.getSerializationConfig();

			return INTROSPECTOR.forSerialization(config, mapper.constructType(type), config);
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
