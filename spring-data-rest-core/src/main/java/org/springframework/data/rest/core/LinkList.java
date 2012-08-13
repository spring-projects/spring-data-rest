package org.springframework.data.rest.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple abstraction for representing a list of {@link Link}s.
 *
 * @author Jon Brisbin
 */
public class LinkList {

  private List<Link> links = new ArrayList<Link>();

  /**
   * Add a {@link Link} to this list.
   *
   * @param link
   *
   * @return
   */
  public LinkList add(Link link) {
    links.add(link);
    return this;
  }

  /**
   * Get the {@link Link}s in this list.
   *
   * @return
   */
  public List<Link> getLinks() {
    return this.links;
  }

}
