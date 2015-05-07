/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.JsonSchemaFormat;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.json.JsonSchema.Descriptors;
import org.springframework.data.rest.webmvc.json.JsonSchema.EnumProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.Item;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.Property;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Converter to create {@link JsonSchema} instances for {@link PersistentEntity}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityToJsonSchemaConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	private static final TypeDescriptor SCHEMA_TYPE = TypeDescriptor.valueOf(JsonSchema.class);
	private static final TypeInformation<?> STRING_TYPE_INFORMATION = ClassTypeInformation.from(String.class);

	private final Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
	private final ResourceMappings mappings;
	private final PersistentEntities entities;
	private final MessageSourceAccessor accessor;
	private final ObjectMapper objectMapper;
	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link PersistentEntityToJsonSchemaConverter} for the given {@link PersistentEntities} and
	 * {@link ResourceMappings}.
	 * 
	 * @param entities must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 * @param objectMapper must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 */
	public PersistentEntityToJsonSchemaConverter(PersistentEntities entities, ResourceMappings mappings,
			MessageSourceAccessor accessor, ObjectMapper objectMapper, RepositoryRestConfiguration configuration) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(accessor, "MessageSourceAccessor must not be null!");
		Assert.notNull(objectMapper, "ObjectMapper must not be null!");
		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

		this.entities = entities;
		this.mappings = mappings;
		this.accessor = accessor;
		this.objectMapper = objectMapper;
		this.configuration = configuration;

		for (TypeInformation<?> domainType : entities.getManagedTypes()) {
			convertiblePairs.add(new ConvertiblePair(domainType.getType(), JsonSchema.class));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return Class.class.isAssignableFrom(sourceType.getType())
				&& JsonSchema.class.isAssignableFrom(targetType.getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return convertiblePairs;
	}

	/**
	 * Converts the given type into a {@link JsonSchema} instance.
	 * 
	 * @param domainType must not be {@literal null}.
	 * @return
	 */
	public JsonSchema convert(Class<?> domainType) {
		return (JsonSchema) convert(domainType, STRING_TYPE, SCHEMA_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public JsonSchema convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		final PersistentEntity<?, ?> persistentEntity = entities.getPersistentEntity((Class<?>) source);
		final ResourceMetadata metadata = mappings.getMetadataFor(persistentEntity.getType());

		Descriptors descriptors = new Descriptors();
		List<JsonSchemaProperty<?>> propertiesFor = getPropertiesFor(persistentEntity.getType(), metadata, descriptors);

		return new JsonSchema(persistentEntity.getName(), resolveMessage(metadata.getItemResourceDescription()),
				propertiesFor, descriptors);
	}

	private List<JsonSchemaProperty<?>> getPropertiesFor(Class<?> type, final ResourceMetadata metadata,
			final Descriptors descriptors) {

		final PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);
		final JacksonMetadata jackson = new JacksonMetadata(objectMapper, type);
		final AssociationLinks associationLinks = new AssociationLinks(mappings);

		if (entity == null) {
			return Collections.<JsonSchemaProperty<?>> emptyList();
		}

		final List<JsonSchemaProperty<?>> properties = new ArrayList<JsonSchema.JsonSchemaProperty<?>>();

		for (BeanPropertyDefinition definition : jackson) {

			PersistentProperty<?> persistentProperty = entity.getPersistentProperty(definition.getInternalName());
			TypeInformation<?> propertyType = persistentProperty == null ? ClassTypeInformation.from(definition
					.getPrimaryMember().getRawType()) : persistentProperty.getTypeInformation();
			Class<?> rawPropertyType = propertyType.getType();

			JsonSchemaFormat format = configuration.metadataConfiguration().getSchemaFormatFor(rawPropertyType);
			ResourceDescription description = persistentProperty == null ? jackson.getFallbackDescription(definition)
					: getDescriptionFor(persistentProperty, metadata);
			Property property = getSchemaProperty(definition, propertyType, description);

			if (persistentProperty != null && !persistentProperty.isWritable()) {
				property = property.withReadOnly();
			}

			if (format != null) {

				// Types with explicitly registered format -> value object with format
				properties.add(property.with(format));
				continue;
			}

			Pattern pattern = configuration.metadataConfiguration().getPatternFor(rawPropertyType);

			if (pattern != null) {
				properties.add(property.with(pattern));
				continue;
			}

			if (jackson.isValueType()) {
				properties.add(property.with(STRING_TYPE_INFORMATION));
				continue;
			}

			if (persistentProperty == null) {
				properties.add(property);
				continue;
			}

			if (persistentProperty.isIdProperty() && !configuration.isIdExposedFor(rawPropertyType)) {
				continue;
			}

			if (associationLinks.isLinkableAssociation(persistentProperty)) {
				properties.add(property.with(JsonSchemaFormat.URI));
			} else {

				if (persistentProperty.isEntity()) {

					if (!descriptors.hasDescriptorFor(propertyType)) {
						descriptors.addDescriptor(propertyType,
								new Item(propertyType, getNestedPropertiesFor(persistentProperty, descriptors)));
					}

					properties.add(property.with(propertyType, Descriptors.getReference(propertyType)));

				} else {

					properties.add(property.with(propertyType));
				}
			}
		}

		return properties;
	}

	private Collection<JsonSchemaProperty<?>> getNestedPropertiesFor(PersistentProperty<?> property,
			Descriptors descriptors) {

		if (!property.isEntity()) {
			return Collections.emptyList();
		}

		return getPropertiesFor(property.getActualType(), mappings.getMetadataFor(property.getActualType()), descriptors);
	}

	private Property getSchemaProperty(BeanPropertyDefinition definition, TypeInformation<?> type,
			ResourceDescription description) {

		String name = definition.getName();
		String resolvedDescription = resolveMessage(description);
		boolean required = definition.isRequired();
		Class<?> rawType = type.getType();

		if (!rawType.isEnum()) {
			return new Property(name, resolvedDescription, required);
		}

		return new EnumProperty(name, rawType, description.getDefaultMessage().equals(resolvedDescription) ? null
				: resolvedDescription, required);
	}

	private ResourceDescription getDescriptionFor(PersistentProperty<?> property, ResourceMetadata metadata) {

		ResourceMapping propertyMapping = metadata.getMappingFor(property);
		return propertyMapping.getDescription();
	}

	private String resolveMessage(MessageSourceResolvable resolvable) {

		if (resolvable == null) {
			return null;
		}

		try {
			return accessor.getMessage(resolvable);
		} catch (NoSuchMessageException o_O) {

			if (configuration.metadataConfiguration().omitUnresolvableDescriptionKeys()) {
				return null;
			} else {
				throw o_O;
			}
		}
	}
}
