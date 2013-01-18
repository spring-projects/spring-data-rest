package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.webmvc.support.PagingAndSorting;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
class RepositoryRestRequest {

  private final RepositoryRestConfiguration config;
  private final HttpServletRequest          request;
  private final PagingAndSorting            pagingAndSorting;
  private final URI                         baseUri;
  private final RepositoryInformation       repoInfo;
  private final ResourceMapping             repoMapping;
  private final Link                        repoLink;
  private final Object                      repository;
  private final RepositoryMethodInvoker     repoMethodInvoker;
  private final PersistentEntity            persistentEntity;
  private final ResourceMapping             entityMapping;

  public RepositoryRestRequest(RepositoryRestConfiguration config,
                               Repositories repositories,
                               HttpServletRequest request,
                               PagingAndSorting pagingAndSorting,
                               URI baseUri,
                               RepositoryInformation repoInfo) {
    this.config = config;
    this.request = request;
    this.pagingAndSorting = pagingAndSorting;
    this.baseUri = baseUri;
    this.repoInfo = repoInfo;
    this.repoMapping = getResourceMapping(config, repoInfo);
    if(null == repoMapping) {
      this.repoLink = null;
      this.repository = null;
      this.repoMethodInvoker = null;
      this.persistentEntity = null;
      this.entityMapping = null;
    } else {
      this.repoLink = new Link(buildUri(baseUri, repoMapping.getPath()).toString(), repoMapping.getRel());
      this.repository = repositories.getRepositoryFor(repoInfo.getDomainType());
      this.persistentEntity = repositories.getPersistentEntity(repoInfo.getDomainType());
      this.repoMethodInvoker = new RepositoryMethodInvoker(repository, repoInfo, persistentEntity);
      this.entityMapping = getResourceMapping(config, persistentEntity);
    }
  }

  HttpServletRequest getRequest() {
    return request;
  }

  PagingAndSorting getPagingAndSorting() {
    return pagingAndSorting;
  }

  URI getBaseUri() {
    return baseUri;
  }

  RepositoryInformation getRepositoryInformation() {
    return repoInfo;
  }

  ResourceMapping getRepositoryResourceMapping() {
    return repoMapping;
  }

  Link getRepositoryLink() {
    return repoLink;
  }

  Object getRepository() {
    return repository;
  }

  RepositoryMethodInvoker getRepositoryMethodInvoker() {
    return repoMethodInvoker;
  }

  PersistentEntity getPersistentEntity() {
    return persistentEntity;
  }

  ResourceMapping getPersistentEntityResourceMapping() {
    return entityMapping;
  }

  void addNextLink(Page page, List<Link> links) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);
    // Add existing query parameters
    addQueryParameters(request, builder);

    builder.queryParam(config.getPageParamName(), page.getNumber() + 1)
           .queryParam(config.getLimitParamName(), pagingAndSorting.getPageSize());

    links.add(new Link(builder.build().toString(), "page.next"));
  }

  void addPrevLink(Page page, List<Link> links) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);
    // Add existing query parameters
    addQueryParameters(request, builder);

    builder.queryParam(config.getPageParamName(), page.getNumber() - 1)
           .queryParam(config.getLimitParamName(), pagingAndSorting.getPageSize());

    links.add(new Link(builder.build().toString(), "page.previous"));
  }

  @SuppressWarnings({"unchecked"}) Link buildEntitySelfLink(Object o, ConversionService conversionService) {
    BeanWrapper bean = BeanWrapper.create(o, conversionService);
    Object id = bean.getProperty(persistentEntity.getIdProperty());
    URI uri = buildUri(baseUri, repoMapping.getPath(), id.toString());
    return new Link(uri.toString(), "self");
  }

  private void addQueryParameters(HttpServletRequest request,
                                  UriComponentsBuilder builder) {
    for(Enumeration<String> names = request.getParameterNames(); names.hasMoreElements(); ) {
      String name = names.nextElement();
      String value = request.getParameter(name);
      if(name.equals(config.getPageParamName()) || name.equals(config.getLimitParamName())) {
        continue;
      }

      builder.queryParam(name, value);
    }
  }

}
