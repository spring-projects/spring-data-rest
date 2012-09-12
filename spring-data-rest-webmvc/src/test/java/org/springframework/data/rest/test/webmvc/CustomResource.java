package org.springframework.data.rest.test.webmvc;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * @author Jon Brisbin
 */
public class CustomResource extends Resource<Map<String, Object>> {

  public CustomResource(Map<String, Object> properties) {
    super(properties);
  }

  @JsonProperty("@id")
  public String getSelfLink() {
    return super.getId().getHref();
  }

  @JsonProperty("_links")
  @Override public List<Link> getLinks() {
    return super.getLinks();
  }

  @JsonAnyGetter
  @Override public Map<String, Object> getContent() {
    return super.getContent();
  }

}
