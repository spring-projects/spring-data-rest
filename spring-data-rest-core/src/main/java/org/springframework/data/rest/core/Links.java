package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class Links {

  private List<Link> links = new ArrayList<Link>();

  public Links add(Link link) {
    links.add(link);
    return this;
  }

  @JsonProperty("_links")
  public List<Link> getLinks() {
    return this.links;
  }

}
