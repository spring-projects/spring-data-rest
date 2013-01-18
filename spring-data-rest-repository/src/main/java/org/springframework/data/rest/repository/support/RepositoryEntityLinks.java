package org.springframework.data.rest.repository.support;

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.net.URI;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.core.AbstractEntityLinks;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
public class RepositoryEntityLinks extends AbstractEntityLinks {

  private final URI                         baseUri;
  private final Repositories                repositories;
  private final RepositoryRestConfiguration config;

  public RepositoryEntityLinks(URI baseUri,
                               Repositories repositories,
                               RepositoryRestConfiguration config) {
    this.baseUri = baseUri;
    this.repositories = repositories;
    this.config = config;
  }

  @Override public boolean supports(Class<?> delimiter) {
    PersistentEntity persistentEntity = repositories.getPersistentEntity(delimiter);
    return (null != persistentEntity);
  }

  @Override public LinkBuilder linkFor(Class<?> type) {
    RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(type);
    PersistentEntity persistentEntity = repositories.getPersistentEntity(type);
    if(null == persistentEntity) {
      throw new IllegalArgumentException(type + " is not managed by any repository.");
    }
    return new PersistentEntityLinkBuilder(baseUri, repoInfo, persistentEntity);
  }

  @Override public LinkBuilder linkFor(Class<?> type, Object... parameters) {
    return linkFor(type);
  }

  @Override public Link linkToCollectionResource(Class<?> type) {
    RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(type);
    if(null == repoInfo) {
      throw new IllegalArgumentException(type + " is not managed by any repository.");
    }
    ResourceMapping mapping = getResourceMapping(config, repoInfo);
    return linkFor(type).withRel(mapping.getRel());
  }

  @Override public Link linkToSingleResource(Class<?> type, Object id) {
    RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(type);
    if(null == repoInfo) {
      throw new IllegalArgumentException(type + " is not managed by any repository.");
    }
    ResourceMapping repoMapping = getResourceMapping(config, repoInfo);
    PersistentEntity persistentEntity = repositories.getPersistentEntity(type);
    ResourceMapping entityMapping = getResourceMapping(config, persistentEntity);
    return linkFor(type).slash(id).withRel(repoMapping.getRel() + "." + entityMapping.getRel());
  }

  private class PersistentEntityLinkBuilder implements LinkBuilder {
    private final UriComponentsBuilder builder;
    private final ResourceMapping      repoMapping;
    private final ResourceMapping      entityMapping;

    private PersistentEntityLinkBuilder(URI baseUri,
                                        RepositoryInformation repoInfo,
                                        PersistentEntity persistentEntity) {
      this.repoMapping = getResourceMapping(config, repoInfo);
      this.entityMapping = getResourceMapping(config, persistentEntity);
      this.builder = UriComponentsBuilder.fromUri(buildUri(baseUri, repoMapping.getPath()));
    }

    @Override public LinkBuilder slash(Object object) {
      String path = String.format("%s", object);
      if(object instanceof PersistentProperty) {
        String propName = ((PersistentProperty)object).getName();
        if(entityMapping.hasResourceMappingFor(propName)) {
          path = entityMapping.getResourceMappingFor(propName).getPath();
        }
      }
      builder.pathSegment(path);
      return this;
    }

    @Override public LinkBuilder slash(Identifiable<?> identifiable) {
      return slash(identifiable.getId());
    }

    @Override public URI toUri() {
      return builder.build().toUri();
    }

    @Override public Link withRel(String rel) {
      return new Link(builder.build().toUriString(), rel);
    }

    @Override public Link withSelfRel() {
      return withRel("self");
    }
  }

}
