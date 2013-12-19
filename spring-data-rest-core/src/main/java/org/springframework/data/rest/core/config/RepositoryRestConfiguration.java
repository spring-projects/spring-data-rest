/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.core.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public class RepositoryRestConfiguration {

	private URI baseUri = null;
	private int defaultPageSize = 20;
	private int maxPageSize = 1000;
	private String pageParamName = "page";
	private String limitParamName = "limit";
	private String sortParamName = "sort";
	private MediaType defaultMediaType = MediaTypes.HAL_JSON;
	private boolean returnBodyOnCreate = false;
	private boolean returnBodyOnUpdate = false;
	private List<Class<?>> exposeIdsFor = new ArrayList<Class<?>>();
	private ResourceMappingConfiguration domainMappings = new ResourceMappingConfiguration();
	private ResourceMappingConfiguration repoMappings = new ResourceMappingConfiguration();

	/**
	 * The base URI against which the exporter should calculate its links.
	 * 
	 * @return The base URI.
	 */
	public URI getBaseUri() {
		return baseUri;
	}

	/**
	 * The base URI against which the exporter should calculate its links.
	 * 
	 * @param baseUri The base URI.
	 */
	public RepositoryRestConfiguration setBaseUri(URI baseUri) {
		Assert.notNull(baseUri, "The baseUri cannot be null.");
		this.baseUri = baseUri;
		return this;
	}

	/**
	 * Get the default size of {@link org.springframework.data.domain.Pageable}s. Default is 20.
	 * 
	 * @return The default page size.
	 */
	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	/**
	 * Set the default size of {@link org.springframework.data.domain.Pageable}s.
	 * 
	 * @param defaultPageSize The default page size.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setDefaultPageSize(int defaultPageSize) {
		Assert.isTrue((defaultPageSize > 0), "Page size must be greater than 0.");
		this.defaultPageSize = defaultPageSize;
		return this;
	}

	/**
	 * Get the maximum size of pages.
	 * 
	 * @return Maximum page size.
	 */
	public int getMaxPageSize() {
		return maxPageSize;
	}

	/**
	 * Set the maximum size of pages.
	 * 
	 * @param maxPageSize Maximum page size.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setMaxPageSize(int maxPageSize) {
		Assert.isTrue((defaultPageSize > 0), "Maximum page size must be greater than 0.");
		this.maxPageSize = maxPageSize;
		return this;
	}

	/**
	 * Get the name of the URL query string parameter that indicates what page to return. Default is 'page'.
	 * 
	 * @return Name of the query parameter used to indicate the page number to return.
	 */
	public String getPageParamName() {
		return pageParamName;
	}

	/**
	 * Set the name of the URL query string parameter that indicates what page to return.
	 * 
	 * @param pageParamName Name of the query parameter used to indicate the page number to return.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setPageParamName(String pageParamName) {
		Assert.notNull(pageParamName, "Page param name cannot be null.");
		this.pageParamName = pageParamName;
		return this;
	}

	/**
	 * Get the name of the URL query string parameter that indicates how many results to return at once. Default is
	 * 'limit'.
	 * 
	 * @return Name of the query parameter used to indicate the maximum number of entries to return at a time.
	 */
	public String getLimitParamName() {
		return limitParamName;
	}

	/**
	 * Set the name of the URL query string parameter that indicates how many results to return at once.
	 * 
	 * @param limitParamName Name of the query parameter used to indicate the maximum number of entries to return at a
	 *          time.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setLimitParamName(String limitParamName) {
		Assert.notNull(limitParamName, "Limit param name cannot be null.");
		this.limitParamName = limitParamName;
		return this;
	}

	/**
	 * Get the name of the URL query string parameter that indicates what direction to sort results. Default is 'sort'.
	 * 
	 * @return Name of the query string parameter used to indicate what field to sort on.
	 */
	public String getSortParamName() {
		return sortParamName;
	}

	/**
	 * Set the name of the URL query string parameter that indicates what direction to sort results.
	 * 
	 * @param sortParamName Name of the query string parameter used to indicate what field to sort on.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setSortParamName(String sortParamName) {
		Assert.notNull(sortParamName, "Sort param name cannot be null.");
		this.sortParamName = sortParamName;
		return this;
	}

	/**
	 * Get the {@link MediaType} to use as a default when none is specified.
	 * 
	 * @return Default content type if none has been specified.
	 */
	public MediaType getDefaultMediaType() {
		return defaultMediaType;
	}

	/**
	 * Set the {@link MediaType} to use as a default when none is specified.
	 * 
	 * @param defaultMediaType Default content type if none has been specified.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setDefaultMediaType(MediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
		return this;
	}

	/**
	 * Whether to return a response body after creating an entity.
	 * 
	 * @return {@literal true} to return a body on create, {@literal false} otherwise.
	 */
	public boolean isReturnBodyOnCreate() {
		return returnBodyOnCreate;
	}

	/**
	 * Set whether to return a response body after creating an entity.
	 * 
	 * @param returnBodyOnCreate {@literal true} to return a body on create, {@literal false} otherwise.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setReturnBodyOnCreate(boolean returnBodyOnCreate) {
		this.returnBodyOnCreate = returnBodyOnCreate;
		return this;
	}

	/**
	 * Whether to return a response body after updating an entity.
	 * 
	 * @return {@literal true} to return a body on update, {@literal false} otherwise.
	 */
	public boolean isReturnBodyOnUpdate() {
		return returnBodyOnUpdate;
	}

	/**
	 * Sets whether to return a response body after updating an entity.
	 * 
	 * @param returnBodyOnUpdate
	 * @return
	 */
	public RepositoryRestConfiguration setReturnBodyOnUpdate(boolean returnBodyOnUpdate) {
		this.returnBodyOnUpdate = returnBodyOnUpdate;
		return this;
	}

	/**
	 * Start configuration a {@link ResourceMapping} for a specific domain type.
	 * 
	 * @param domainType The {@link Class} of the domain type to configure a mapping for.
	 * @return A new {@link ResourceMapping} for configuring how a domain type is mapped.
	 */
	public ResourceMapping setResourceMappingForDomainType(Class<?> domainType) {
		return domainMappings.setResourceMappingFor(domainType);
	}

	/**
	 * Get the {@link ResourceMapping} for a specific domain type.
	 * 
	 * @param domainType The {@link Class} of the domain type.
	 * @return A {@link ResourceMapping} for that domain type or {@literal null} if none exists.
	 */
	public ResourceMapping getResourceMappingForDomainType(Class<?> domainType) {
		return domainMappings.getResourceMappingFor(domainType);
	}

	/**
	 * Whether there is a {@link ResourceMapping} for the given domain type.
	 * 
	 * @param domainType The domain type to find a {@link ResourceMapping} for.
	 * @return {@literal true} if a {@link ResourceMapping} exists for this domain class, {@literal false} otherwise.
	 */
	public boolean hasResourceMappingForDomainType(Class<?> domainType) {
		return domainMappings.hasResourceMappingFor(domainType);
	}

	/**
	 * Get the {@link ResourceMappingConfiguration} that is currently configured.
	 * 
	 * @return
	 */
	public ResourceMappingConfiguration getDomainTypesResourceMappingConfiguration() {
		return domainMappings;
	}

	/**
	 * Start configuration a {@link ResourceMapping} for a specific repository interface.
	 * 
	 * @param repositoryInterface The {@link Class} of the repository interface to configure a mapping for.
	 * @return A new {@link ResourceMapping} for configuring how a repository interface is mapped.
	 */
	public ResourceMapping setResourceMappingForRepository(Class<?> repositoryInterface) {
		return repoMappings.setResourceMappingFor(repositoryInterface);
	}

	/**
	 * Get the {@link ResourceMapping} for a specific repository interface.
	 * 
	 * @param repositoryInterface The {@link Class} of the repository interface.
	 * @return A {@link ResourceMapping} for that repository interface or {@literal null} if none exists.
	 */
	public ResourceMapping getResourceMappingForRepository(Class<?> repositoryInterface) {
		return repoMappings.getResourceMappingFor(repositoryInterface);
	}

	/**
	 * Whether there is a {@link ResourceMapping} configured for this {@literal Repository} class.
	 * 
	 * @param repositoryInterface
	 * @return
	 */
	public boolean hasResourceMappingForRepository(Class<?> repositoryInterface) {
		return repoMappings.hasResourceMappingFor(repositoryInterface);
	}

	public ResourceMapping findRepositoryMappingForPath(String path) {
		Class<?> type = repoMappings.findTypeForPath(path);
		if (null == type) {
			return null;
		}
		return repoMappings.getResourceMappingFor(type);
	}

	/**
	 * Should we expose the ID property for this domain type?
	 * 
	 * @param domainType The domain type we may need to expose the ID for.
	 * @return {@literal true} is the ID is to be exposed, {@literal false} otherwise.
	 */
	public boolean isIdExposedFor(Class<?> domainType) {
		return exposeIdsFor.contains(domainType);
	}

	/**
	 * Set the list of domain types for which we will expose the ID value as a normal property.
	 * 
	 * @param domainTypes Array of types to expose IDs for.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration exposeIdsFor(Class<?>... domainTypes) {
		Collections.addAll(exposeIdsFor, domainTypes);
		return this;
	}
}
