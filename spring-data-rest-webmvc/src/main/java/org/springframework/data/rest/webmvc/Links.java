package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.SimpleLink;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Links {

  private List<SimpleLink> links = new ArrayList<SimpleLink>();

  public Links add(SimpleLink link) {
    links.add(link);
    return this;
  }

  @JsonProperty("_links")
  public List<SimpleLink> getLinks() {
    return this.links;
  }

}
