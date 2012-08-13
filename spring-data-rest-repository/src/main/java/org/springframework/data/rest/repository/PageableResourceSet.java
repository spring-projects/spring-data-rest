package org.springframework.data.rest.repository;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.rest.core.ResourceSet;

/**
 * @author Jon Brisbin
 */
public class PageableResourceSet extends ResourceSet {

  protected long           resourceCount = 0;
  @JsonProperty("page")
  protected PagingMetadata paging        = new PagingMetadata(-1, 0);

  public long getResourceCount() {
    return resourceCount;
  }

  public PageableResourceSet setResourceCount(long resourceCount) {
    this.resourceCount = resourceCount;
    return this;
  }

  public PagingMetadata getPaging() {
    return paging;
  }

  public PageableResourceSet setPaging(PagingMetadata paging) {
    this.paging = paging;
    return this;
  }

}
