package org.springframework.data.rest.core;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonUnwrapped;

/**
 * Wraps a simple object or a bean plus a set of links.
 *
 * @author Jon Brisbin
 */
public class Resource<T> extends LinkAware<Resource<T>> {

  @JsonUnwrapped
  protected T resource;

  public Resource() {
  }

  public Resource(T resource) {
    this.resource = resource;
  }

  public T getResource() {
    return resource;
  }

  public void setResource(T resource) {
    this.resource = resource;
  }

}
