package org.springframework.data.rest.webmvc.json;

import static org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.*;
import static org.springframework.util.StringUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.annotation.Description;
import org.springframework.data.rest.repository.mapping.ResourceMappings;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityToJsonSchemaConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	private static final TypeDescriptor SCHEMA_TYPE = TypeDescriptor.valueOf(JsonSchema.class);

	private final Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
	private final ResourceMappings mappings;
	private final Repositories repositories;

	/**
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	public PersistentEntityToJsonSchemaConverter(Repositories repositories, ResourceMappings mappings) {

		this.repositories = repositories;
		this.mappings = mappings;

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity((Class<?>) source);
		final ResourceMetadata metadata = mappings.getMappingFor(persistentEntity.getClass());
		String entityDesc = persistentEntity.getType().isAnnotationPresent(Description.class) ? persistentEntity.getType()
				.getAnnotation(Description.class).value() : null;

		final JsonSchema jsonSchema = new JsonSchema(persistentEntity.getName(), entityDesc);
		persistentEntity.doWithProperties(new PropertyHandler() {
			@Override
			public void doWithPersistentProperty(PersistentProperty persistentProperty) {
				Class<?> propertyType = persistentProperty.getType();
				String type = uncapitalize(propertyType.getSimpleName());
				boolean notNull = persistentProperty.getField().isAnnotationPresent(Nonnull.class)
						|| persistentProperty.getGetter().isAnnotationPresent(Nonnull.class)
						|| persistentProperty.getField().isAnnotationPresent(NotNull.class)
						|| persistentProperty.getGetter().isAnnotationPresent(NotNull.class);
				String desc = persistentProperty.getField().isAnnotationPresent(Description.class) ? persistentProperty
						.getField().getAnnotation(Description.class).value() : persistentProperty.getGetter().isAnnotationPresent(
						Description.class) ? persistentProperty.getGetter().getAnnotation(Description.class).value() : null;

				JsonSchema.Property property;
				if (persistentProperty.isCollectionLike()) {
					property = new JsonSchema.ArrayProperty("array", desc, notNull);
				} else {
					property = new JsonSchema.Property(type, desc, notNull);
				}
				jsonSchema.addProperty(persistentProperty.getName(), property);
			}
		});

		final List<Link> links = new ArrayList<Link>();

		persistentEntity.doWithAssociations(new AssociationHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.AssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
			 */
			@Override
			public void doWithAssociation(Association association) {

				PersistentProperty persistentProperty = association.getInverse();

				if (!metadata.isExported(persistentProperty)) {
					return;
				}

				RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, URI.create("{id}"));
				maybeAddAssociationLink(builder, mappings, persistentProperty, links);
			}
		});

		jsonSchema.add(links);

		return jsonSchema;
	}
}
