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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;

/**
 * Jackson 2 module to serialize and deserialize {@link PersistentEntityResource}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class PersistentEntityJackson2Module extends SimpleModule {

	private static final long serialVersionUID = -7289265674870906323L;
	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);
	private static final TypeDescriptor URI_DESCRIPTOR = TypeDescriptor.valueOf(URI.class);

	/**
	 * Creates a new {@link PersistentEntityJackson2Module} using the given {@link ResourceMappings}, {@link Repositories}
	 * , {@link RepositoryRestConfiguration} and {@link UriToEntityConverter}.
	 * 
	 * @param mappings must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 */
	public PersistentEntityJackson2Module(ResourceMappings mappings, Repositories repositories,
			RepositoryRestConfiguration config, UriToEntityConverter converter) {

		super(new Version(2, 0, 0, null, "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(converter, "UriToEntityConverter must not be null!");

		addSerializer(new PersistentEntityResourceSerializer(mappings, config));
		setSerializerModifier(new AssociationOmittingSerializerModifier(repositories, mappings, config));
		setDeserializerModifier(new AssociationUriResolvingDeserializerModifier(repositories, converter, mappings));
	}

	public static boolean maybeAddAssociationLink(RepositoryLinkBuilder builder, ResourceMappings mappings,
			PersistentProperty<?> persistentProperty, List<Link> links) {

		Assert.isTrue(persistentProperty.isAssociation(), "PersistentProperty must be an association!");
		ResourceMetadata ownerMetadata = mappings.getMappingFor(persistentProperty.getOwner().getType());

		if (!ownerMetadata.isManagedResource(persistentProperty)) {
			return false;
		}

		ResourceMapping propertyMapping = ownerMetadata.getMappingFor(persistentProperty);

		if (propertyMapping.isExported()) {
			links.add(builder.slash(propertyMapping.getPath()).withRel(propertyMapping.getRel()));
			// This is an association. We added a Link.
			return true;
		}

		// This is not an association. No Link was added.
		return false;
	}

	/**
	 * Custom {@link JsonSerializer} for {@link PersistentEntityResource}s to turn associations into {@link Link}s.
	 * Delegates to standard {@link Resource} serialization afterwards.
	 * 
	 * @author Oliver Gierke
	 */
	private static class PersistentEntityResourceSerializer extends StdSerializer<PersistentEntityResource<?>> {

		private final ResourceMappings mappings;
		private final RepositoryRestConfiguration configuration;

		/**
		 * Creates a new {@link PersistentEntityResourceSerializer} using the given {@link ResourceMappings} and
		 * {@link RepositoryRestConfiguration}.
		 * 
		 * @param mappings must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private PersistentEntityResourceSerializer(ResourceMappings mappings, RepositoryRestConfiguration configuration) {

			super((Class) PersistentEntityResource.class);

			Assert.notNull(mappings, "ResourceMappings must not be null!");
			Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

			this.mappings = mappings;
			this.configuration = configuration;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(final PersistentEntityResource<?> resource, final JsonGenerator jgen,
				final SerializerProvider provider) throws IOException, JsonGenerationException {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Serializing PersistentEntity " + resource.getPersistentEntity());
			}

			Object obj = resource.getContent();
			PersistentEntity<?, ?> entity = resource.getPersistentEntity();
			BeanWrapper<PersistentEntity<Object, ?>, Object> wrapper = BeanWrapper.create(obj, null);
			Object entityId = wrapper.getProperty(entity.getIdProperty());
			ResourceMetadata metadata = mappings.getMappingFor(entity.getType());
			URI baseUri = configuration.getBaseUri();

			final RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, baseUri).slash(entityId);
			final List<Link> links = new ArrayList<Link>();
			links.addAll(resource.getLinks());

			// Add associations as links
			entity.doWithAssociations(new SimpleAssociationHandler() {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
				 */
				@Override
				public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

					PersistentProperty<?> property = association.getInverse();

					if (maybeAddAssociationLink(builder, mappings, property, links)) {
						return;
					}
				}
			});

			Resource<Object> resourceToRender = new Resource<Object>(obj, links);
			provider.defaultSerializeValue(resourceToRender, jgen);
		}
	}

	/**
	 * {@link BeanSerializerModifier} to drop the property descriptors for associations.
	 * 
	 * @author Oliver Gierke
	 */
	private static class AssociationOmittingSerializerModifier extends BeanSerializerModifier {

		private final Repositories repositories;
		private final ResourceMappings mappings;
		private final RepositoryRestConfiguration configuration;

		/**
		 * Creates a new {@link AssociationOmittingSerializerModifier} for the given {@link Repositories},
		 * {@link ResourceMappings} and {@link RepositoryRestConfiguration}.
		 * 
		 * @param repositories must not be {@literal null}.
		 * @param mappings must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 */
		private AssociationOmittingSerializerModifier(Repositories repositories, ResourceMappings mappings,
				RepositoryRestConfiguration configuration) {

			this.repositories = repositories;
			this.mappings = mappings;
			this.configuration = configuration;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.BeanSerializerModifier#updateBuilder(com.fasterxml.jackson.databind.SerializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.ser.BeanSerializerBuilder)
		 */
		@Override
		public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc,
				BeanSerializerBuilder builder) {

			PersistentEntity<?, ?> entity = repositories.getPersistentEntity(beanDesc.getBeanClass());

			if (entity == null) {
				return builder;
			}

			List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();
			ResourceMetadata resourceMetadata = mappings.getMappingFor(entity.getType());

			for (BeanPropertyWriter writer : builder.getProperties()) {

				PersistentProperty<?> persistentProperty = entity.getPersistentProperty(writer.getName());

				if (persistentProperty.isAssociation()) {

					if (!resourceMetadata.isManagedResource(persistentProperty)) {
						continue;
					}

					if (mappings.getMappingFor(persistentProperty.getActualType()).isExported()) {
						continue;
					}

					ResourceMapping propertyMapping = resourceMetadata.getMappingFor(persistentProperty);

					if (!propertyMapping.isExported()) {
						continue;
					}
				}

				if (persistentProperty.isIdProperty() && !configuration.isIdExposedFor(entity.getType())) {
					continue;
				}

				result.add(writer);
			}

			builder.setProperties(result);

			return builder;
		}
	}

	/**
	 * A {@link BeanDeserializerModifier} that registers a custom {@link UriStringDeserializer} for association properties
	 * of {@link PersistentEntity}s. This allows to submit URIs for those properties in request payloads, so that
	 * non-optional associations can be populated on resource creation.
	 * 
	 * @author Oliver Gierke
	 */
	private static class AssociationUriResolvingDeserializerModifier extends BeanDeserializerModifier {

		private final UriToEntityConverter converter;
		private final Repositories repositories;
		private final ResourceMappings mappings;

		/**
		 * Creates a new {@link AssociationUriResolvingDeserializerModifier} using the given {@link Repositories},
		 * {@link UriToEntityConverter} and {@link ResourceMappings}.
		 * 
		 * @param repositories must not be {@literal null}.
		 * @param converter must not be {@literal null}.
		 * @param mappings must not be {@literal null}.
		 */
		public AssociationUriResolvingDeserializerModifier(Repositories repositories, UriToEntityConverter converter,
				ResourceMappings mappings) {

			Assert.notNull(repositories, "Repositories must not be null!");
			Assert.notNull(converter, "UriToEntityConverter must not be null!");
			Assert.notNull(mappings, "ResourceMappings must not be null!");

			this.repositories = repositories;
			this.converter = converter;
			this.mappings = mappings;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.BeanDeserializerModifier#updateBuilder(com.fasterxml.jackson.databind.DeserializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder)
		 */
		@Override
		public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
				BeanDeserializerBuilder builder) {

			Iterator<SettableBeanProperty> properties = builder.getProperties();
			PersistentEntity<?, ?> entity = repositories.getPersistentEntity(beanDesc.getBeanClass());
			ResourceMetadata metadata = mappings.getMappingFor(beanDesc.getBeanClass());

			if (entity == null) {
				return builder;
			}

			while (properties.hasNext()) {

				SettableBeanProperty property = properties.next();
				PersistentProperty<?> persistentProperty = entity.getPersistentProperty(property.getName());
				ResourceMapping propertyMapping = metadata.getMappingFor(persistentProperty);

				if (!persistentProperty.isAssociation() || !propertyMapping.isExported()) {
					continue;
				}

				UriStringDeserializer uriStringDeserializer = new UriStringDeserializer(persistentProperty, converter);

				if (persistentProperty.isCollectionLike()) {

					CollectionLikeType collectionType = config.getTypeFactory().constructCollectionLikeType(
							persistentProperty.getType(), persistentProperty.getActualType());
					CollectionValueInstantiator instantiator = new CollectionValueInstantiator(persistentProperty);
					CollectionDeserializer collectionDeserializer = new CollectionDeserializer(collectionType,
							uriStringDeserializer, null, instantiator);

					builder.addOrReplaceProperty(property.withValueDeserializer(collectionDeserializer), false);

				} else {
					builder.addOrReplaceProperty(property.withValueDeserializer(uriStringDeserializer), false);
				}
			}

			return builder;
		}
	}

	/**
	 * Custom {@link JsonDeserializer} to interpret {@link String} values as URIs and resolve them using a
	 * {@link UriToEntityConverter}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class UriStringDeserializer extends StdDeserializer<Object> {

		private static final long serialVersionUID = -2175900204153350125L;

		private final PersistentProperty<?> property;
		private final UriToEntityConverter converter;

		/**
		 * Creates a new {@link UriStringDeserializer} for the given {@link PersistentProperty} using the given
		 * {@link UriToEntityConverter}.
		 * 
		 * @param property must not be {@literal null}.
		 * @param converter must not be {@literal null}.
		 */
		public UriStringDeserializer(PersistentProperty<?> property, UriToEntityConverter converter) {

			super(property.getActualType());

			this.property = property;
			this.converter = converter;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			String uriString = jp.getValueAsString();
			TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(property.getActualType());

			return converter.convert(URI.create(uriString), URI_DESCRIPTOR, typeDescriptor);
		}
	}

	/**
	 * {@link ValueInstantiator} to create collection or map instances based on the type of the configured
	 * {@link PersistentProperty}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class CollectionValueInstantiator extends ValueInstantiator {

		private final PersistentProperty<?> property;

		/**
		 * Creates a new {@link CollectionValueInstantiator} for the given {@link PersistentProperty}.
		 * 
		 * @param property must not be {@literal null} and must be a collection.
		 */
		public CollectionValueInstantiator(PersistentProperty<?> property) {

			Assert.notNull(property, "Property must not be null!");
			Assert.isTrue(property.isCollectionLike() || property.isMap(), "Property must be a collection or map property!");

			this.property = property;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.ValueInstantiator#getValueTypeDesc()
		 */
		@Override
		public String getValueTypeDesc() {
			return property.getType().getName();
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.ValueInstantiator#createUsingDefault(com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public Object createUsingDefault(DeserializationContext ctxt) throws IOException, JsonProcessingException {

			Class<?> collectionOrMapType = property.getType();

			return property.isMap() ? CollectionFactory.createMap(collectionOrMapType, 0) : CollectionFactory
					.createCollection(collectionOrMapType, 0);
		}
	}
}
