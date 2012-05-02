package org.springframework.data.rest.core;

import java.net.URI;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class SimpleLink implements Link {

  private String rel;
  private URI href;

  public SimpleLink() {
  }

  public SimpleLink(String rel, URI href) {
    this.rel = rel;
    this.href = href;
  }

  @Override public String rel() {
    return rel;
  }

  @Override public URI href() {
    return href;
  }

  @Override public String toString() {
    return "SimpleLink{" +
        "rel='" + rel + '\'' +
        ", href=" + href +
        '}';
  }

}
