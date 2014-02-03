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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class PersistentEntityJackson2Module extends SimpleModule {

	private static final long serialVersionUID = -7289265674870906323L;
	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);

	private final ResourceMappings mappings;

	@Autowired private Repositories repositories;
	@Autowired private RepositoryRestConfiguration config;

	public PersistentEntityJackson2Module(ResourceMappings resourceMappings, ConversionService conversionService) {

		super(new Version(1, 1, 0, "BUILD-SNAPSHOT", "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(resourceMappings, "ResourceMappings must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

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
