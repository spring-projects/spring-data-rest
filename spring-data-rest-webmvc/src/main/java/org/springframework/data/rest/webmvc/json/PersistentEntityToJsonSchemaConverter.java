/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.*;
import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.json.JsonSchema.ArrayProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.Property;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityToJsonSchemaConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	private static final TypeDescriptor SCHEMA_TYPE = TypeDescriptor.valueOf(JsonSchema.class);

	private final Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
	private final ResourceMappings mappings;
	private final Repositories repositories;
	private final MessageSourceAccessor accessor;

	/**
	 * Creates a new {@link PersistentEntityToJsonSchemaConverter} for the given {@link Repositories} and
	 * {@link ResourceMappings}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param accessor
	 */
	public PersistentEntityToJsonSchemaConverter(Repositories repositories, ResourceMappings mappings,
			MessageSourceAccessor accessor) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.accessor = accessor;

		for (Class<?> domainType : repositories) {
			convertiblePairs.add(new ConvertiblePair(domainType, JsonSchema.class));
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

	public JsonSchema convert(Class<?> domainType) {
		return (JsonSchema) convert(domainType, STRING_TYPE, SCHEMA_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity((Class<?>) source);
		final ResourceMetadata metadata = mappings.getMappingFor(persistentEntity.getType());
		final JsonSchema jsonSchema = new JsonSchema(persistentEntity.getName(), accessor.getMessage(metadata
				.getItemResourceDescription()));

		persistentEntity.doWithProperties(new SimplePropertyHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.PropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
			 */
			@Override
			public void doWithPersistentProperty(PersistentProperty<?> persistentProperty) {

				Class<?> propertyType = persistentProperty.getType();
				String type = uncapitalize(propertyType.getSimpleName());

				ResourceMapping propertyMapping = metadata.getMappingFor(persistentProperty);

				boolean notNull = persistentProperty.isAnnotationPresent(NotNull.class);
				ResourceDescription description = propertyMapping.getDescription();
				String message = accessor.getMessage(description);

				Property property = persistentProperty.isCollectionLike() ? //
				new ArrayProperty("array", message, notNull)
						: new Property(type, message, notNull);

				jsonSchema.addProperty(persistentProperty.getName(), property);
			}
		});

		final List<Link> links = new ArrayList<Link>();

		persistentEntity.doWithAssociations(new SimpleAssociationHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.AssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
			 */
			@Override
			public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

				PersistentProperty<?> persistentProperty = association.getInverse();

				if (!metadata.isExported(persistentProperty)) {
					return;
				}

				RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, null).slash("{id}");
				maybeAddAssociationLink(builder, mappings, persistentProperty, links);
			}
		});

		jsonSchema.add(links);

		return jsonSchema;
	}
}
