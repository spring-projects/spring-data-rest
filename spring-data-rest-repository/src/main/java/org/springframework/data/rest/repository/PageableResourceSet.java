package org.springframework.data.rest.repository;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.rest.core.ResourceSet;

/**
 * @author Jon Brisbin
 */
public class PageableResourceSet extends ResourceSet {

  @JsonProperty("page")
  protected PagingMetadata paging = new PagingMetadata(-1, 20, 0, 0);

  public PagingMetadata getPaging() {
    return paging;
  }

  public PageableResourceSet setPaging(PagingMetadata paging) {
    this.paging = paging;
    return this;
  }

}
