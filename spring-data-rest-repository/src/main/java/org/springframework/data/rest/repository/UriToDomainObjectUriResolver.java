package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.UriResolver;
import org.springframework.data.rest.core.util.UriUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ClassUtils;

/**
 * @author Jon Brisbin
 */
public class UriToDomainObjectUriResolver
    extends RepositoryExporterSupport<UriToDomainObjectUriResolver>
    implements UriResolver<Object> {

  @Autowired(required = false)
  private List<ConversionService> conversionServices = Arrays.<ConversionService>asList(new DefaultFormattingConversionService());

  public List<ConversionService> getConversionServices() {
    return conversionServices;
  }

  public UriToDomainObjectUriResolver setConversionServices(List<ConversionService> conversionServices) {
    this.conversionServices = conversionServices;
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
    Serializable serId = null;
    if(ClassUtils.isAssignable(idType, String.class)) {
      serId = sId;
    } else {
      for(ConversionService cs : conversionServices) {
        if(cs.canConvert(String.class, idType)) {
          serId = cs.convert(sId, idType);
          break;
        }
      }
    }

    return repo.findOne(serId);
  }

}
