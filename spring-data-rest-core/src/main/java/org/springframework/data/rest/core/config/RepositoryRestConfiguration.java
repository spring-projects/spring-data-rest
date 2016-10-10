/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;

/**
 * Spring Data REST configuration options.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Jeremy Rickard
 * @author Greg Turnquist
 * @author Mark Paluch
 */
@SuppressWarnings("deprecation")
public class RepositoryRestConfiguration {

	private static final URI NO_URI = URI.create("");

	private URI baseUri = NO_URI;
	private URI basePath = NO_URI;
	private int defaultPageSize = 20;
	private int maxPageSize = 1000;
	private String pageParamName = "page";
	private String limitParamName = "size";
	private String sortParamName = "sort";
	private MediaType defaultMediaType = MediaTypes.HAL_JSON;
	private boolean useHalAsDefaultJsonMediaType = true;
	private Boolean returnBodyOnCreate = null;
	private Boolean returnBodyOnUpdate = null;
	private List<Class<?>> exposeIdsFor = new ArrayList<Class<?>>();
	private ResourceMappingConfiguration domainMappings = new ResourceMappingConfiguration();
	private ResourceMappingConfiguration repoMappings = new ResourceMappingConfiguration();
	private RepositoryDetectionStrategy repositoryDetectionStrategy = RepositoryDetectionStrategies.DEFAULT;

	private final RepositoryCorsRegistry corsRegistry = new RepositoryCorsRegistry();
	private final ProjectionDefinitionConfiguration projectionConfiguration;
	private final MetadataConfiguration metadataConfiguration;
	private final EntityLookupConfiguration entityLookupConfiguration;
	private final List<Class<?>> valueTypes = new ArrayList<Class<?>>();

	private final EnumTranslationConfiguration enumTranslationConfiguration;
	private boolean enableEnumTranslation = false;

	/**
	 * Creates a new {@link RepositoryRestConfiguration} with the given {@link ProjectionDefinitionConfiguration}.
	 * 
	 * @param projectionConfiguration must not be {@literal null}.
	 * @param metadataConfiguration must not be {@literal null}.
	 * @param enumTranslationConfiguration must not be {@literal null}.
	 */
	public RepositoryRestConfiguration(ProjectionDefinitionConfiguration projectionConfiguration,
			MetadataConfiguration metadataConfiguration, EnumTranslationConfiguration enumTranslationConfiguration) {

		Assert.notNull(projectionConfiguration, "ProjectionDefinitionConfiguration must not be null!");
		Assert.notNull(metadataConfiguration, "MetadataConfiguration must not be null!");
		Assert.notNull(enumTranslationConfiguration, "EnumTranslationConfiguration must not be null!");

		this.projectionConfiguration = projectionConfiguration;
		this.metadataConfiguration = metadataConfiguration;
		this.enumTranslationConfiguration = enumTranslationConfiguration;
		this.entityLookupConfiguration = new EntityLookupConfiguration();
	}

	/**
	 * The base URI against which the exporter should calculate its links.
	 * 
	 * @return The base URI.
	 */
	public URI getBaseUri() {
		return basePath != NO_URI ? basePath : baseUri;
	}

	/**
	 * The base path to expose repository resources under.
	 * 
	 * @return the basePath
	 */
	public URI getBasePath() {
		return basePath;
	}

