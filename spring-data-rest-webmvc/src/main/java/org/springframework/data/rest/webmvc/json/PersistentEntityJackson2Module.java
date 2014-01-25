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


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriDomainClassConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.data.rest.webmvc.support.RepositoryUriResolver;
import org.springframework.hateoas.Link;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Jon Brisbin
 * @author Nick Weedon
 */
public abstract class PersistentEntityJackson2Module extends SimpleModule implements InitializingBean {

	private static final long serialVersionUID = -7289265674870906323L;
	static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);

	private final ResourceMappings mappings;

	@Autowired private Repositories repositories;
	@Autowired private RepositoryRestConfiguration config;
	@Autowired private UriDomainClassConverter uriDomainClassConverter;

	public PersistentEntityJackson2Module(ResourceMappings resourceMappings) {

		super(new Version(1, 1, 0, "BUILD-SNAPSHOT", "org.springframework.data.rest", "jackson-module"));

		Assert.notNull(resourceMappings, "ResourceMappings must not be null!");

		this.mappings = resourceMappings;
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
		addSerializer(new ResourceSerializer(mappings, config) {
			@Override
			protected ObjectMapper getObjectMapper() {
				return PersistentEntityJackson2Module.this.getObjectMapper();
			}
			
		});
		
		for (Class<?> domainType : repositories) {
			PersistentEntity<?, ?> pe = repositories.getPersistentEntity(domainType);
			if (null == pe) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("The domain class {} does not have PersistentEntity metadata.", domainType.getName());
				}
			} else {
				addDeserializer(domainType, 
						new ResourceDeserializer(pe, uriDomainClassConverter, 
								new RepositoryUriResolver(repositories, mappings)));
			}
		}
	}

	/**
	 * Retrieve the the ObjectMapper bean
	 * 
	 * The reason that method injection is used here to wire in the ObjectMapper bean
	 * is to avoid a circular object reference between the ObjectMapper bean and this bean. 
	 */
	protected abstract ObjectMapper getObjectMapper();
	
}
