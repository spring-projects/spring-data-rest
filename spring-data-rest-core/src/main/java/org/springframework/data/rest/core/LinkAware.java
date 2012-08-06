package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public abstract class LinkAware<T extends LinkAware<? super T>> {

  @JsonProperty("links")
  protected List<Link> links = new ArrayList<Link>();

  public List<Link> getLinks() {
    return links;
  }

  @SuppressWarnings({"unchecked"})
  public T setLinks(List<Link> links) {
    if(null == links) {
      this.links = Collections.emptyList();
    } else {
      this.links = links;
    }
    return (T)this;
  }

  @SuppressWarnings({"unchecked"})
  public T addLink(Link link) {
    Assert.notNull(link, "Link cannot be null!");
    links.add(link);
    return (T)this;
  }

}