	/**
	 * Configures the base path to be used by Spring Data REST to expose repository resources.
	 * 
	 * @param basePath the basePath to set
	 */
	public void setBasePath(String basePath) {

		Assert.isTrue(!basePath.startsWith("http"), "Use a path not a URI");
		basePath = StringUtils.trimTrailingCharacter(basePath, '/');
		this.basePath = URI.create(basePath.startsWith("/") ? basePath : "/".concat(basePath));

		Assert.isTrue(!this.basePath.isAbsolute(), "Absolute URIs are not supported as base path!");
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
		Assert.isTrue(defaultPageSize > 0, "Page size must be greater than 0.");
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
		Assert.isTrue(defaultPageSize > 0, "Maximum page size must be greater than 0.");
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
	 * @param defaultMediaType default content type if none has been specified.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setDefaultMediaType(MediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
		return this;
	}

	/**
	 * Returns whether HAL will be served as primary representation in case on {@code application/json} is requested. This
	 * defaults to {@literal true}. If configured to {@literal false} the legacy Spring Data representation will be
	 * rendered.
	 * 
	 * @return
	 */
	public boolean useHalAsDefaultJsonMediaType() {
		return this.useHalAsDefaultJsonMediaType;
	}

	/**
	 * Configures whether HAL will be served as primary representation in case on {@code application/json} is requested.
	 * This defaults to {@literal true}. If configured to {@literal false} the legacy Spring Data representation will be
	 * rendered.
	 * 
	 * @param useHalAsDefaultJsonMediaType
	 * @return
	 */
	public RepositoryRestConfiguration useHalAsDefaultJsonMediaType(boolean useHalAsDefaultJsonMediaType) {
		this.useHalAsDefaultJsonMediaType = useHalAsDefaultJsonMediaType;
		return this;
	}

	/**
	 * Convenience method to activate returning response bodies for all {@code PUT} and {@code POST} requests, i.e. both
	 * creating and updating entities.
	 * 
	 * @param returnBody can be {@literal null}, expressing the decision shall be derived from the presence of an
	 *          {@code Accept} header in the request.
	 * @return
	 */
	public RepositoryRestConfiguration setReturnBodyForPutAndPost(Boolean returnBody) {

		setReturnBodyOnCreate(returnBody);
		setReturnBodyOnUpdate(returnBody);

		return this;
	}

	/**
	 * Whether to return a response body after creating an entity.
	 * 
	 * @return {@link java.lang.Boolean#TRUE} to enforce returning a body on create, {@link java.lang.Boolean#FALSE}
	 *         otherwise. If {@literal null} and an {@code Accept} header present in the request will cause a body being
	 *         returned. If the {@code Accept} header is not present, no body will be rendered.
	 * @deprecated use {@link #returnBodyOnCreate(String)}
	 */
	@Deprecated
	public Boolean isReturnBodyOnCreate() {
		return returnBodyOnCreate;
	}

	/**
	 * Whether to return a response body after creating an entity considering the given accept header.
	 * 
	 * @param acceptHeader can be {@literal null} or empty.
	 * @return
	 */
	public boolean returnBodyOnCreate(String acceptHeader) {
		return returnBodyOnCreate == null ? StringUtils.hasText(acceptHeader) : returnBodyOnCreate;
	}

	/**
	 * Set whether to return a response body after creating an entity.
	 * 
	 * @param returnBody can be {@literal null}, expressing the decision shall be derived from the presence of an
	 *          {@code Accept} header in the request.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setReturnBodyOnCreate(Boolean returnBody) {
		this.returnBodyOnCreate = returnBody;
		return this;
	}

	/**
	 * Whether to return a response body after updating an entity.
	 * 
	 * @return {@link java.lang.Boolean#TRUE} to enforce returning a body on create, {@link java.lang.Boolean#FALSE}
	 *         otherwise. If {@literal null} and an {@code Accept} header present in the request will cause a body being
	 *         returned. If the {@code Accept} header is not present, no body will be rendered.
	 * @deprecated use {@link #returnBodyOnUpdate(String)}
	 */
	@Deprecated
	public Boolean isReturnBodyOnUpdate() {
		return returnBodyOnUpdate;
	}

	/**
	 * Whether to return a response body after updating an entity considering the given accept header.
	 * 
	 * @param acceptHeader can be {@literal null} or empty.
	 * @return
	 */
	public boolean returnBodyOnUpdate(String acceptHeader) {
		return returnBodyOnUpdate == null ? StringUtils.hasText(acceptHeader) : returnBodyOnUpdate;
	}

	/**
	 * Set whether to return a response body after updating an entity.
	 *
	 * @param returnBody can be {@literal null}, expressing the decision shall be derived from the presence of an
	 *          {@code Accept} header in the request.
	 * @return {@literal this}
	 */
	public RepositoryRestConfiguration setReturnBodyOnUpdate(Boolean returnBodyOnUpdate) {
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

	/**
	 * Returns the {@link ProjectionDefinitionConfiguration} to register addition projections.
	 * 
	 * @return
	 * @deprecated since 2.4, use {@link #getProjectionConfiguration()} instead.
	 */
	@Deprecated
	public ProjectionDefinitionConfiguration projectionConfiguration() {
		return getProjectionConfiguration();
	}

	/**
	 * Returns the {@link ProjectionDefinitionConfiguration} to register addition projections.
	 * 
	 * @return
	 */
	public ProjectionDefinitionConfiguration getProjectionConfiguration() {
		return projectionConfiguration;
	}

	/**
	 * Returns the {@link MetadataConfiguration} to customize metadata exposure.
	 * 
	 * @return
	 * @deprecated since 2.4, use {@link #getMetadataConfiguration()} instead.
	 */
	@Deprecated
	public MetadataConfiguration metadataConfiguration() {
		return metadataConfiguration;
	}

	/**
	 * Returns the {@link MetadataConfiguration} to customize metadata exposure.
	 * 
	 * @return
	 */
	public MetadataConfiguration getMetadataConfiguration() {
		return metadataConfiguration;
	}

	/**
	 * Configures whether to enable enum value translation via the Spring Data REST default resource bundle. Defaults to
	 * {@literal false} for backwards compatibility reasons. Will use the fully qualified enum name as key. For further
	 * details see {@link EnumTranslator}.
	 * 
	 * @param enableEnumTranslation
	 * @see #getEnumTranslationConfiguration()
	 */
	public void setEnableEnumTranslation(boolean enableEnumTranslation) {
		this.enableEnumTranslation = enableEnumTranslation;
	}

	/**
	 * Returns whether enum value translation is enabled.
	 * 
	 * @return
	 * @since 2.4
	 */
	public boolean isEnableEnumTranslation() {
		return this.enableEnumTranslation;
	}

	/**
	 * Returns the {@link EnumTranslationConfiguration} to be used.
	 * 
	 * @return must not be {@literal null}.
	 * @since 2.4
	 */
	public EnumTranslationConfiguration getEnumTranslationConfiguration() {
		return this.enumTranslationConfiguration;
	}

	/**
	 * Returns the {@link RepositoryDetectionStrategy} to be used to decide which repositories get exposed. Will be
	 * {@link RepositoryDetectionStrategies#DEFAULT} by default.
	 * 
	 * @return will never be {@literal null}.
	 * @see RepositoryDetectionStrategies
	 * @since 2.5
	 */
	public RepositoryDetectionStrategy getRepositoryDetectionStrategy() {
		return repositoryDetectionStrategy;
	}

	/**
	 * Configures the {@link RepositoryDetectionStrategy} to be used to determine which repositories get exposed. Defaults
	 * to {@link RepositoryDetectionStrategies#DEFAULT}.
	 * 
	 * @param repositoryDetectionStrategy can be {@literal null}.
	 * @since 2.5
	 */
	public void setRepositoryDetectionStrategy(RepositoryDetectionStrategy repositoryDetectionStrategy) {
		this.repositoryDetectionStrategy = repositoryDetectionStrategy == null ? RepositoryDetectionStrategies.DEFAULT
				: repositoryDetectionStrategy;
	}

	/**
	 * Returns the {@link RepositoryCorsRegistry} to configure Cross-origin resource sharing.
	 *
	 * @return the {@link RepositoryCorsRegistry}.
	 * @since 2.6
	 * @see RepositoryCorsRegistry
	 * @see CorsRegistration
	 */
	public RepositoryCorsRegistry getCorsRegistry() {
		return corsRegistry;
	}

	/**
	 * Configures Cross-origin resource sharing given a {@code path}.
	 *
	 * @param path path or path pattern, must not be {@literal null} or empty.
	 * @return the {@link CorsRegistration} to build a CORS configuration.
	 * @since 2.6
	 * @see CorsConfiguration
	 */
	public CorsRegistration addCorsMapping(String path) {

		Assert.notNull(path, "Path must not be null!");
		Assert.hasText(path, "Path must not be empty!");

		return corsRegistry.addMapping(path);
	}

	/**
	 * Returns the {@link EntityLookupRegistrar} to create custom {@link EntityLookup} instances registered in the
	 * configuration.
	 * 
	 * @return the {@link EntityLookupRegistrar} to build custom {@link EntityLookup}s.
	 * @since 2.5
	 */
	public EntityLookupRegistrar withEntityLookup() {
		return entityLookupConfiguration;
	}

	/**
	 * Returns all {@link EntityLookup}s considering the customizations made to the configuration.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @return
	 */
	public List<EntityLookup<?>> getEntityLookups(Repositories repositories) {

		Assert.notNull(repositories, "Repositories must not be null!");

		return entityLookupConfiguration.getEntityLookups(repositories);
	}

	public boolean isLookupType(Class<?> type) {
		return this.entityLookupConfiguration.isLookupType(type);
	}
}
