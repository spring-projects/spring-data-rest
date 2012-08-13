package org.springframework.data.rest.core;

import java.net.URI;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Implementation of {@link Link}.
 *
 * @author Jon Brisbin
 */
public class ResourceLink implements Link, Comparable<Link> {

  @JsonProperty("rel")
  private String rel;
  @JsonProperty("href")
  private URI    href;

  public ResourceLink() {
  }

  public ResourceLink(String rel, URI href) {
    this.rel = rel;
    this.href = href;
  }

  @Override public String rel() {
    return rel;
  }

  @Override public URI href() {
    return href;
  }

  @Override public int compareTo(Link link) {
    if(null == rel || null == link.rel()) {
      return -1;
    }

    int i = rel.compareTo(link.rel());
    if(i != 0) {
      return i;
    }

    if(null == href || null == link.href()) {
      return -1;
    }

    return (href.compareTo(link.href()));
  }

  @Override public String toString() {
    return "ResourceLink{" +
        "rel='" + rel + '\'' +
        ", href=" + href +
        '}';
  }

}
