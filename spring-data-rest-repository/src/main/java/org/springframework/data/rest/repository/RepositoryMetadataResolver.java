package org.springframework.data.rest.repository;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.rest.core.Resolver;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public class RepositoryMetadataResolver<M extends RepositoryMetadata<E>, E extends EntityMetadata<? extends AttributeMetadata>>
    implements Resolver<M>,
               ApplicationContextAware {

  private ApplicationContext applicationContext;
  @Autowired(required = false)
  private List<RepositoryExporter> repositoryExporters = Collections.emptyList();

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public List<RepositoryExporter> getRepositoryExporters() {
    return repositoryExporters;
  }

  public RepositoryMetadataResolver<M, E> setRepositoryExporters(List<RepositoryExporter> repositoryExporters) {
    Assert.notNull(repositoryExporters, "List of RepositoryExporters cannot be null!");
    this.repositoryExporters = repositoryExporters;
    return this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public M resolve(URI baseUri, URI uri) {
    URI tail;
    if(null == (tail = tail(baseUri, uri))) {
      return null;
    }

    String path = tail.getPath();
    for(RepositoryExporter exporter : repositoryExporters) {
      RepositoryMetadata repoMeta;
      if(null != (repoMeta = exporter.repositoryMetadataFor(path))) {
        return (M)repoMeta;
      }
    }

    return null;
  }

}
