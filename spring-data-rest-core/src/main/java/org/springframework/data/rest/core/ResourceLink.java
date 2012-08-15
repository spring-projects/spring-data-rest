package org.springframework.data.rest.core;

import java.net.URI;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.util.Assert;

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

  public String getRel() {
    return rel();
  }

  public ResourceLink setRel(String rel) {
    this.rel = rel;
    return this;
  }

  public URI getHref() {
    return href();
  }

  public ResourceLink setHref(URI href) {
    Assert.notNull(href, "href URI cannot be null.");
    this.href = href;
    return this;
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
