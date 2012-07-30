package org.springframework.data.rest.webmvc;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestConfiguration {

  public static final RepositoryRestConfiguration DEFAULT = new RepositoryRestConfiguration();

  private int                           defaultPageSize     = 20;
  private String                        pageParamName       = "page";
  private String                        limitParamName      = "limit";
  private String                        sortParamName       = "sort";
  private String                        jsonpParamName      = "callback";
  private String                        jsonpOnErrParamName = null;
  private List<HttpMessageConverter<?>> customConverters    = Collections.emptyList();
  private MediaType                     defaultMediaType    = MediaType.APPLICATION_JSON;
  private boolean                       dumpErrors          = false;

  public int getDefaultPageSize() {
    return defaultPageSize;
  }

  public RepositoryRestConfiguration setDefaultPageSize(int defaultPageSize) {
    Assert.isTrue((defaultPageSize > 0), "Page size must be greater than 0.");
    this.defaultPageSize = defaultPageSize;
    return this;
  }

  public String getPageParamName() {
    return pageParamName;
  }

  public RepositoryRestConfiguration setPageParamName(String pageParamName) {
    Assert.notNull(pageParamName, "Page param name cannot be null.");
    this.pageParamName = pageParamName;
    return this;
  }

  public String getLimitParamName() {
    return limitParamName;
  }

  public RepositoryRestConfiguration setLimitParamName(String limitParamName) {
    Assert.notNull(limitParamName, "Limit param name cannot be null.");
    this.limitParamName = limitParamName;
    return this;
  }

  public String getSortParamName() {
    return sortParamName;
  }

  public RepositoryRestConfiguration setSortParamName(String sortParamName) {
    Assert.notNull(sortParamName, "Sort param name cannot be null.");
    this.sortParamName = sortParamName;
    return this;
  }

  public List<HttpMessageConverter<?>> getCustomConverters() {
    return customConverters;
  }

  public RepositoryRestConfiguration setCustomConverters(List<HttpMessageConverter<?>> customConverters) {
    Assert.notNull(customConverters, "Custom converters list cannot be null.");
    this.customConverters = customConverters;
    return this;
  }

  public String getJsonpParamName() {
    return jsonpParamName;
  }

  public RepositoryRestConfiguration setJsonpParamName(String jsonpParamName) {
    this.jsonpParamName = jsonpParamName;
    return this;
  }

  public String getJsonpOnErrParamName() {
    return jsonpOnErrParamName;
  }

  public RepositoryRestConfiguration setJsonpOnErrParamName(String jsonpOnErrParamName) {
    this.jsonpOnErrParamName = jsonpOnErrParamName;
    return this;
  }

  public MediaType getDefaultMediaType() {
    return defaultMediaType;
  }

  public RepositoryRestConfiguration setDefaultMediaType(MediaType defaultMediaType) {
    this.defaultMediaType = defaultMediaType;
    return this;
  }

  public boolean isDumpErrors() {
    return dumpErrors;
  }

  public RepositoryRestConfiguration setDumpErrors(boolean dumpErrors) {
    this.dumpErrors = dumpErrors;
    return this;
  }

}
