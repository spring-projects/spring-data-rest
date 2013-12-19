package org.springframework.data.rest.webmvc.json;

import static org.springframework.beans.BeanUtils.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriDomainClassConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityJackson2Module extends SimpleModule implements InitializingBean {

	private static final long serialVersionUID = -7289265674870906323L;
	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);
	private static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);

	private final ResourceMappings mappings;
	private final ConversionService conversionService;

	@Autowired private Repositories repositories;
	@Autowired private RepositoryRestConfiguration config;
	@Autowired private UriDomainClassConverter uriDomainClassConverter;

	public PersistentEntityJackson2Module(ResourceMappings resourceMappings, ConversionService conversionService) {

		super(new Version(1, 1, 0, "BUILD-SNAPSHOT", "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(resourceMappings, "ResourceMappings must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.mappings = resourceMappings;
		this.conversionService = conversionService;

		addSerializer(new ResourceSerializer());
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void afterPropertiesSet() throws Exception {
		for (Class<?> domainType : repositories) {
			PersistentEntity<?, ?> pe = repositories.getPersistentEntity(domainType);
			if (null == pe) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("The domain class {} does not have PersistentEntity metadata.", domainType.getName());
				}
			} else {
				addDeserializer(domainType, new ResourceDeserializer(pe));
			}
		}
	}

	private class ResourceDeserializer<T extends Object> extends StdDeserializer<T> {

		private static final long serialVersionUID = 8195592798684027681L;
		private final PersistentEntity<?, ?> persistentEntity;

		private ResourceDeserializer(final PersistentEntity<?, ?> persistentEntity) {
			super(persistentEntity.getType());
			this.persistentEntity = persistentEntity;
		}

		@SuppressWarnings({ "unchecked", "incomplete-switch", "unused" })
		@Override
		public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			Object entity = instantiateClass(handledType());

			BeanWrapper<?, Object> wrapper = BeanWrapper.create(entity, conversionService);
			ResourceMetadata metadata = mappings.getMappingFor(handledType());

			for (JsonToken tok = jp.nextToken(); tok != JsonToken.END_OBJECT; tok = jp.nextToken()) {
				String name = jp.getCurrentName();
				switch (tok) {
					case FIELD_NAME: {
						if ("href".equals(name)) {
							URI uri = URI.create(jp.nextTextValue());
							TypeDescriptor entityType = TypeDescriptor.forObject(entity);
							if (uriDomainClassConverter.matches(URI_TYPE, entityType)) {
								entity = uriDomainClassConverter.convert(uri, URI_TYPE, entityType);
							}

							continue;
						}

						if ("rel".equals(name)) {
							// rel is currently ignored
							continue;
						}

						PersistentProperty<?> persistentProperty = persistentEntity.getPersistentProperty(name);
						if (null == persistentProperty) {
							continue;
						}

						Object val = null;

						if ("links".equals(name)) {
							if ((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
								while ((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
									// Advance past the links
								}
							} else if (tok == JsonToken.VALUE_NULL) {
								// skip null value
							} else {
								throw new HttpMessageNotReadableException(
										"Property 'links' is not of array type. Either eliminate this property from the document or make it an array.");
							}
							continue;
						}

						if (null == persistentProperty) {
							// do nothing
							continue;
						}

						// Try and read the value of this attribute.
						// The method of doing that varies based on the type of the property.
						if (persistentProperty.isCollectionLike()) {

							Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>) persistentProperty
									.getType();
							Collection<Object> collection = CollectionFactory.createCollection(collectionType, 0);

							if ((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
								while ((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
									Object cval = jp.readValueAs(persistentProperty.getComponentType());
									collection.add(cval);
								}

								val = collection;
							} else if (tok == JsonToken.VALUE_NULL) {
								val = null;
							} else {
								throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Collection.");
							}
						} else if (persistentProperty.isMap()) {

							Class<? extends Map<?, ?>> mapType = (Class<? extends Map<?, ?>>) persistentProperty.getType();
							Map<Object, Object> map = CollectionFactory.createMap(mapType, 0);

							if ((tok = jp.nextToken()) == JsonToken.START_OBJECT) {
								do {
									name = jp.getCurrentName();
									// TODO resolve domain object from URI
									tok = jp.nextToken();
									Object mval = jp.readValueAs(persistentProperty.getMapValueType());

									map.put(name, mval);
								} while ((tok = jp.nextToken()) != JsonToken.END_OBJECT);

								val = map;
							} else if (tok == JsonToken.VALUE_NULL) {
								val = null;
							} else {
								throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Map.");
							}
						} else {
							if ((tok = jp.nextToken()) != JsonToken.VALUE_NULL) {
								val = jp.readValueAs(persistentProperty.getType());
							}
						}

						wrapper.setProperty(persistentProperty, val, false);

						break;
					}
				}
			}

			return (T) entity;
		}
	}

	private class ResourceSerializer extends StdSerializer<PersistentEntityResource<?>> {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private ResourceSerializer() {
			super((Class) PersistentEntityResource.class);
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

			final PersistentEntity<?, ?> entity = resource.getPersistentEntity();
			final BeanWrapper<PersistentEntity<Object, ?>, Object> wrapper = BeanWrapper.create(obj, null);
			final Object entityId = wrapper.getProperty(entity.getIdProperty());
			final ResourceMetadata metadata = mappings.getMappingFor(entity.getType());
			final RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, config.getBaseUri()).slash(entityId);

			final List<Link> links = new ArrayList<Link>();
			// Start with ResourceProcessor-added links
			links.addAll(resource.getLinks());

			final Map<String, Object> model = new LinkedHashMap<String, Object>();

			try {

				entity.doWithProperties(new SimplePropertyHandler() {

					/*
					 * (non-Javadoc)
					 * @see org.springframework.data.mapping.SimplePropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
					 */
					@Override
					public void doWithPersistentProperty(PersistentProperty<?> property) {

						boolean idAvailableAndShallNotBeExposed = property.isIdProperty()
								&& !config.isIdExposedFor(entity.getType());

						if (idAvailableAndShallNotBeExposed) {
							return;
						}

						// Property is a normal or non-managed property.
						model.put(property.getName(), wrapper.getProperty(property));
					}
				});

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

						// Association Link was not added, probably because this isn't a managed type. Add value of property inline.
						if (metadata.isExported(property)) {
							model.put(property.getName(), wrapper.getProperty(property));
						}

					}
				});

				MapResource mapResource = new MapResource(model, links);
				jgen.writeObject(mapResource);

			} catch (IllegalStateException e) {
				throw (IOException) e.getCause();
			}
		}
	}

	private static class MapResource extends Resource<Map<String, Object>> {

		/**
		 * @param content
		 * @param links
		 */
		public MapResource(Map<String, Object> content, Iterable<Link> links) {
			super(content, links);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.Resource#getContent()
		 */
		@Override
		@JsonIgnore
		public Map<String, Object> getContent() {
			return super.getContent();
		}

		@JsonAnyGetter
		public Map<String, Object> any() {
			return getContent();
		}
	}
}
