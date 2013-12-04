package org.springframework.data.rest.webmvc.json;

import static org.springframework.beans.BeanUtils.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;

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
	@Autowired private Repositories repositories;
	@Autowired private RepositoryRestConfiguration config;
	@Autowired private UriDomainClassConverter uriDomainClassConverter;
	private final ResourceMappings mappings;

	public PersistentEntityJackson2Module(ResourceMappings resourceMappings) {

		super(new Version(1, 1, 0, "BUILD-SNAPSHOT", "org.springframework.data.rest", "jackson-module"));

		this.mappings = resourceMappings;
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

		@SuppressWarnings({ "unchecked", "incomplete-switch", "null", "unused" })
		@Override
		public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			Object entity = instantiateClass(getValueClass());
			BeanWrapper<?, Object> wrapper = BeanWrapper.create(entity, null);

			ResourceMetadata metadata = mappings.getMappingFor(getValueClass());

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
							Class<? extends Collection<?>> ctype = (Class<? extends Collection<?>>) persistentProperty.getType();
							Collection<Object> c = (Collection<Object>) wrapper.getProperty(persistentProperty);
							if (null == c || c == Collections.EMPTY_LIST || c == Collections.EMPTY_SET) {
								if (Collection.class.isAssignableFrom(ctype)) {
									c = new ArrayList<Object>();
								} else if (Set.class.isAssignableFrom(ctype)) {
									c = new HashSet<Object>();
								}
							}

							if ((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
								while ((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
									Object cval = jp.readValueAs(persistentProperty.getComponentType());
									c.add(cval);
								}

								val = c;
							} else if (tok == JsonToken.VALUE_NULL) {
								val = null;
							} else {
								throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Collection.");
							}
						} else if (persistentProperty.isMap()) {
							Class<? extends Map<?, ?>> mtype = (Class<? extends Map<?, ?>>) persistentProperty.getType();
							Map<Object, Object> m = (Map<Object, Object>) wrapper.getProperty(persistentProperty);
							if (null == m || m == Collections.EMPTY_MAP) {
								m = new HashMap<Object, Object>();
							}

							if ((tok = jp.nextToken()) == JsonToken.START_OBJECT) {
								do {
									name = jp.getCurrentName();
									// TODO resolve domain object from URI
									tok = jp.nextToken();
									Object mval = jp.readValueAs(persistentProperty.getMapValueType());

									m.put(name, mval);
								} while ((tok = jp.nextToken()) != JsonToken.END_OBJECT);

								val = m;
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
			final ResourceMappings mappings = new ResourceMappings(config, repositories);
			final ResourceMetadata metadata = mappings.getMappingFor(entity.getType());
			final RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, config.getBaseUri()).slash(entityId);

			final List<Link> links = new ArrayList<Link>();
			// Start with ResourceProcessor-added links
			links.addAll(resource.getLinks());
			jgen.writeStartObject();

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
						Object propertyValue = wrapper.getProperty(property);
						try {
							jgen.writeObjectField(property.getName(), propertyValue);
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
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
						Object propertyValue = wrapper.getProperty(property);
						try {
							jgen.writeObjectField(property.getName(), propertyValue);
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
					}
				});

				jgen.writeArrayFieldStart("links");

				for (Link l : links) {
					jgen.writeObject(l);
				}

				jgen.writeEndArray();

			} catch (IllegalStateException e) {
				throw (IOException) e.getCause();
			} finally {
				jgen.writeEndObject();
			}
		}
	}
}
