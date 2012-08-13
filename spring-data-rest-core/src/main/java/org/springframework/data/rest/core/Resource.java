package org.springframework.data.rest.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.springframework.util.Assert;

/**
 * Wraps a simple object or a bean plus a set of links.
 *
 * @author Jon Brisbin
 */
public class Resource<T> {

  @JsonUnwrapped
  protected T resource;
  @JsonProperty("links")
  protected Set<Link> links = new HashSet<Link>();

  public Resource() {
  }

  public Resource(T resource) {
    this.resource = resource;
  }

  /**
   * Get the resource. May be {@literal null}.
   *
   * @return The resource or {@literal null}.
   */
  public T getResource() {
    return resource;
  }

  /**
   * Set the resource.
   *
   * @param resource
   *
   * @return {@literal this}
   */
  public Resource<T> setResource(T resource) {
    this.resource = resource;
    return this;
  }

  /**
   * Get the set of {@link Link}s for this resource.
   *
   * @return
   */
  public Set<Link> getLinks() {
    return links;
  }

  /**
   * Set the entire set of {@link Link}s.
   *
   * @param links
   *
   * @return {@literal this}
   */
  @SuppressWarnings({"unchecked"})
  public Resource<T> setLinks(Set<Link> links) {
    if(null == links) {
      this.links = Collections.emptySet();
    } else {
      this.links = links;
    }
    return this;
  }

  /**
   * Add a {@link Link} to this resource's set.
   *
   * @param link
   *
   * @return {@literal this}
   */
  public Resource<T> addLink(Link link) {
    Assert.notNull(link, "Link cannot be null.");
    links.add(link);
    return this;
  }

}
