package org.springframework.data.rest.repository.context;

import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.http.server.ServerHttpRequest;

/**
 * @author Jon Brisbin
 */
public class BeforeRenderResourceEvent extends RenderEvent {
  public BeforeRenderResourceEvent(ServerHttpRequest request, RepositoryMetadata repoMeta, Object source) {
    super(request, repoMeta, source);
  }
}
