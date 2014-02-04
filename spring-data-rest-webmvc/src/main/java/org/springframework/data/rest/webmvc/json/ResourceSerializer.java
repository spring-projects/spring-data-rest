/*
 * Copyright 2014 the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link PersistentEntityResource} deserializer registered by {@link PersistentEntityJackson2Module}.
 * 
 * This Json serializer is responsible for the json serialization {@link PersistentEntityResource} classes,
 * specific to a particular domain object. the {@link PersistentEntityResource} class is esentially a hal
 * decorator, facilitating the amendment of association links.
 * 
 * The serializer embeds the domain object in a {@link MapResource} class which is essentially a Hal
 * decorator, facilitating the amendment of association links. The corresponding links are then added and 
 * a filter is used to filter out the association fields that were already added as links.   
 * 
 * @author Nick Weedon
 */
abstract class ResourceSerializer extends StdSerializer<PersistentEntityResource<?>> {

	static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);
	final ResourceMappings mappings;
	final RepositoryRestConfiguration config;
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) 
	ResourceSerializer(ResourceMappings mappings, RepositoryRestConfiguration config) {
		super((Class) PersistentEntityResource.class);
		this.mappings = mappings;
		this.config = config;
	}

	protected abstract ObjectMapper getObjectMapper();
	
	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(final PersistentEntityResource<?> resource, final JsonGenerator jgen,
			final SerializerProvider provider) throws IOException, JsonGenerationException {

		if (PersistentEntityJackson2Module.LOG.isDebugEnabled()) {
			PersistentEntityJackson2Module.LOG.debug("Serializing PersistentEntity " + resource.getPersistentEntity());
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

		// Currently the adding of association links is deterministic per domain class.
		// It is tempting for this reason to set up the filter and links at configuration time.
		// This is potentially a bad idea however as it makes it difficult to add later changes
		// that add links based on factors other than those strictly pertinent to the 
		// domain class (such as inlining associations based on query parameters).
		try {
			final Set<String> filteredProperties = new HashSet<String>();

			if(entity.getIdProperty() != null && !config.isIdExposedFor(entity.getType())) {
				filteredProperties.add(entity.getIdProperty().getName());
			}

			// Add associations as links
			entity.doWithAssociations(new SimpleAssociationHandler() {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
				 */
				@Override
				public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

					PersistentProperty<?> property = association.getInverse();

					if (PersistentEntityJackson2Module.maybeAddAssociationLink(builder, mappings, property, links) || !metadata.isExported(property)) {
						// A link was added so don't inline this association
						filteredProperties.add(property.getName());
					}
					
				}
			});

			MapResource mapResource = new MapResource(model, links, obj);
			
			// Create a Jackson JSON filter to remove 'filteredProperties' from the JSON output
			FilterProvider filters = 
					new SimpleFilterProvider().addFilter(DomainClassIntrospector.ENTITY_JSON_FILTER,
							SimpleBeanPropertyFilter.serializeAllExcept(filteredProperties));
			
			// Output the map resource using the filter and a custom AnnotationIntrospector
			// used by the objectmapper (see configuration class).
			// The custom AnnotationInstrospector associates the 'domain class' with the
			// filter we defined (an alternative to using the @JsonFilter class annotation)
			getObjectMapper().writer(filters).writeValue(jgen, mapResource);
		} catch (IllegalStateException e) {
			throw (IOException) e.getCause();
		}
	}
	
	static class MapResource extends Resource<Map<String, Object>> {
		
		/**
		 * @param content
		 * @param links
		 */
		public MapResource(Map<String, Object> content, Iterable<Link> links, Object obj) {
			super(content, links);
			this.setObj(obj);
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

		@com.fasterxml.jackson.annotation.JsonUnwrapped		
		private Object obj;
		
		public Object getObj() {
			return obj;
		}

		public void setObj(Object obj) {
			this.obj = obj;
		}
	}
}