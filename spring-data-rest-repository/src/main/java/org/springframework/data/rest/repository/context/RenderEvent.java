package org.springframework.data.rest.repository.context;

import org.springframework.data.rest.core.Resource;
import org.springframework.data.rest.core.ResourceSet;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public abstract class RenderEvent extends RepositoryEvent {

  protected final ServerHttpRequest  request;
  protected final RepositoryMetadata repositoryMetadata;
  protected final boolean            topLevelResource;

  public RenderEvent(ServerHttpRequest request, RepositoryMetadata repoMeta, Object source) {
    super(source);
    Assert.isTrue(source instanceof Resource || source instanceof ResourceSet,
                  "Event source must be of type 'Resource' or 'ResourceSet'");
    this.request = request;
    this.repositoryMetadata = repoMeta;
    this.topLevelResource = (source instanceof ResourceSet);
  }

  public ServerHttpRequest getRequest() {
    return request;
  }

  public RepositoryMetadata getRepositoryMetadata() {
    return repositoryMetadata;
  }

  public Resource getResource() {
    if(getSource() instanceof Resource) {
      return (Resource)getSource();
    } else {
      throw new IllegalStateException("Source of event is not a Resource, it's " + source);
    }
  }

  public ResourceSet getResources() {
    if(getSource() instanceof ResourceSet) {
      return (ResourceSet)getSource();
    } else {
      throw new IllegalStateException("Source of event is not a Resources, it's " + source);
    }
  }

  public boolean isTopLevelResource() {
    return topLevelResource;
  }

}
