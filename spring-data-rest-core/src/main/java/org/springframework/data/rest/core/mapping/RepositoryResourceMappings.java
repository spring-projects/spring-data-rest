/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.util.Assert;

/**
 * Central abstraction obtain {@link ResourceMetadata} and {@link ResourceMapping} instances for domain types and
 * repositories.
 *
 * @author Oliver Gierke
 */
public class RepositoryResourceMappings extends PersistentEntitiesResourceMappings {

	private final Repositories repositories;
	private final RepositoryRestConfiguration configuration;
	private final Map<Class<?>, SearchResourceMappings> searchCache = new HashMap<Class<?>, SearchResourceMappings>();

	/**
	 * Creates a new {@link RepositoryResourceMappings} from the given {@link RepositoryRestConfiguration},
	 * {@link PersistentEntities}, and {@link Repositories}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param entities must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 */
	public RepositoryResourceMappings(Repositories repositories, PersistentEntities entities,
			RepositoryRestConfiguration configuration) {

		super(entities);

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

		this.repositories = repositories;
		this.configuration = configuration;
		this.populateCache(repositories, configuration);
	}

	private void populateCache(Repositories repositories, RepositoryRestConfiguration configuration) {

		for (Class<?> type : repositories) {

			RepositoryInformation repositoryInformation = repositories.getRequiredRepositoryInformation(type);
			Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();
			PersistentEntity<?, ?> entity = repositories.getPersistentEntity(type);

			RepositoryDetectionStrategy strategy = configuration.getRepositoryDetectionStrategy();
			LinkRelationProvider provider = configuration.getRelProvider();

			CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(repositoryInformation, strategy,
					provider);
			RepositoryAwareResourceMetadata information = new RepositoryAwareResourceMetadata(entity, mapping, this,
					repositoryInformation);

			addToCache(repositoryInterface, information);

			if (!hasMetadataFor(type) || information.isPrimary()) {
				addToCache(type, information);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMappings#getSearchResourceMappings(java.lang.Class)
	 */
	@Override
	public SearchResourceMappings getSearchResourceMappings(Class<?> domainType) {

		Assert.notNull(domainType, "Type must not be null!");

		if (searchCache.containsKey(domainType)) {
			return searchCache.get(domainType);
		}

		RepositoryInformation repositoryInformation = repositories.getRequiredRepositoryInformation(domainType);
		List<MethodResourceMapping> mappings = new ArrayList<MethodResourceMapping>();
		ResourceMetadata resourceMapping = getMetadataFor(domainType);

		if (resourceMapping.isExported()) {
			for (Method queryMethod : repositoryInformation.getQueryMethods()) {
				RepositoryMethodResourceMapping methodMapping = new RepositoryMethodResourceMapping(queryMethod,
						resourceMapping, repositoryInformation, exposeMethodsByDefault());
				if (methodMapping.isExported()) {
					mappings.add(methodMapping);
				}
			}
		}

		SearchResourceMappings searchResourceMappings = new SearchResourceMappings(mappings);
		searchCache.put(domainType, searchResourceMappings);
		return searchResourceMappings;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMappings#hasMappingFor(java.lang.Class)
	 */
	@Override
	public boolean hasMappingFor(Class<?> type) {

		if (super.hasMappingFor(type)) {
			return true;
		}

		if (repositories.hasRepositoryFor(type)) {
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings#isMapped(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public boolean isMapped(PersistentProperty<?> property) {
		return repositories.hasRepositoryFor(property.getActualType()) && super.isMapped(property);
	}

	/**
	 * Returns whether to expose repository methods by default, i.e. without the need to explicitly annotate them with
	 * {@link RestResource}.
	 *
	 * @since 3.1
	 * @see RepositoryRestConfiguration#exposeRepositoryMethodsByDefault()
	 */
	boolean exposeMethodsByDefault() {
		return configuration.exposeRepositoryMethodsByDefault();
	}

	/**
	 * Returns the underlying {@link ExposureConfiguration}.
	 *
	 * @return will never be {@literal null}.
	 * @since 3.1
	 * @see RepositoryRestConfiguration#getExposureConfiguration()
	 */
	ExposureConfiguration getExposureConfiguration() {
		return configuration.getExposureConfiguration();
	}
}
