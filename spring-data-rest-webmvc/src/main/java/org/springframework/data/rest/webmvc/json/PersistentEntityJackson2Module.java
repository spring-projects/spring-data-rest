/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.util.CastUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Jackson 2 module to serialize and deserialize {@link PersistentEntityResource}s.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Alex Leigh
 */
public class PersistentEntityJackson2Module extends SimpleModule {

	private static final long serialVersionUID = -7289265674870906323L;
	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);
	private static final TypeDescriptor URI_DESCRIPTOR = TypeDescriptor.valueOf(URI.class);

	/**
	 * Creates a new {@link PersistentEntityJackson2Module} using the given {@link Associations},
	 * {@link PersistentEntities}, {@link UriToEntityConverter}, {@link LinkCollector}, {@link RepositoryInvokerFactory},
	 * {@link LookupObjectSerializer}, {@link RepresentationModelProcessorInvoker} and {@link EmbeddedResourcesAssembler}.
	 *
	 * @param associations must not be {@literal null}.
	 * @param entities must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @param collector must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @param lookupObjectSerializer must not be {@literal null}.
	 * @param invoker must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 */
	public PersistentEntityJackson2Module(Associations associations, PersistentEntities entities,
			UriToEntityConverter converter, LinkCollector collector, RepositoryInvokerFactory factory,
			LookupObjectSerializer lookupObjectSerializer, RepresentationModelProcessorInvoker invoker,
			EmbeddedResourcesAssembler assembler) {

		super("persistent-entity-resource", new Version(2, 0, 0, null, "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(associations, "AssociationLinks must not be null");
		Assert.notNull(entities, "Repositories must not be null");
		Assert.notNull(converter, "UriToEntityConverter must not be null");
		Assert.notNull(collector, "LinkCollector must not be null");

		NestedEntitySerializer serializer = new NestedEntitySerializer(entities, assembler, invoker);
		addSerializer(new PersistentEntityResourceSerializer(collector));
		addSerializer(new ProjectionSerializer(collector, associations, invoker, false));
		addSerializer(new ProjectionResourceContentSerializer(false));

		setSerializerModifier(
				new AssociationOmittingSerializerModifier(entities, associations, serializer, lookupObjectSerializer));
		setDeserializerModifier(
				new AssociationUriResolvingDeserializerModifier(entities, associations, converter, factory));
	}

	/**
	 * Custom {@link JsonSerializer} for {@link PersistentEntityResource}s to turn associations into {@link Link}s.
	 * Delegates to standard {@link EntityModel} serialization afterwards.
	 *
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("serial")
	private static class PersistentEntityResourceSerializer extends StdSerializer<PersistentEntityResource> {

		private final LinkCollector collector;

		/**
		 * Creates a new {@link PersistentEntityResourceSerializer} using the given {@link PersistentEntities} and
		 * {@link Associations}.
		 *
		 * @param entities must not be {@literal null}.
		 */
		private PersistentEntityResourceSerializer(LinkCollector collector) {

			super(PersistentEntityResource.class);

			this.collector = collector;
		}

		@Override
		public void serialize(final PersistentEntityResource resource, final JsonGenerator jgen,
				final SerializerProvider provider) throws IOException, JsonGenerationException {

			LOG.debug("Serializing PersistentEntity {}", resource.getPersistentEntity());

			Object content = resource.getContent();

			if (hasScalarSerializer(content, provider)) {
				provider.defaultSerializeValue(content, jgen);
				return;
			}

			Links links = getLinks(resource);

			if (TargetAware.class.isInstance(content)) {

				TargetAware targetAware = (TargetAware) content;
				provider.defaultSerializeValue(new ProjectionResource(targetAware, links), jgen);
				return;
			}

			@SuppressWarnings("deprecation")
			EntityModel<Object> resourceToRender = new EntityModel<Object>(resource.getContent(), links) {

				@JsonUnwrapped
				public Iterable<?> getEmbedded() {
					return resource.getEmbeddeds();
				}
			};

			provider.defaultSerializeValue(resourceToRender, jgen);
		}

		private Links getLinks(PersistentEntityResource resource) {

			Object source = getLinkSource(resource.getContent());

			return resource.isNested() ? collector.getLinksForNested(source, resource.getLinks())
					: collector.getLinksFor(source, resource.getLinks());
		}

		private Object getLinkSource(Object object) {
			return TargetAware.class.isInstance(object) ? ((TargetAware) object).getTarget() : object;
		}

		private static boolean hasScalarSerializer(Object source, SerializerProvider provider) throws JsonMappingException {

			JsonSerializer<Object> serializer = provider.findValueSerializer(source.getClass());
			return serializer instanceof ToStringSerializer || serializer instanceof StdScalarSerializer;
		}
	}

	/**
	 * {@link BeanSerializerModifier} to drop the property descriptors for associations.
	 *
	 * @author Oliver Gierke
	 */
	static class AssociationOmittingSerializerModifier extends BeanSerializerModifier {

		private final PersistentEntities entities;
		private final Associations associations;
		private final NestedEntitySerializer nestedEntitySerializer;
		private final LookupObjectSerializer lookupObjectSerializer;

		public AssociationOmittingSerializerModifier(PersistentEntities entities, Associations associations,
				NestedEntitySerializer nestedEntitySerializer, LookupObjectSerializer lookupObjectSerializer) {

			Assert.notNull(entities, "PersistentEntities must not be null");
			Assert.notNull(associations, "Associations must not be null");
			Assert.notNull(nestedEntitySerializer, "NestedEntitySerializer must not be null");
			Assert.notNull(lookupObjectSerializer, "LookupObjectSerializer must not be null");

			this.entities = entities;
			this.associations = associations;
			this.nestedEntitySerializer = nestedEntitySerializer;
			this.lookupObjectSerializer = lookupObjectSerializer;
		}

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
				List<BeanPropertyWriter> beanProperties) {

			return entities.getPersistentEntity(beanDesc.getBeanClass()).map(entity -> {

				List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();

				for (BeanPropertyWriter writer : beanProperties) {

					Optional<? extends PersistentProperty<?>> findProperty = findProperty(writer.getName(), entity, beanDesc);

					if (!findProperty.isPresent()) {
						result.add(writer);
						continue;
					}

					findProperty.flatMap(it -> {

						if (associations.isLookupType(it)) {

							LOG.debug("Assigning lookup object serializer for {}", it);
							writer.assignSerializer(lookupObjectSerializer);

							return Optional.of(writer);
						}

						// Is there a default projection?

						if (associations.isLinkableAssociation(it)) {
							return Optional.empty();
						}

						// Skip ids unless explicitly configured to expose
						if (it.isIdProperty() && !associations.isIdExposed(entity)) {
							return Optional.empty();
						}

						if (it.isVersionProperty()) {
							return Optional.empty();
						}

						if (it.isEntity() && !writer.isUnwrapping()) {
							LOG.debug("Assigning nested entity serializer for {}", it);
							writer.assignSerializer(nestedEntitySerializer);
						}

						return Optional.of(writer);

					}).ifPresent(result::add);
				}

				return result;

			}).orElse(beanProperties);
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
		private Optional<? extends PersistentProperty<?>> findProperty(String finalName,
				PersistentEntity<?, ? extends PersistentProperty<?>> entity, BeanDescription description) {

			return description.findProperties().stream()//
					.filter(it -> it.getName().equals(finalName))//
					.findFirst().map(it -> entity.getPersistentProperty(it.getInternalName()));
		}
	}

	/**
	 * Serializer to wrap values into an {@link EntityModel} instance and collecting all association links.
	 *
	 * @author Oliver Gierke
	 * @author Alex Leigh
	 * @since 2.5
	 */
	static class NestedEntitySerializer extends StdSerializer<Object> {

		private static final long serialVersionUID = -2327469118972125954L;

		private final PersistentEntities entities;
		private final EmbeddedResourcesAssembler assembler;
		private final RepresentationModelProcessorInvoker invoker;

		public NestedEntitySerializer(PersistentEntities entities, EmbeddedResourcesAssembler assembler,
				RepresentationModelProcessorInvoker invoker) {

			super(Object.class);
			this.entities = entities;
			this.assembler = assembler;
			this.invoker = invoker;
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			if (value instanceof Collection) {

				Collection<?> source = (Collection<?>) value;
				List<Object> resources = new ArrayList<Object>();

				for (Object element : source) {
					resources.add(toModel(element, provider));
				}

				provider.defaultSerializeValue(resources, gen);

			} else if (value instanceof Map) {

				Map<?, ?> source = (Map<?, ?>) value;
				Map<Object, Object> resources = CollectionFactory.createApproximateMap(value.getClass(), source.size());

				for (Entry<?, ?> entry : source.entrySet()) {
					resources.put(entry.getKey(), toModel(entry.getValue(), provider));
				}

				provider.defaultSerializeValue(resources, gen);

			} else {
				provider.defaultSerializeValue(toModel(value, provider), gen);
			}
		}

		@Override
		public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
				TypeSerializer typeSerializer) throws IOException {
			serialize(value, gen, provider);
		}

		private Object toModel(Object value, SerializerProvider provider) throws JsonMappingException {

			JsonSerializer<Object> serializer = provider.findValueSerializer(value.getClass());

			if (JsonValueSerializer.class.isInstance(serializer)) {
				return value;
			}

			JsonSerializer<Object> unwrappingSerializer = serializer.unwrappingSerializer(NameTransformer.NOP);

			if (!unwrappingSerializer.isUnwrappingSerializer()) {
				return value;
			}

			PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(value.getClass());

			return invoker.invokeProcessorsFor(PersistentEntityResource.build(value, entity).//
					withEmbedded(assembler.getEmbeddedResources(value)).//
					buildNested());
		}
	}

	/**
	 * A {@link BeanDeserializerModifier} that registers a custom {@link UriStringDeserializer} for association properties
	 * of {@link PersistentEntity}s. This allows to submit URIs for those properties in request payloads, so that
	 * non-optional associations can be populated on resource creation.
	 *
	 * @author Oliver Gierke
	 * @author Lars Vierbergen
	 */
	public static class AssociationUriResolvingDeserializerModifier extends BeanDeserializerModifier {

		private final PersistentEntities entities;
		private final Associations associationLinks;
		private final UriToEntityConverter converter;
		private final RepositoryInvokerFactory factory;

		public AssociationUriResolvingDeserializerModifier(PersistentEntities entities, Associations associations,
				UriToEntityConverter converter, RepositoryInvokerFactory factory) {

			Assert.notNull(entities, "PersistentEntities must not be null");
			Assert.notNull(associations, "Associations must not be null");
			Assert.notNull(converter, "UriToEntityConverter must not be null");
			Assert.notNull(factory, "RepositoryInvokerFactory must not be null");

			this.entities = entities;
			this.associationLinks = associations;
			this.converter = converter;
			this.factory = factory;
		}

		@Override
		public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
				BeanDeserializerBuilder builder) {

			ValueInstantiatorCustomizer customizer = new ValueInstantiatorCustomizer(builder.getValueInstantiator(), config);
			Iterator<SettableBeanProperty> properties = builder.getProperties();

			entities.getPersistentEntity(beanDesc.getBeanClass()).ifPresent(entity -> {

				MappedProperties mapped = MappedProperties.forDescription(entity, beanDesc);

				while (properties.hasNext()) {

					SettableBeanProperty property = properties.next();
					PersistentProperty<?> persistentProperty = mapped.getPersistentProperty(property.getName());

					if (persistentProperty == null) {
						continue;
					}

					TypeInformation<?> propertyType = persistentProperty.getTypeInformation();

					if (associationLinks.isLookupType(persistentProperty)) {

						RepositoryInvokingDeserializer repositoryInvokingDeserializer = new RepositoryInvokingDeserializer(factory,
								persistentProperty);
						JsonDeserializer<?> deserializer = wrapIfCollection(propertyType, repositoryInvokingDeserializer, config);

						builder.addOrReplaceProperty(property.withValueDeserializer(deserializer), false);
						continue;
					}

					if (!associationLinks.isLinkableAssociation(persistentProperty)) {
						continue;
					}

					Class<?> actualPropertyType = persistentProperty.getActualType();
					UriStringDeserializer uriStringDeserializer = new UriStringDeserializer(actualPropertyType, converter);
					JsonDeserializer<?> deserializer = wrapIfCollection(propertyType, uriStringDeserializer, config);

					customizer.replacePropertyIfNeeded(builder, property.withValueDeserializer(deserializer));
				}
			});

			return customizer.conclude(builder);
		}

		/**
		 * Advanced customization of the {@link CreatorProperty} instances customized to additionally register them with the
		 * {@link ValueInstantiator} backing the {@link BeanDeserializerModifier}. This is necessary as the standard
		 * customization does not propagate into the initial object construction as the {@link CreatorProperty} instances
		 * for that are looked up via the {@link ValueInstantiator} and the property model behind those is not undergoing
		 * the customization currently (Jackson 2.9.9).
		 *
		 * @author Oliver Drotbohm
		 * @see https://github.com/FasterXML/jackson-databind/issues/2367
		 */
		public static class ValueInstantiatorCustomizer {

			public static final Field CONSTRUCTOR_ARGS_FIELD;

			private final SettableBeanProperty[] properties;
			private final StdValueInstantiator instantiator;

			static {

				CONSTRUCTOR_ARGS_FIELD = ReflectionUtils.findField(StdValueInstantiator.class, "_constructorArguments");
				ReflectionUtils.makeAccessible(CONSTRUCTOR_ARGS_FIELD);
			}

			ValueInstantiatorCustomizer(ValueInstantiator instantiator, DeserializationConfig config) {

				this.instantiator = StdValueInstantiator.class.isInstance(instantiator) //
						? StdValueInstantiator.class.cast(instantiator) //
						: null;

				this.properties = this.instantiator == null || this.instantiator.getFromObjectArguments(config) == null //
						? new SettableBeanProperty[0] //
						: this.instantiator.getFromObjectArguments(config).clone(); //
			}

			/**
			 * Replaces the logically same property with the given {@link SettableBeanProperty} on the given
			 * {@link BeanDeserializerBuilder}. In case we get a {@link CreatorProperty} we also register that one to be later
			 * exposed via the {@link ValueInstantiator} backing the {@link BeanDeserializerBuilder}.
			 *
			 * @param builder must not be {@literal null}.
			 * @param property must not be {@literal null}.
			 */
			void replacePropertyIfNeeded(BeanDeserializerBuilder builder, SettableBeanProperty property) {

				builder.addOrReplaceProperty(property, false);

				if (!CreatorProperty.class.isInstance(property)) {
					return;
				}

				properties[((CreatorProperty) property).getCreatorIndex()] = property;
			}

			/**
			 * Concludes the setup of the given {@link BeanDeserializerBuilder} by reflectively registering the potentially
			 * customized {@link SettableBeanProperty} instances in the {@link ValueInstantiator} backing the builder.
			 *
			 * @param builder must not be {@literal null}.
			 * @return
			 */
			BeanDeserializerBuilder conclude(BeanDeserializerBuilder builder) {

				if (instantiator == null) {
					return builder;
				}

				ReflectionUtils.setField(CONSTRUCTOR_ARGS_FIELD, instantiator, properties);

				builder.setValueInstantiator(instantiator);

				return builder;
			}
		}

		private static JsonDeserializer<?> wrapIfCollection(TypeInformation<?> property,
				JsonDeserializer<Object> elementDeserializer, DeserializationConfig config) {

			if (!property.isCollectionLike()) {
				return elementDeserializer;
			}

			CollectionLikeType collectionType = config.getTypeFactory().constructCollectionLikeType(property.getType(),
					property.getActualType().getType());
			CollectionValueInstantiator instantiator = new CollectionValueInstantiator(property);
			return new CollectionDeserializer(collectionType, elementDeserializer, null, instantiator);
		}
	}

	/**
	 * Custom {@link JsonDeserializer} to interpret {@link String} values as URIs and resolve them using a
	 * {@link UriToEntityConverter}.
	 *
	 * @author Oliver Gierke
	 * @author Valentin Rentschler
	 */
	public static class UriStringDeserializer extends StdDeserializer<Object> {

		private static final long serialVersionUID = -2175900204153350125L;
		private static final String UNEXPECTED_VALUE = "Expected URI cause property %s points to the managed domain type";

		private final Class<?> type;
		private final UriToEntityConverter converter;

		/**
		 * Creates a new {@link UriStringDeserializer} for the given {@link PersistentProperty} using the given
		 * {@link UriToEntityConverter}.
		 *
		 * @param type must not be {@literal null}.
		 * @param converter must not be {@literal null}.
		 */
		public UriStringDeserializer(Class<?> type, UriToEntityConverter converter) {

			super(type);

			this.type = type;
			this.converter = converter;
		}

		@Override
		public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			String source = jp.getValueAsString();

			if (!StringUtils.hasText(source)) {
				return null;
			}

			try {
				URI uri = UriTemplate.of(source).expand();
				TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(type);

				return converter.convert(uri, URI_DESCRIPTOR, typeDescriptor);
			} catch (IllegalArgumentException o_O) {
				throw ctxt.weirdStringException(source, URI.class, String.format(UNEXPECTED_VALUE, type));
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
		private final Associations associations;
		private final RepresentationModelProcessorInvoker invoker;
		private final boolean unwrapping;

		/**
		 * Creates a new {@link ProjectionSerializer} for the given {@link LinkCollector}, {@link ResourceMappings} whether
		 * to be in unwrapping mode or not.
		 *
		 * @param collector must not be {@literal null}.
		 * @param mappings must not be {@literal null}.
		 * @param invoker must not be {@literal null}.
		 * @param unwrapping
		 */
		ProjectionSerializer(LinkCollector collector, Associations mappings,
				RepresentationModelProcessorInvoker invoker, boolean unwrapping) {

			super(TargetAware.class);

			this.collector = collector;
			this.associations = mappings;
			this.invoker = invoker;
			this.unwrapping = unwrapping;
		}

		@Override
		public void serialize(TargetAware value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonGenerationException {

			if (!unwrapping) {
				jgen.writeStartObject();
			}

			provider.//
					findValueSerializer(ProjectionResource.class, null).//
					unwrappingSerializer(null).//
					serialize(toModel(value), jgen, provider);

			if (!unwrapping) {
				jgen.writeEndObject();
			}
		}

		@Override
		public boolean isUnwrappingSerializer() {
			return unwrapping;
		}

		@Override
		public JsonSerializer<TargetAware> unwrappingSerializer(NameTransformer unwrapper) {
			return new ProjectionSerializer(collector, associations, invoker, true);
		}

		/**
		 * Creates a {@link ProjectionResource} for the given {@link TargetAware}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		ProjectionResource toModel(TargetAware value) {

			Object target = value.getTarget();
			ResourceMetadata metadata = associations.getMetadataFor(value.getTargetClass());
			Links links = metadata != null && metadata.isExported() ? collector.getLinksFor(target) : Links.NONE;

			EntityModel<TargetAware> resource = invoker.invokeProcessorsFor(EntityModel.of(value, links));

			return new ProjectionResource(resource.getContent(), resource.getLinks());
		}
	}

	static class ProjectionResource extends EntityModel<ProjectionResourceContent> {

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

		private final boolean unwrapping;

		/**
		 * Creates a new {@link ProjectionResourceContentSerializer}.
		 *
		 * @param unwrapping whether to expose the unwrapping state.
		 */
		public ProjectionResourceContentSerializer(boolean unwrapping) {

			super(ProjectionResourceContent.class);
			this.unwrapping = unwrapping;
		}

		@Override
		public void serialize(ProjectionResourceContent value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonGenerationException {

			provider.//
					findValueSerializer(value.getProjectionInterface(), null).//
					unwrappingSerializer(null).//
					serialize(value.getProjection(), jgen, provider);
		}

		@Override
		public boolean isUnwrappingSerializer() {
			return unwrapping;
		}

		@Override
		public JsonSerializer<ProjectionResourceContent> unwrappingSerializer(NameTransformer unwrapper) {
			return new ProjectionResourceContentSerializer(true);
		}
	}

	/**
	 * {@link ValueInstantiator} to create collection or map instances based on the type of the configured
	 * {@link PersistentProperty}.
	 *
	 * @author Oliver Gierke
	 */
	static class CollectionValueInstantiator extends ValueInstantiator {

		private final TypeInformation<?> property;

		/**
		 * Creates a new {@link CollectionValueInstantiator} for the given {@link PersistentProperty}.
		 *
		 * @param property must not be {@literal null} and must be a collection.
		 */
		public CollectionValueInstantiator(TypeInformation<?> property) {

			Assert.notNull(property, "Property must not be null");
			Assert.isTrue(property.isCollectionLike() || property.isMap(), "Property must be a collection or map property");

			this.property = property;
		}

		@Override
		public String getValueTypeDesc() {
			return property.getType().getName();
		}

		@Override
		public Object createUsingDefault(DeserializationContext ctxt) throws IOException, JsonProcessingException {

			Class<?> collectionOrMapType = property.getType();

			return property.isMap() ? CollectionFactory.createMap(collectionOrMapType, 0)
					: CollectionFactory.createCollection(collectionOrMapType, 0);
		}
	}

	private static class RepositoryInvokingDeserializer extends StdScalarDeserializer<Object> {

		private static final long serialVersionUID = -3033458643050330913L;
		private final RepositoryInvoker invoker;

		private RepositoryInvokingDeserializer(RepositoryInvokerFactory factory, PersistentProperty<?> property) {

			super(property.getActualType());
			this.invoker = factory.getInvokerFor(_valueClass);
		}

		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			Object id = p.getCurrentToken().isNumeric() ? p.getValueAsLong() : p.getValueAsString();

			return invoker.invokeFindById(id).orElse(null);
		}
	}

	public static class LookupObjectSerializer extends ToStringSerializer {

		private static final long serialVersionUID = -3033458643050330913L;

		private final PluginRegistry<EntityLookup<?>, Class<?>> lookups;

		public LookupObjectSerializer(PluginRegistry<EntityLookup<?>, Class<?>> lookups) {

			Assert.notNull(lookups, "EntityLookups must not be null");

			this.lookups = lookups;
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			if (value instanceof Collection) {

				gen.writeStartArray();

				for (Object element : (Collection<?>) value) {
					gen.writeObject(getLookupKey(element));
				}

				gen.writeEndArray();

			} else {
				gen.writeObject(getLookupKey(value));
			}
		}

		private Object getLookupKey(Object value) {

			return lookups.getPluginFor(value.getClass()) //
					.<EntityLookup<Object>> map(CastUtils::cast)
					.orElseThrow(() -> new IllegalArgumentException("No EntityLookup found for " + value.getClass().getName()))
					.getResourceIdentifier(value);
		}
	}
}
