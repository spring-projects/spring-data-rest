package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.util.Assert;

/**
 * Abstraction for representing resources to the user agent.
 *
 * @author Jon Brisbin
 */
@SuppressWarnings({"unchecked"})
public class ResourceSet {

  @JsonProperty("content")
  protected List<Resource<?>> resources = new ArrayList<Resource<?>>();
  @JsonProperty("links")
  protected Set<Link>         links     = new HashSet<Link>();

  /**
   * Get the {@link Resource}s this {@literal ResourceSet} manages.
   *
   * @return
   */
  public List<Resource<?>> getResources() {
    return resources;
  }

  /**
   * Set the {@link Resource}s this {@literal ResourceSet} manages.
   *
   * @param resources
   *
   * @return
   */
  public ResourceSet setResources(List<Resource<?>> resources) {
    if(null == resources) {
      this.resources = Collections.emptyList();
    } else {
      this.resources = resources;
    }
    return this;
  }

  /**
   * Add a {@link Resource} to this set.
   *
   * @param resource
   *
   * @return {@literal this}
   */
  public ResourceSet addResource(Resource<?> resource) {
    resources.add((null == resource ? new Resource<Object>() : resource));
    return this;
  }

  /**
   * Get the set of {@link Link}s.
   *
   * @return
   */
  public Set<Link> getLinks() {
    return links;
  }

  /**
   * Set the {@link Link}s this {@literal ResourceSet} manages.
   *
   * @param links
   *
   * @return {@literal this}
   */
  @SuppressWarnings({"unchecked"})
  public ResourceSet setLinks(Set<Link> links) {
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
  public ResourceSet addLink(Link link) {
    Assert.notNull(link, "Link cannot be null.");
    links.add(link);
    return this;
  }

}
