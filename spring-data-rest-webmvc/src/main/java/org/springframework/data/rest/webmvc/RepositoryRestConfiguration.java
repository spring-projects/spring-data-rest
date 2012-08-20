package org.springframework.data.rest.webmvc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;

/**
 * Central configuration helper class for the REST exporter. If something within the REST exporter is configurable,
 * there is a property here you can use to set the value.
 *
 * @author Jon Brisbin
 */
public class RepositoryRestConfiguration {

  public static final RepositoryRestConfiguration DEFAULT = new RepositoryRestConfiguration();

  private int                                       defaultPageSize           = 20;
  private String                                    pageParamName             = "page";
  private String                                    limitParamName            = "limit";
  private String                                    sortParamName             = "sort";
  private String                                    jsonpParamName            = "callback";
  private String                                    jsonpOnErrParamName       = null;
  private List<HttpMessageConverter<?>>             customConverters          = Collections.emptyList();
  private Multimap<Class<?>, ResourcePostProcessor> resourcePostProcessors    = ArrayListMultimap.create();
  private List<ResourceSetPostProcessor>            resourceSetPostProcessors = Collections.emptyList();
  private Map<Class<?>, Class<?>>                   typeMappings              = Collections.emptyMap();
  private MediaType                                 defaultMediaType          = MediaType.APPLICATION_JSON;
  private boolean                                   dumpErrors                = true;

  /**
   * Get the default size of {@link org.springframework.data.domain.Pageable}s. Default is 20.
   *
   * @return
   */
  public int getDefaultPageSize() {
    return defaultPageSize;
  }

