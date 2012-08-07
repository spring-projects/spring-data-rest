package org.springframework.data.rest.repository.context;

import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.http.server.ServerHttpRequest;

/**
 * Event emitted before the the object is rendered to the client. Implementations of {@link
 * AbstractRepositoryEventListener} can listen for these events and alter the output of the resource being sent to the
 * link.
 *
 * @author Jon Brisbin
 */
public class BeforeRenderResourcesEvent extends RenderEvent {
  public BeforeRenderResourcesEvent(ServerHttpRequest request, RepositoryMetadata repoMeta, Object source) {
    super(request, repoMeta, source);
  }
}
