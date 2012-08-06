package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class Links {

  private List<Link> links = new ArrayList<Link>();

  public Links add(Link link) {
    links.add(link);
    return this;
  }

  public List<Link> getLinks() {
    return this.links;
  }

}
