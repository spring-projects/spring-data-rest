package org.springframework.data.rest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the {@link ResourceMapping} configurations for any resources being exported. This includes domain entities
 * and repositories.
 *
 * @author Jon Brisbin
 */
public class ResourceMappingConfiguration {

  private final Map<Class<?>, ResourceMapping> resourceMappings = new HashMap<Class<?>, ResourceMapping>();

  public ResourceMapping addResourceMappingFor(Class<?> type) {
    ResourceMapping rm = resourceMappings.get(type);
    if(null == rm) {
      rm = new ResourceMapping(type);
      resourceMappings.put(type, rm);
    }
    return rm;
  }

  public ResourceMapping getResourceMappingFor(Class<?> type) {
    return resourceMappings.get(type);
  }

  public boolean hasResourceMappingFor(Class<?> type) {
    return resourceMappings.containsKey(type);
  }

  public Class<?> findTypeForPath(String path) {
    if(null == path) {
      return null;
    }
    for(Map.Entry<Class<?>, ResourceMapping> entry : resourceMappings.entrySet()) {
      if(path.equals(entry.getValue().getPath())) {
        return entry.getKey();
      }
    }
    return null;
  }

}
