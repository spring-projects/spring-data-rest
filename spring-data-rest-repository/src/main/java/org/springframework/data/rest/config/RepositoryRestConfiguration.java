package org.springframework.data.rest.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestConfiguration {

  private URI                           baseUri             = null;
  private int                           defaultPageSize     = 20;
  private String                        pageParamName       = "page";
  private String                        limitParamName      = "limit";
  private String                        sortParamName       = "sort";
  private String                        jsonpParamName      = "callback";
  private String                        jsonpOnErrParamName = null;
  private List<HttpMessageConverter<?>> customConverters    = Collections.emptyList();
  private Map<Class<?>, Class<?>>       typeMappings        = Collections.emptyMap();
  private MediaType                     defaultMediaType    = MediaType.APPLICATION_JSON;
  private boolean                       dumpErrors          = true;
  private List<Class<?>>                exposeIdsFor        = new ArrayList<Class<?>>();
  private ResourceMappingConfiguration  domainMappings      = new ResourceMappingConfiguration();
  private ResourceMappingConfiguration  repoMappings        = new ResourceMappingConfiguration();

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
   * @param baseUri
   *     The base URI.
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
   * @param defaultPageSize
   *     The default page size.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setDefaultPageSize(int defaultPageSize) {
    Assert.isTrue((defaultPageSize > 0), "Page size must be greater than 0.");
    this.defaultPageSize = defaultPageSize;
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
   * @param pageParamName
   *     Name of the query parameter used to indicate the page number to return.
   *
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
   * @param limitParamName
   *     Name of the query parameter used to indicate the maximum number of entries to return at a time.
   *
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
   * @param sortParamName
   *     Name of the query string parameter used to indicate what field to sort on.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setSortParamName(String sortParamName) {
    Assert.notNull(sortParamName, "Sort param name cannot be null.");
    this.sortParamName = sortParamName;
    return this;
  }

  /**
   * Get the list of custom {@link HttpMessageConverter}s to be used to convert user input to objects and visa versa.
   *
   * @return List of custom {@literal HttpMessageConverter}s.
   */
  public List<HttpMessageConverter<?>> getCustomConverters() {
    return customConverters;
  }

  /**
   * Set the list of custom {@link HttpMessageConverter}s to be used to convert user input to objects and visa versa.
   *
   * @param customConverters
   *     List of custom {@literal HttpMessageConverter}s.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setCustomConverters(List<HttpMessageConverter<?>> customConverters) {
    Assert.notNull(customConverters, "Custom converters list cannot be null.");
    this.customConverters = customConverters;
    return this;
  }

  /**
   * Get the list of domain type to repository implementation mappings that will help the exporters narrow down the
   * correct {@link org.springframework.data.repository.Repository} to return for a given domain type.
   *
   * @return A {@link Map} of domain type to repository mappings.
   */
  public Map<Class<?>, Class<?>> getDomainTypeToRepositoryMappings() {
    return typeMappings;
  }

  /**
   * Set the list of domain type to repository implementation mappings that will help the exporters narrow down the
   * correct {@link org.springframework.data.repository.Repository} to return for a given domain type.
   *
   * @param typeMappings
   *     A {@link Map} of domain type to repository mappings.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setDomainTypeToRepositoryMappings(Map<Class<?>, Class<?>> typeMappings) {
    this.typeMappings = typeMappings;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * JSONP wrapper for results.
   *
   * @return Name of the query string parameter used to indicate the JSONP callback function.
   */
  public String getJsonpParamName() {
    return jsonpParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * JSONP wrapper for results.
   *
   * @param jsonpParamName
   *     Name of the query string parameter used to indicate the JSONP callback function.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setJsonpParamName(String jsonpParamName) {
    this.jsonpParamName = jsonpParamName;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * error handler JSONP wrapper for errors.
   *
   * @return Name of the query string parameter used to indicate what javascript function to use as the JSONP error
   *         response.
   */
  public String getJsonpOnErrParamName() {
    return jsonpOnErrParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * error handler JSONP wrapper for errors.
   *
   * @param jsonpOnErrParamName
   *     Name of the query string parameter used to indicate what javascript function to use as the JSONP error
   *     response.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setJsonpOnErrParamName(String jsonpOnErrParamName) {
    this.jsonpOnErrParamName = jsonpOnErrParamName;
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
   * @param defaultMediaType
   *     Default content type if none has been specified.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setDefaultMediaType(MediaType defaultMediaType) {
    this.defaultMediaType = defaultMediaType;
    return this;
  }

  /**
   * Should exception messages be logged to the body of the response in a JSON object?
   *
   * @return Flag indicating whether exception messages are logged to the body of the response.
   */
  public boolean isDumpErrors() {
    return dumpErrors;
  }

  /**
   * Set whether exception messages should be logged to the body of the response as a JSON object.
   *
   * @param dumpErrors
   *     Flag indicating whether exception messages are logged to the body of the response.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration setDumpErrors(boolean dumpErrors) {
    this.dumpErrors = dumpErrors;
    return this;
  }

  /**
   * Start configuration a {@link ResourceMapping} for a specific domain type.
   *
   * @param domainType
   *     The {@link Class} of the domain type to configure a mapping for.
   *
   * @return A new {@link ResourceMapping} for configuring how a domain type is mapped.
   */
  public ResourceMapping addResourceMappingForDomainType(Class<?> domainType) {
    return domainMappings.addResourceMappingFor(domainType);
  }

  /**
   * Get the {@link ResourceMapping} for a specific domain type.
   *
   * @param domainType
   *     The {@link Class} of the domain type.
   *
   * @return A {@link ResourceMapping} for that domain type or {@literal null} if none exists.
   */
  public ResourceMapping getResourceMappingForDomainType(Class<?> domainType) {
    return domainMappings.getResourceMappingFor(domainType);
  }

  public boolean hasResourceMappingForDomainType(Class<?> domainType) {
    return domainMappings.hasResourceMappingFor(domainType);
  }

  public ResourceMappingConfiguration getDomainTypesResourceMappingConfiguration() {
    return domainMappings;
  }

  /**
   * Start configuration a {@link ResourceMapping} for a specific repository interface.
   *
   * @param repositoryInterface
   *     The {@link Class} of the repository interface to configure a mapping for.
   *
   * @return A new {@link ResourceMapping} for configuring how a repository interface is mapped.
   */
  public ResourceMapping setResourceMappingForRepository(Class<?> repositoryInterface) {
    return repoMappings.addResourceMappingFor(repositoryInterface);
  }

  /**
   * Get the {@link ResourceMapping} for a specific repository interface.
   *
   * @param repositoryInterface
   *     The {@link Class} of the repository interface.
   *
   * @return A {@link ResourceMapping} for that repository interface or {@literal null} if none exists.
   */
  public ResourceMapping getResourceMappingForRepository(Class<?> repositoryInterface) {
    return repoMappings.getResourceMappingFor(repositoryInterface);
  }

  public boolean hasResourceMappingForRepository(Class<?> repositoryInterface) {
    return repoMappings.hasResourceMappingFor(repositoryInterface);
  }

  public ResourceMapping findRepositoryMappingForPath(String path) {
    Class<?> type = repoMappings.findTypeForPath(path);
    if(null == type) {
      return null;
    }
    return repoMappings.getResourceMappingFor(type);
  }

  /**
   * Should we expose the ID property for this domain type?
   *
   * @param domainType
   *     The domain type we may need to expose the ID for.
   *
   * @return {@literal true} is the ID is to be exposed, {@literal false} otherwise.
   */
  public boolean isIdExposedFor(Class<?> domainType) {
    return exposeIdsFor.contains(domainType);
  }

  /**
   * Set the list of domain types for which we will expose the ID value as a normal property.
   *
   * @param domainTypes
   *     Array of types to expose IDs for.
   *
   * @return {@literal this}
   */
  public RepositoryRestConfiguration exposeIdsFor(Class<?>... domainTypes) {
    Collections.addAll(exposeIdsFor, domainTypes);
    return this;
  }

}
