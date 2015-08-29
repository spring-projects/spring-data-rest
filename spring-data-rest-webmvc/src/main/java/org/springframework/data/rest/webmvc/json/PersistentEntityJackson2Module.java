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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.mapping.AssociationLinks;
import org.springframework.data.rest.webmvc.mapping.LinkCollectingAssociationHandler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.util.NameTransformer;

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
	 * @param entities must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 */
	public PersistentEntityJackson2Module(ResourceMappings mappings, PersistentEntities entities,
			RepositoryRestConfiguration config, UriToEntityConverter converter, EntityLinks entityLinks) {

		super(new Version(2, 0, 0, null, "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(entities, "Repositories must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(converter, "UriToEntityConverter must not be null!");

		AssociationLinks associationLinks = new AssociationLinks(mappings);
		LinkCollector collector = new LinkCollector(entities, entityLinks, associationLinks);

		addSerializer(new PersistentEntityResourceSerializer(collector));
		addSerializer(new ProjectionSerializer(collector, mappings));
		addSerializer(new ProjectionResourceContentSerializer());

		setSerializerModifier(new AssociationOmittingSerializerModifier(entities, associationLinks, config));
		setDeserializerModifier(new AssociationUriResolvingDeserializerModifier(entities, converter, associationLinks));
	}

	/**
	 * Custom {@link JsonSerializer} for {@link PersistentEntityResource}s to turn associations into {@link Link}s.
	 * Delegates to standard {@link Resource} serialization afterwards.
	 * 
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("serial")
	private static class PersistentEntityResourceSerializer extends StdSerializer<PersistentEntityResource> {

		private final LinkCollector collector;

		/**
		 * Creates a new {@link PersistentEntityResourceSerializer} using the given {@link PersistentEntities} and
		 * {@link AssociationLinks}.
		 * 
		 * @param entities must not be {@literal null}.
		 * @param links must not be {@literal null}.
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private PersistentEntityResourceSerializer(LinkCollector collector) {

			super((Class) PersistentEntityResource.class);

			this.collector = collector;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(final PersistentEntityResource resource, final JsonGenerator jgen,
				final SerializerProvider provider) throws IOException, JsonGenerationException {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Serializing PersistentEntity " + resource.getPersistentEntity());
			}

			Object content = resource.getContent();

			if (TargetAware.class.isInstance(content)) {

				TargetAware targetAware = (TargetAware) content;
				Links links = collector.getLinksFor(targetAware.getTarget(), resource.getLinks());
				provider.defaultSerializeValue(new ProjectionResource(targetAware, links), jgen);
				return;
			}

			Links links = collector.getLinksFor(resource.getContent(), resource.getLinks());

			Resource<Object> resourceToRender = new Resource<Object>(resource.getContent(), links) {

				@JsonUnwrapped
				public Iterable<?> getEmbedded() {
					return resource.getEmbeddeds();
				}
			};

			provider.defaultSerializeValue(resourceToRender, jgen);
		}
	}

	/**
	 * {@link BeanSerializerModifier} to drop the property descriptors for associations.
	 * 
	 * @author Oliver Gierke
	 */
	static class AssociationOmittingSerializerModifier extends BeanSerializerModifier {

		private final PersistentEntities entities;
		private final RepositoryRestConfiguration configuration;
		private final AssociationLinks associationLinks;

		/**
		 * Creates a new {@link AssociationOmittingSerializerModifier} for the given {@link PersistentEntities},
		 * {@link AssociationLinks} and {@link RepositoryRestConfiguration}.
		 * 
		 * @param entities must not be {@literal null}.
		 * @param associationLinks must not be {@literal null}.
		 * @param configuration must not be {@literal null}.
		 */
		public AssociationOmittingSerializerModifier(PersistentEntities entities, AssociationLinks associationLinks,
				RepositoryRestConfiguration configuration) {

			Assert.notNull(entities, "PersistentEntities must not be null!");
			Assert.notNull(associationLinks, "AssociationLinks must not be null!");
			Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

			this.entities = entities;
			this.configuration = configuration;
			this.associationLinks = associationLinks;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.BeanSerializerModifier#updateBuilder(com.fasterxml.jackson.databind.SerializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.ser.BeanSerializerBuilder)
		 */
		@Override
		public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc,
				BeanSerializerBuilder builder) {

			PersistentEntity<?, ?> entity = entities.getPersistentEntity(beanDesc.getBeanClass());

			if (entity == null) {
				return builder;
			}

			List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();

			for (BeanPropertyWriter writer : builder.getProperties()) {

				// Skip exported associations
				PersistentProperty<?> persistentProperty = findProperty(writer.getName(), entity, beanDesc);

				if (persistentProperty == null) {
					result.add(writer);
					continue;
				}

				// Is there a default projection?

				if (associationLinks.isLinkableAssociation(persistentProperty)) {
					continue;
				}

				// Skip ids unless explicitly configured to expose
				if (persistentProperty.isIdProperty() && !configuration.isIdExposedFor(entity.getType())) {
					continue;
				}

				if (persistentProperty.isVersionProperty()) {
					continue;
				}

				result.add(writer);
			}

			builder.setProperties(result);

			return builder;
		}

		/**
		 * Returns the {@link PersistentProperty} for the property with the given final name (the name that it will be
		 * rendered under eventually).
		 * 
		 * @param finalName the output name the property will be rendered under.
		 * @param entity the {@link PersistentEntity} to find the property on.
		 * @param description the Jackson {@link BeanDescription}.
		 * @return
		 */
		private PersistentProperty<?> findProperty(String finalName, PersistentEntity<?, ?> entity,
				BeanDescription description) {

			for (BeanPropertyDefinition definition : description.findProperties()) {
				if (definition.getName().equals(finalName)) {
					return entity.getPersistentProperty(definition.getInternalName());
				}
			}

			return null;
		}
	}

	/**
	 * A {@link BeanDeserializerModifier} that registers a custom {@link UriStringDeserializer} for association properties
	 * of {@link PersistentEntity}s. This allows to submit URIs for those properties in request payloads, so that
	 * non-optional associations can be populated on resource creation.
	 * 
	 * @author Oliver Gierke
	 */
	public static class AssociationUriResolvingDeserializerModifier extends BeanDeserializerModifier {

		private final UriToEntityConverter converter;
		private final PersistentEntities repositories;
		private final AssociationLinks associationLinks;

		/**
		 * Creates a new {@link AssociationUriResolvingDeserializerModifier} using the given {@link Repositories},
		 * {@link UriToEntityConverter} and {@link AssociationLinks}.
		 * 
		 * @param repositories must not be {@literal null}.
		 * @param converter must not be {@literal null}.
		 * @param mappings must not be {@literal null}.
		 */
		public AssociationUriResolvingDeserializerModifier(PersistentEntities repositories, UriToEntityConverter converter,
				AssociationLinks associationLinks) {

			Assert.notNull(repositories, "Repositories must not be null!");
			Assert.notNull(converter, "UriToEntityConverter must not be null!");
			Assert.notNull(associationLinks, "AssociationLinks must not be null!");

			this.repositories = repositories;
			this.converter = converter;
			this.associationLinks = associationLinks;
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

			if (entity == null) {
				return builder;
			}

			while (properties.hasNext()) {

				SettableBeanProperty property = properties.next();
				PersistentProperty<?> persistentProperty = entity.getPersistentProperty(property.getName());

				if (!associationLinks.isLinkableAssociation(persistentProperty)) {
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
	 * @author Valentin Rentschler
	 */
	static class UriStringDeserializer extends StdDeserializer<Object> {

		private static final long serialVersionUID = -2175900204153350125L;
		private static final String UNEXPECTED_VALUE = "Expected URI cause property %s points to the managed domain type!";

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

			String source = jp.getValueAsString();

			if (!StringUtils.hasText(source)) {
				return null;
			}

			try {
				URI uri = new UriTemplate(source).expand();
				TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(property.getActualType());

				return converter.convert(uri, URI_DESCRIPTOR, typeDescriptor);
			} catch (IllegalArgumentException o_O) {
				throw ctxt.weirdStringException(source, URI.class, String.format(UNEXPECTED_VALUE, property));
			}
		}

		/**
		 * Deserialize by ignoring the {@link TypeDeserializer}, as URIs will either resolve to {@literal null} or a
		 * concrete instance anyway.
		 * 
		 * @see com.fasterxml.jackson.databind.deser.std.StdDeserializer#deserializeWithType(com.fasterxml.jackson.core.JsonParser,
		 *      com.fasterxml.jackson.databind.DeserializationContext,
		 *      com.fasterxml.jackson.databind.jsontype.TypeDeserializer)
		 */
		@Override
		public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
				throws IOException {
			return deserialize(jp, ctxt);
		}
	}

	@SuppressWarnings("serial")
	static class ProjectionSerializer extends StdSerializer<TargetAware> {

		private final LinkCollector collector;
		private final ResourceMappings mappings;
		private boolean unwrapping;

		/**
		 * Creates a new {@link ProjectionSerializer} for the given {@link LinkCollector}.
		 * 
		 * @param collector
		 */
		public ProjectionSerializer(LinkCollector collector, ResourceMappings mappings) {

			super(TargetAware.class);

			this.collector = collector;
			this.mappings = mappings;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(TargetAware value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
				JsonGenerationException {

			Object target = value.getTarget();
			Links links = mappings.getMetadataFor(value.getTargetClass()).isExported() ? collector.getLinksFor(target)
					: new Links();

			jgen.writeStartObject();

			provider.//
					findValueSerializer(ProjectionResource.class, null).//
					unwrappingSerializer(null).//
					serialize(new ProjectionResource(value, links), jgen, provider);

			jgen.writeEndObject();
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#isUnwrappingSerializer()
		 */
		@Override
		public boolean isUnwrappingSerializer() {
			return unwrapping;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#unwrappingSerializer(com.fasterxml.jackson.databind.util.NameTransformer)
		 */
		@Override
		public JsonSerializer<TargetAware> unwrappingSerializer(NameTransformer unwrapper) {
			this.unwrapping = true;
			return this;
		}
	}

	static class ProjectionResource extends Resource<ProjectionResourceContent> {

		ProjectionResource(TargetAware projection, Iterable<Link> links) {
			super(new ProjectionResourceContent(projection, projection.getClass().getInterfaces()[0]), links);
		}
	}

	static class ProjectionResourceContent {

		private final Object projection;
		private final Class<?> projectionInterface;

		/**
		 * @param projection
		 * @param projectionInterface
		 */
		public ProjectionResourceContent(Object projection, Class<?> projectionInterface) {
			this.projection = projection;
			this.projectionInterface = projectionInterface;
		}

		public Object getProjection() {
			return projection;
		}

		public Class<?> getProjectionInterface() {
			return projectionInterface;
		}
	}

	@SuppressWarnings("serial")
	private static class ProjectionResourceContentSerializer extends StdSerializer<ProjectionResourceContent> {

		private boolean unwrapping;

		/**
		 * Creates a new {@link ProjectionResourceContentSerializer}.
		 */
		public ProjectionResourceContentSerializer() {
			super(ProjectionResourceContent.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(ProjectionResourceContent value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonGenerationException {

			provider.//
					findValueSerializer(value.getProjectionInterface(), null).//
					unwrappingSerializer(null).//
					serialize(value.getProjection(), jgen, provider);
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#isUnwrappingSerializer()
		 */
		@Override
		public boolean isUnwrappingSerializer() {
			return unwrapping;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#unwrappingSerializer(com.fasterxml.jackson.databind.util.NameTransformer)
		 */
		@Override
		public JsonSerializer<ProjectionResourceContent> unwrappingSerializer(NameTransformer unwrapper) {

			this.unwrapping = true;
			return this;
		}
	}

	/**
	 * A service to collect all standard links that need to be added to a certain object.
	 *
	 * @author Oliver Gierke
	 */
	private static class LinkCollector {

		private final PersistentEntities entities;
		private final AssociationLinks associationLinks;
		private final EntityLinks links;

		/**
		 * Creates a new {@link PersistentEntities}, {@link EntityLinks} and {@link AssociationLinks}.
		 * 
		 * @param entities must not be {@literal null}.
		 * @param entityLinks must not be {@literal null}.
		 * @param associationLinks must not be {@literal null}.
		 */
		public LinkCollector(PersistentEntities entities, EntityLinks entityLinks, AssociationLinks associationLinks) {

			Assert.notNull(entities, "PersistentEntities must not be null!");
			Assert.notNull(entityLinks, "EntityLinks must not be null!");
			Assert.notNull(associationLinks, "AssociationLinks must not be null!");

			this.links = entityLinks;
			this.entities = entities;
			this.associationLinks = associationLinks;
		}

		/**
		 * Returns all {@link Links} for the given object.
		 * 
		 * @param object must not be {@literal null}.
		 * @return
		 */
		public Links getLinksFor(Object object) {
			return getLinksFor(object, Collections.<Link> emptyList());
		}

		/**
		 * Returns all {@link Links} for the given object and already existing {@link Link}.
		 * 
		 * @param object must not be {@literal null}.
		 * @param existingLinks must not be {@literal null}.
		 * @return
		 */
		public Links getLinksFor(Object object, List<Link> existingLinks) {

			Assert.notNull(object, "Object must not be null!");
			Assert.notNull(existingLinks, "Existing links must not be null!");

			PersistentEntity<?, ?> entity = entities.getPersistentEntity(object.getClass());

			Links links = new Links(existingLinks);
			Link selfLink = createSelfLink(object, entity, links);

			if (selfLink == null) {
				return links;
			}

			Path path = new Path(selfLink.expand().getHref());

			LinkCollectingAssociationHandler handler = new LinkCollectingAssociationHandler(entities, path, associationLinks);
			entity.doWithAssociations(handler);

			List<Link> result = new ArrayList<Link>(existingLinks);
			result.addAll(handler.getLinks());

			return addSelfLinkIfNecessary(object, entity, result);
		}

		private Links addSelfLinkIfNecessary(Object object, PersistentEntity<?, ?> entity, List<Link> existing) {

			Links result = new Links(existing);

			if (result.hasLink(Link.REL_SELF)) {
				return result;
			}

			List<Link> list = new ArrayList<Link>();
			list.add(createSelfLink(object, entity, result));
			list.addAll(existing);

			return new Links(list);
		}

		private Link createSelfLink(Object object, PersistentEntity<?, ?> entity, Links existing) {

			if (existing.hasLink(Link.REL_SELF)) {
				return existing.getLink(Link.REL_SELF);
			}

			IdentifierAccessor accessor = entity.getIdentifierAccessor(object);
			Object identifier = accessor.getIdentifier();

			if (identifier == null) {
				return null;
			}

			return links.linkToSingleResource(entity.getType(), identifier).withSelfRel();
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
