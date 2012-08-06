package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Abstraction for representing resources to the user agent.
 *
 * @author Jon Brisbin
 */
@SuppressWarnings({"unchecked"})
public class Resources extends LinkAware<Resources> {

  @JsonProperty("content")
  protected List<Resource<?>> resources = new ArrayList<Resource<?>>();

  public List getResources() {
    return resources;
  }

  public Resources setResources(List resources) {
    if(null == resources) {
      this.resources = Collections.emptyList();
    } else {
      this.resources = resources;
    }
    return this;
  }

  public Resources addResource(Resource<?> resource) {
    resources.add((null == resource ? new Resource<Object>() : resource));
    return this;
  }

}
