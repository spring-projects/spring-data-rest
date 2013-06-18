/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.repository.mapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.support.RepositoriesUtils;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.util.Assert;

/**
 * Central abstraction obtain {@link ResourceMetadata} and {@link ResourceMapping} instances for domain types and
 * repositories.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public class ResourceMappings implements ResourceMetadataProvider, Iterable<ResourceMetadata> {

	private final RepositoryRestConfiguration config;
	private final ResourceMappingFactory factory;
	private final Repositories repositories;

	private final Map<Class<?>, ResourceMetadata> cache = new HashMap<Class<?>, ResourceMetadata>();
	private final Map<Class<?>, Map<String, ResourceMapping>> searchCache = new HashMap<Class<?>, Map<String, ResourceMapping>>();

	/**
	 * Creates a new {@link ResourceMappings} using the given {@link RepositoryRestConfiguration} and {@link Repositories}
	 * .
	 * 
	 * @param config
	 * @param repositories
	 */
	public ResourceMappings(RepositoryRestConfiguration config, Repositories repositories) {
		this(config, repositories, new EvoInflectorRelProvider());
	}

	/**
	 * Creates a new {@link ResourceMappings} from the given {@link RepositoryRestConfiguration}, {@link Repositories} and
	 * {@link RelProvider}.
	 * 
	 * @param config must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public ResourceMappings(RepositoryRestConfiguration config, Repositories repositories, RelProvider relProvider) {

		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(relProvider, "RelProvider must not be null!");

		this.config = config;
		this.repositories = repositories;
		this.factory = new ResourceMappingFactory(relProvider);

		this.populateCache(repositories);
	}

	/**
	 * Returns a {@link ResourceMetadata} for the given type if available.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public ResourceMetadata getMappingFor(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");
		return cache.get(type);
	}

	private final void populateCache(Repositories repositories) {

		for (Class<?> type : repositories) {

			RepositoryInformation repositoryInformation = repositories.getRepositoryInformationFor(type);
			Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();

			CollectionResourceMapping mapping = factory.getMappingForType(repositoryInterface, fromConfig(type),
					fromConfig(repositoryInterface));

			RepositoryAwareResourceInformation information = new RepositoryAwareResourceInformation(repositories, mapping,
					this, repositoryInformation);

			cache.put(repositoryInterface, information);

			if (!cache.containsKey(type) || information.isPrimary()) {
				cache.put(type, information);
			}
		}
	}

	/**
	 * Returns the {@link ResourceMapping}s for the search resources of the given type.
	 * 
	 * @param type
	 * @return
	 */
	public Map<String, ResourceMapping> getSearchResourceMappings(Class<?> type) {

		if (searchCache.containsKey(type)) {
			return searchCache.get(type);
		}

		Class<?> domainType = RepositoriesUtils.getDomainType(type);
		RepositoryInformation repositoryInformation = repositories.getRepositoryInformationFor(domainType);
		Map<String, ResourceMapping> mappings = new HashMap<String, ResourceMapping>();

		for (Method queryMethod : repositoryInformation.getQueryMethods()) {
			mappings.put(queryMethod.getName(), new RepositoryMethodResourceMapping(queryMethod));
		}

		searchCache.put(type, mappings);
		return mappings;
	}

	/**
	 * Returns whether we have a {@link ResourceMapping} for the given type and it is exported.
	 * 
	 * @param type
	 * @return
	 */
	public boolean exportsMappingFor(Class<?> type) {

		if (!hasMappingFor(type)) {
			return false;
		}

		ResourceMetadata metadata = getMappingFor(type);
		return metadata.isExported();
	}

	/**
	 * Returns whether we have a {@link ResourceMapping} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public boolean hasMappingFor(Class<?> type) {

		if (cache.containsKey(type)) {
			return true;
		}

		if (repositories.hasRepositoryFor(type)) {
			return true;
		}

		if (RepositoriesUtils.isRepositoryInterface(type) && hasMappingFor(RepositoriesUtils.getDomainType(type))) {
			return true;
		}

		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMetadataProvider#getMappingFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public ResourceMapping getMappingFor(PersistentProperty<?> property) {
		return getMappingFor(property.getActualType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMetadataProvider#hasMappingFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public boolean isMapped(PersistentProperty<?> property) {

		ResourceMapping metadata = getMappingFor(property);
		return metadata != null && metadata.isExported();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ResourceMetadata> iterator() {
		return cache.values().iterator();
	}

	private CollectionResourceMapping fromConfig(Class<?> domainType) {

		org.springframework.data.rest.config.ResourceMapping mapping = config.getResourceMappingForDomainType(domainType);

		if (mapping == null) {
			return null;
		}

		return new SimpleCollectionResourceMapping(mapping.getRel(), null, mapping.getPath(), mapping.isExported());
	}
}