  /**
   * Set the default size of {@link org.springframework.data.domain.Pageable}s.
   *
   * @param defaultPageSize
   *
   * @return
   */
  public RepositoryRestConfiguration setDefaultPageSize(int defaultPageSize) {
    Assert.isTrue((defaultPageSize > 0), "Page size must be greater than 0.");
    this.defaultPageSize = defaultPageSize;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates what page to return. Default is 'page'.
   *
   * @return
   */
  public String getPageParamName() {
    return pageParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates what page to return.
   *
   * @param pageParamName
   *
   * @return
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
   * @return
   */
  public String getLimitParamName() {
    return limitParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates how many results to return at once.
   *
   * @param limitParamName
   *
   * @return
   */
  public RepositoryRestConfiguration setLimitParamName(String limitParamName) {
    Assert.notNull(limitParamName, "Limit param name cannot be null.");
    this.limitParamName = limitParamName;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates what direction to sort results. Default is 'sort'.
   *
   * @return
   */
  public String getSortParamName() {
    return sortParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates what direction to sort results.
   *
   * @param sortParamName
   *
   * @return
   */
  public RepositoryRestConfiguration setSortParamName(String sortParamName) {
    Assert.notNull(sortParamName, "Sort param name cannot be null.");
    this.sortParamName = sortParamName;
    return this;
  }

  /**
   * Get the list of custom {@link HttpMessageConverter}s to be used to convert user input to objects and visa versa.
   *
   * @return
   */
  public List<HttpMessageConverter<?>> getCustomConverters() {
    return customConverters;
  }

  /**
   * Set the list of custom {@link HttpMessageConverter}s to be used to convert user input to objects and visa versa.
   *
   * @param customConverters
   *
   * @return
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
   * @return
   */
  public Map<Class<?>, Class<?>> getDomainTypeToRepositoryMappings() {
    return typeMappings;
  }

  /**
   * Set the list of domain type to repository implementation mappings that will help the exporters narrow down the
   * correct {@link org.springframework.data.repository.Repository} to return for a given domain type.
   *
   * @param typeMappings
   *
   * @return
   */
  public RepositoryRestConfiguration setDomainTypeToRepositoryMappings(Map<Class<?>, Class<?>> typeMappings) {
    this.typeMappings = typeMappings;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * JSONP wrapper for results.
   *
   * @return
   */
  public String getJsonpParamName() {
    return jsonpParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * JSONP wrapper for results.
   *
   * @param jsonpParamName
   *
   * @return
   */
  public RepositoryRestConfiguration setJsonpParamName(String jsonpParamName) {
    this.jsonpParamName = jsonpParamName;
    return this;
  }

  /**
   * Get the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * error handler JSONP wrapper for errors.
   *
   * @return
   */
  public String getJsonpOnErrParamName() {
    return jsonpOnErrParamName;
  }

  /**
   * Set the name of the URL query string parameter that indicates the name of the javascript function to use as the
   * error handler JSONP wrapper for errors.
   *
   * @param jsonpOnErrParamName
   *
   * @return
   */
  public RepositoryRestConfiguration setJsonpOnErrParamName(String jsonpOnErrParamName) {
    this.jsonpOnErrParamName = jsonpOnErrParamName;
    return this;
  }

  /**
   * Get the {@link MediaType} to use as a default when none is specified.
   *
   * @return
   */
  public MediaType getDefaultMediaType() {
    return defaultMediaType;
  }

  /**
   * Set the {@link MediaType} to use as a default when none is specified.
   *
   * @param defaultMediaType
   *
   * @return
   */
  public RepositoryRestConfiguration setDefaultMediaType(MediaType defaultMediaType) {
    this.defaultMediaType = defaultMediaType;
    return this;
  }

  /**
   * Should exception messages be logged to the body of the response in a JSON object?
   *
   * @return
   */
  public boolean isDumpErrors() {
    return dumpErrors;
  }

  /**
   * Set whether exception messages should be logged to the body of the response as a JSON object.
   *
   * @param dumpErrors
   *
   * @return
   */
  public RepositoryRestConfiguration setDumpErrors(boolean dumpErrors) {
    this.dumpErrors = dumpErrors;
    return this;
  }

  /**
   * Get the list of {@link ResourceSetPostProcessor}s that will potentially alter the responses going back to the
   * client.
   *
   * @return
   */
  public List<ResourceSetPostProcessor> getResourceSetPostProcessors() {
    return resourceSetPostProcessors;
  }

  /**
   * Set the list of {@link ResourceSetPostProcessor}s that will potentially alter the responses going back to the
   *
   * @param resourceSetPostProcessors
   */
  @Autowired(required = false)
  public RepositoryRestConfiguration setResourceSetPostProcessors(List<ResourceSetPostProcessor> resourceSetPostProcessors) {
    Assert.notNull(resourceSetPostProcessors, "ResourceSetPostProcessors cannot be null.");
    this.resourceSetPostProcessors = resourceSetPostProcessors;
    return this;
  }

  /**
   * Add a {@link ResourcePostProcessor} that is responsible for post-processing a particular domain type.
   *
   * @param type
   * @param postProcessor
   *
   * @return
   */
  public RepositoryRestConfiguration addResourcePostProcessor(Class<?> type, ResourcePostProcessor postProcessor) {
    Assert.notNull(type, "Type for ResourcePostProcessor cannot be null.");
    Assert.notNull(postProcessor, "ResourcePostProcessor for type " + type.getName() + " cannot be null.");
    resourcePostProcessors.put(type, postProcessor);
    return this;
  }

  /**
   * Set tje {@link ResourcePostProcessor} map used to determine what post-processors to run for which domain type.
   *
   * @param postProcessors
   *
   * @return
   */
  public RepositoryRestConfiguration setResourcePostProcessors(Map<Class<?>, ResourcePostProcessor> postProcessors) {
    if(null == postProcessors) {
      return this;
    }
    for(Map.Entry<Class<?>, ResourcePostProcessor> entry : postProcessors.entrySet()) {
      addResourcePostProcessor(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Get the {@link ResourcePostProcessor}s assigned to a particular domain type.
   *
   * @param type
   *
   * @return
   */
  public Collection<ResourcePostProcessor> getResourcePostProcessors(Class<?> type) {
    Collection<ResourcePostProcessor> pps = resourcePostProcessors.get(type);
    if(null == pps) {
      return Collections.emptyList();
    } else {
      return pps;
    }
  }

}
