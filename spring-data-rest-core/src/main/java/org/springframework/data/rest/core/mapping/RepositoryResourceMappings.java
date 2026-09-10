/*
 * Copyright 2013-present the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
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
 * @author Steve Rutherford
 */
public class RepositoryResourceMappings extends PersistentEntitiesResourceMappings {

	private final Repositories repositories;
	private final RepositoryRestConfiguration configuration;
	private final Map<Class<?>, SearchResourceMappings> searchCache = new HashMap<Class<?>, SearchResourceMappings>();

	/**
	 * Tracks the "winning" {@link RepositoryInformation} per domain type — i.e. the one whose metadata was stored in
	 * the domain-type cache slot. This is used by {@link #getSearchResourceMappings(Class)} to ensure that query
	 * methods are read from the exported repository when multiple repositories exist for the same domain type
	 * (DATAREST-80 / GH-465).
	 */
	private final Map<Class<?>, RepositoryInformation> repositoryInfoByDomainType = new HashMap<>();

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

		Assert.notNull(repositories, "Repositories must not be null");
		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null");

		this.repositories = repositories;
		this.configuration = configuration;
		this.populateCache(entities, configuration, null);
	}

	/**
	 * Creates a new {@link RepositoryResourceMappings} from the given {@link RepositoryRestConfiguration},
	 * {@link PersistentEntities}, {@link Repositories}, and {@link ListableBeanFactory}.
	 * <p>
	 * Using this constructor allows proper detection of all repository interfaces for a given domain type, including
	 * cases where multiple repository interfaces exist for the same domain type (e.g. for security purposes). The
	 * {@link ListableBeanFactory} is used to enumerate all {@link RepositoryFactoryInformation} beans, which provides
	 * one entry per repository interface rather than one entry per domain type.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param entities must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 * @param beanFactory must not be {@literal null}.
	 * @since 5.0
	 */
	public RepositoryResourceMappings(Repositories repositories, PersistentEntities entities,
			RepositoryRestConfiguration configuration, ListableBeanFactory beanFactory) {

		super(entities);

		Assert.notNull(repositories, "Repositories must not be null");
		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null");
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");

		this.repositories = repositories;
		this.configuration = configuration;
		this.populateCache(entities, configuration, beanFactory);
	}

	@SuppressWarnings("rawtypes")
	private void populateCache(PersistentEntities entities, RepositoryRestConfiguration configuration,
			ListableBeanFactory beanFactory) {

		RepositoryDetectionStrategy strategy = configuration.getRepositoryDetectionStrategy();
		LinkRelationProvider provider = configuration.getLinkRelationProvider();

		// When a BeanFactory is available, iterate over all RepositoryFactoryInformation beans to discover
		// all repository interfaces, including multiple repositories for the same domain type (DATAREST-80 / GH-465).
		if (beanFactory != null) {

			Collection<RepositoryFactoryInformation> factoryInfos = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(beanFactory, RepositoryFactoryInformation.class).values();

			for (RepositoryFactoryInformation<?, ?> factoryInfo : factoryInfos) {

				RepositoryInformation repositoryInformation = factoryInfo.getRepositoryInformation();
				Class<?> domainType = repositoryInformation.getDomainType();

				if (!entities.getPersistentEntity(domainType).isPresent()) {
					continue;
				}

				PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(domainType);
				Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();

				CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(repositoryInformation, strategy,
						provider);
				RepositoryAwareResourceMetadata information = new RepositoryAwareResourceMetadata(entity, mapping, this,
						repositoryInformation);

				addToCache(repositoryInterface, information);

				// Update the domain type cache entry if:
				// 1. No entry exists yet for this domain type, OR
				// 2. This repository is marked @Primary (explicit override), OR
				// 3. This repository is exported and the existing entry is not (prefer exported over non-exported)
				if (!hasMetadataFor(domainType) || information.isPrimary()
						|| (information.isExported() && !getMetadataFor(domainType).isExported())) {
					addToCache(domainType, information);
					repositoryInfoByDomainType.put(domainType, repositoryInformation);
				}
			}

			return;
		}

		// Fallback: iterate over PersistentEntities and get one repository per domain type.
		// This may miss exported repositories if multiple repositories exist for the same domain type
		// and the annotated one is not the primary one returned by Repositories.
		for (PersistentEntity<?, ? extends PersistentProperty<?>> entity : entities) {

			Class<?> type = entity.getType();

			if (!repositories.hasRepositoryFor(type)) {
				continue;
			}

			RepositoryInformation repositoryInformation = repositories.getRequiredRepositoryInformation(type);
			Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();

			CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(repositoryInformation, strategy,
					provider);
			RepositoryAwareResourceMetadata information = new RepositoryAwareResourceMetadata(entity, mapping, this,
					repositoryInformation);

			addToCache(repositoryInterface, information);

			if (!hasMetadataFor(type) || information.isPrimary()) {
				addToCache(type, information);
				repositoryInfoByDomainType.put(type, repositoryInformation);
			}
		}
	}

	@Override
	public SearchResourceMappings getSearchResourceMappings(Class<?> domainType) {

		Assert.notNull(domainType, "Type must not be null");

		if (searchCache.containsKey(domainType)) {
			return searchCache.get(domainType);
		}

		// Use the repository information that was selected during cache population (the exported one when multiple
		// repositories exist for the same domain type). Fall back to Repositories for backward compatibility.
		RepositoryInformation repositoryInformation = repositoryInfoByDomainType.containsKey(domainType)
				? repositoryInfoByDomainType.get(domainType)
				: repositories.getRequiredRepositoryInformation(domainType);

		List<MethodResourceMapping> mappings = new ArrayList<MethodResourceMapping>();
		ResourceMetadata resourceMapping = getRequiredMetadataFor(domainType);

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
