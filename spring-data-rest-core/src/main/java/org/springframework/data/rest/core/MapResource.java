package org.springframework.data.rest.core;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Abstraction that represents a Map-like REST resource plus a set of links.
 *
 * @author Jon Brisbin
 */
public class MapResource extends Resource<Map<String, Object>> {

  public MapResource() {
  }

  public MapResource(Map<String, Object> resource) {
    this.resource = resource;
  }

  @Override
  @JsonIgnore
  public Map<String, Object> getResource() {
    return resource;
  }

  @JsonAnyGetter
  public Map<String, Object> any() {
    return resource;
  }

}
