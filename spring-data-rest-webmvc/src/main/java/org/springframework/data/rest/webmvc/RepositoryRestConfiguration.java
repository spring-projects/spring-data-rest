package org.springframework.data.rest.webmvc;

import java.util.Collections;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestConfiguration {

  public static final RepositoryRestConfiguration DEFAULT = new RepositoryRestConfiguration();

  private int defaultPageSize = 20;
  private String pageParamName = "page";
  private String limitParamName = "limit";
  private String sortParamName = "sort";
  private List<HttpMessageConverter<?>> customConverters = Collections.emptyList();

  public int getDefaultPageSize() {
    return defaultPageSize;
  }

  public RepositoryRestConfiguration setDefaultPageSize( int defaultPageSize ) {
    this.defaultPageSize = defaultPageSize;
    return this;
  }

  public String getPageParamName() {
    return pageParamName;
  }

  public RepositoryRestConfiguration setPageParamName( String pageParamName ) {
    this.pageParamName = pageParamName;
    return this;
  }

  public String getLimitParamName() {
    return limitParamName;
  }

  public RepositoryRestConfiguration setLimitParamName( String limitParamName ) {
    this.limitParamName = limitParamName;
    return this;
  }

  public String getSortParamName() {
    return sortParamName;
  }

  public RepositoryRestConfiguration setSortParamName( String sortParamName ) {
    this.sortParamName = sortParamName;
    return this;
  }

  public List<HttpMessageConverter<?>> getCustomConverters() {
    return customConverters;
  }

  public RepositoryRestConfiguration setCustomConverters( List<HttpMessageConverter<?>> customConverters ) {
    this.customConverters = customConverters;
    return this;
  }

}
