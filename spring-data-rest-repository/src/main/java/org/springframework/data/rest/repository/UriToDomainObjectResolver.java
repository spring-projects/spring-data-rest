package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.net.URI;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.Resolver;
import org.springframework.data.rest.core.util.UriUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ClassUtils;

/**
 * @author Jon Brisbin
 */
public class UriToDomainObjectResolver
    extends RepositoryExporterSupport<UriToDomainObjectResolver>
    implements Resolver<Object> {

  @Autowired(required = false)
  private ConversionService conversionService = new DefaultFormattingConversionService();

  public ConversionService getConversionService() {
    return conversionService;
  }

  public UriToDomainObjectResolver setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
    return this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public Object resolve(URI baseUri, URI uri) {
    URI relativeUri = baseUri.relativize(uri);
    Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);

    if(uris.size() < 1) {
      return null;
    }

    String repoName = UriUtils.path(uris.get(0));
    String sId = UriUtils.path(uris.get(1));

    RepositoryMetadata repoMeta = repositoryMetadataFor(repoName);

    CrudRepository repo;
    if(null == (repo = repoMeta.repository())) {
      return null;
    }

    EntityMetadata entityMeta;
    if(null == (entityMeta = repoMeta.entityMetadata())) {
      return null;
    }

    Class<? extends Serializable> idType = (Class<? extends Serializable>)entityMeta.idAttribute().type();
    Serializable serId;
    if(ClassUtils.isAssignable(idType, String.class)) {
      serId = sId;
    } else {
      serId = conversionService.convert(sId, idType);
    }

    return repo.findOne(serId);
  }

}
