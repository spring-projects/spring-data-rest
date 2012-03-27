package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.rest.core.Handler;
import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.data.rest.core.util.UriUtils;
import org.springframework.data.rest.repository.JpaEntityMetadata;
import org.springframework.data.rest.repository.JpaRepositoryMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@Controller
public class RepositoryRestController implements InitializingBean {

  public static final String STATUS = "status";
  public static final String HEADERS = "headers";
  public static final String RESOURCE = "resource";
  public static final String SELF = "self";
  public static final String LINKS = "_links";

  public static final int HAS_RESOURCE = 1;
  public static final int HAS_RESOURCE_ID = 2;
  public static final int HAS_SECOND_LEVEL_RESOURCE = 3;
  public static final int HAS_SECOND_LEVEL_ID = 4;

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryRestController.class);

  private URI baseUri = URI.create("http://localhost:8080");
  private MediaType uriListMediaType = MediaType.parseMediaType("text/uri-list");
  private MediaType jsonMediaType = MediaType.parseMediaType("application/x-spring-data+json");
  private JpaRepositoryMetadata repositoryMetadata;
  private ConversionService conversionService = new DefaultConversionService();
  private List<HttpMessageConverter<?>> httpMessageConverters;

  public URI getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(URI baseUri) {
    this.baseUri = baseUri;
  }

  public URI baseUri() {
    return baseUri;
  }

  public RepositoryRestController baseUri(URI baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  public JpaRepositoryMetadata getRepositoryMetadata() {
    return repositoryMetadata;
  }

  public void setRepositoryMetadata(JpaRepositoryMetadata repositoryMetadata) {
    this.repositoryMetadata = repositoryMetadata;
  }

  public JpaRepositoryMetadata repositoryMetadata() {
    return repositoryMetadata;
  }

  public RepositoryRestController repositoryMetadata(JpaRepositoryMetadata repositoryMetadata) {
    this.repositoryMetadata = repositoryMetadata;
    return this;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService conversionService() {
    return conversionService;
  }

  public RepositoryRestController conversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
    return this;
  }

  public List<HttpMessageConverter<?>> getHttpMessageConverters() {
    return httpMessageConverters;
  }

  public void setHttpMessageConverters(List<HttpMessageConverter<?>> httpMessageConverters) {
    this.httpMessageConverters = httpMessageConverters;
  }

  public List<HttpMessageConverter<?>> httpMessageConverters() {
    return httpMessageConverters;
  }

  public RepositoryRestController httpMessageConverters(List<HttpMessageConverter<?>> httpMessageConverters) {
    this.httpMessageConverters = httpMessageConverters;
    return this;
  }

  public MediaType getUriListMediaType() {
    return uriListMediaType;
  }

  public void setUriListMediaType(MediaType uriListMediaType) {
    this.uriListMediaType = uriListMediaType;
  }

  public void setUriListMediaType(String uriListMediaType) {
    this.uriListMediaType = MediaType.valueOf(uriListMediaType);
  }

  public MediaType uriListMediaType() {
    return uriListMediaType;
  }

  public RepositoryRestController uriListMediaType(MediaType uriListMediaType) {
    setUriListMediaType(uriListMediaType);
    return this;
  }

  public RepositoryRestController uriListMediaType(String uriListMediaType) {
    setUriListMediaType(uriListMediaType);
    return this;
  }

  public MediaType getJsonMediaType() {
    return jsonMediaType;
  }

  public void setJsonMediaType(MediaType jsonMediaType) {
    this.jsonMediaType = jsonMediaType;
  }

  public void setJsonMediaType(String jsonMediaType) {
    this.jsonMediaType = MediaType.valueOf(jsonMediaType);
  }

  public MediaType jsonMediaType() {
    return jsonMediaType;
  }

  public RepositoryRestController jsonMediaType(MediaType jsonMediaType) {
    setJsonMediaType(jsonMediaType);
    return this;
  }

  public RepositoryRestController jsonMediaType(String jsonMediaType) {
    setJsonMediaType(jsonMediaType);
    return this;
  }

  @Override public void afterPropertiesSet() throws Exception {
    Assert.notNull(httpMessageConverters, "HttpMessageConverters cannot be null");
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(method = RequestMethod.GET)
  public void get(ServerHttpRequest request, final Model model) {
    if (validBaseUri(request.getURI())) {

      URI relativeUri = baseUri.relativize(request.getURI());
      final Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);
      final int uriCnt = uris.size();
      if (uris.size() > 0) {
        final String repoName = uris.get(0).getPath();
        final CrudRepository repo = repositoryMetadata.repositoryFor(repoName);
        if (null == repo) {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
          return;
        }
        final EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repo);
        final Class<?> domainClass = entityInfo.getJavaType();
        final Class<? extends Serializable> idType = entityInfo.getIdType();
        final EntityType entityType = repositoryMetadata.entityTypeFor(domainClass);
        final JpaEntityMetadata entityMetadata = repositoryMetadata.entityMetadataFor(domainClass);

        switch (uriCnt) {

          // List the entities
          case HAS_RESOURCE: {
            Map<String, List<Link>> resource = new HashMap<String, List<Link>>();
            List<Link> links = new ArrayList<Link>();
            Iterator iter = repo.findAll().iterator();
            while (iter.hasNext()) {
              Object o = iter.next();
              Serializable id = entityInfo.getId(o);
              links.add(new SimpleLink(o.getClass().getSimpleName(),
                                       UriComponentsBuilder.fromUri(baseUri)
                                           .pathSegment(repoName, id.toString())
                                           .build()
                                           .toUri())
              );
            }
            resource.put(LINKS, links);

            model.addAttribute(STATUS, HttpStatus.OK);
            model.addAttribute(RESOURCE, resource);
            return;
          }

          // Retrieve an entity
          case HAS_RESOURCE_ID: {
            final String sId = UriUtils.path(uris.get(1));
            Serializable serId;
            if (idType == String.class) {
              serId = sId;
            } else {
              serId = conversionService.convert(sId, idType);
            }

            final Object entity = repo.findOne(serId);
            if (null == entity) {
              model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
            } else {
              Map<String, Object> entityDto = extractPropertiesLinkAware(entity, entityMetadata, repoName, sId);
              addSelfLink(entityDto, repoName, sId);

              model.addAttribute(STATUS, HttpStatus.OK);
              model.addAttribute(RESOURCE, entityDto);
            }
            return;
          }

          // Retrieve the linked entities
          case HAS_SECOND_LEVEL_RESOURCE:
            // Retrieve a child entity
          case HAS_SECOND_LEVEL_ID: {
            final String sId = UriUtils.path(uris.get(1));
            final Serializable serId;
            if (idType == String.class) {
              serId = sId;
            } else {
              serId = conversionService.convert(sId, idType);
            }

            Object entity = repo.findOne(serId);
            if (null == entity) {
              model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
            } else {
              model.addAttribute(STATUS, HttpStatus.OK);
              final String attrName = UriUtils.path(uris.get(2));
              Attribute attr = entityType.getAttribute(attrName);
              if (null != attr) {
                Class<?> childType;
                if (attr instanceof PluralAttribute) {
                  childType = ((PluralAttribute) attr).getElementType().getJavaType();
                } else {
                  childType = attr.getJavaType();
                }
                final CrudRepository childRepo = repositoryMetadata.repositoryFor(childType);
                if (null == childRepo) {
                  model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
                  return;
                }
                final EntityInformation childEntityInfo = repositoryMetadata.entityInfoFor(childRepo);
                final JpaEntityMetadata childEntityMetadata = repositoryMetadata.entityMetadataFor(childEntityInfo.getJavaType());

                final Object child = entityMetadata.get(attrName, entity);
                if (uriCnt == 3) {
                  Map<String, Object> resource = new HashMap<String, Object>();
                  if (null != child) {
                    if (child instanceof Collection) {
                      List<Link> links = new ArrayList<Link>();
                      for (Object o : (Collection) child) {
                        String childId = childEntityInfo.getId(o).toString();
                        URI uri = UriComponentsBuilder.fromUri(baseUri)
                            .pathSegment(repoName, sId, attrName, childId)
                            .build()
                            .toUri();
                        links.add(new SimpleLink(childType.getSimpleName(), uri));
                      }
                      resource.put(LINKS, links);
                      model.addAttribute(RESOURCE, resource);
                    } else if (child instanceof Map) {
                      List<Object> links = new ArrayList<Object>();
                      for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) child).entrySet()) {
                        String childId = childEntityInfo.getId(entry.getValue()).toString();
                        URI uri = UriComponentsBuilder.fromUri(baseUri)
                            .pathSegment(repoName, sId, attrName, childId)
                            .build()
                            .toUri();
                        Object oKey = entry.getKey();
                        String sKey;
                        if (ClassUtils.isAssignable(oKey.getClass(), String.class)) {
                          sKey = (String) oKey;
                        } else {
                          sKey = conversionService.convert(oKey, String.class);
                        }
                        links.add(new SimpleLink(sKey, uri));
                      }
                      resource.put(attrName, links);
                      model.addAttribute(RESOURCE, resource);
                    } else {
                      model.addAttribute(RESOURCE, child);
                    }
                  }
                } else {
                  final String childId = UriUtils.path(uris.get(3));
                  Class<? extends Serializable> childIdType = childEntityInfo.getIdType();
                  final Serializable childSerId;
                  if (idType == String.class) {
                    childSerId = childId;
                  } else {
                    childSerId = conversionService.convert(childId, childIdType);
                  }

                  final Object o = childRepo.findOne(childSerId);
                  if (null != o) {
                    Map<String, Object> entityDto = extractPropertiesLinkAware(o,
                                                                               childEntityMetadata,
                                                                               repoName,
                                                                               sId,
                                                                               attrName);
                    addSelfLink(entityDto, repositoryMetadata.repositoryNameFor(childRepo), childId);
                    model.addAttribute(RESOURCE, entityDto);
                  } else {
                    model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
                  }
                }
              }
            }

            return;
          }

          // List the repositories
          default:
        }
      }
    } else {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    model.addAttribute(STATUS, HttpStatus.OK);

    Map<String, List<Link>> resource = new HashMap<String, List<Link>>();
    List<Link> links = new ArrayList<Link>();
    for (String name : repositoryMetadata.repositoryNames()) {
      links.add(new SimpleLink(name,
                               UriComponentsBuilder.fromUri(baseUri)
                                   .pathSegment(name)
                                   .build()
                                   .toUri())
      );
    }
    resource.put(LINKS, links);

    model.addAttribute(RESOURCE, resource);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
  public void createOrUpdate(ServerHttpRequest request, Model model) {
    if (validBaseUri(request.getURI())) {

      URI relativeUri = baseUri.relativize(request.getURI());
      final Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);
      if (LOG.isDebugEnabled()) {
        LOG.debug("uris: " + uris);
      }

      final int uriCnt = uris.size();
      if (uriCnt > 0) {
        final String repoName = UriUtils.path(uris.get(0));
        final CrudRepository repo = repositoryMetadata.repositoryFor(repoName);
        if (null == repo) {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
          return;
        }
        final EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repo);
        final Class<?> domainClass = entityInfo.getJavaType();
        final Class<? extends Serializable> idType = entityInfo.getIdType();
        final JpaEntityMetadata entityMetadata = repositoryMetadata.entityMetadataFor(domainClass);
        final MediaType incomingMediaType = request.getHeaders().getContentType();

        switch (uriCnt) {

          // Create a new entity
          case HAS_RESOURCE:
          case HAS_RESOURCE_ID: {
            if (incomingMediaType.equals(jsonMediaType)) {
              try {
                final Map incoming = readIncoming(request, incomingMediaType, Map.class);
                if (null == incoming) {
                  model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
                } else {
                  String resourceId;
                  Serializable serId = null;
                  if (uriCnt == HAS_RESOURCE_ID) {
                    resourceId = UriUtils.path(uris.get(1));
                    if (idType == String.class) {
                      serId = resourceId;
                    } else {
                      serId = conversionService.convert(resourceId, idType);
                    }
                  }

                  final Object entity = request.getMethod() == HttpMethod.PUT ?
                      repo.findOne(serId) :
                      entityMetadata.targetType().newInstance();

                  if (null == entity) {
                    model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
                    return;
                  }

                  entityMetadata.doWithEmbedded(new Handler<Attribute, Void>() {
                    @Override public Void handle(Attribute attribute) {
                      String name = attribute.getName();
                      if (incoming.containsKey(name)) {
                        Object val = incoming.get(name);
                        entityMetadata.set(name, val, entity);
                      }
                      return null;
                    }
                  });

                  if (uriCnt == HAS_RESOURCE_ID && request.getMethod() == HttpMethod.POST) {
                    entityMetadata.id(serId, entity);
                  }

                  Object savedEntity = repo.save(entity);
                  String sId = entityInfo.getId(savedEntity).toString();

                  URI selfUri = UriComponentsBuilder.fromUri(baseUri)
                      .pathSegment(repoName, sId)
                      .build()
                      .toUri();

                  if (request.getMethod() == HttpMethod.POST) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Location", selfUri.toString());
                    model.addAttribute(HEADERS, headers);
                    model.addAttribute(STATUS, HttpStatus.CREATED);
                  } else {
                    model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
                  }
                }
              } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
              }
              return;
            } else {
              model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
              return;
            }
          }

          case HAS_SECOND_LEVEL_RESOURCE: {
            String propertyName = UriUtils.path(uris.get(2));
            Attribute attr = entityMetadata.linkedAttributes().get(propertyName);
            if (null != attr) {
              Object entity = resolveTopLevelResource(request.getURI().toString());
              if (null != entity) {
                try {
                  if (incomingMediaType.equals(uriListMediaType)) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(request.getBody()));
                    String line;
                    while (null != (line = in.readLine())) {
                      String sLinkUri = line.trim();
                      Object childEntity = resolveTopLevelResource(sLinkUri);

                      if (attr instanceof PluralAttribute) {
                        PluralAttribute plAttr = (PluralAttribute) attr;
                        switch (plAttr.getCollectionType()) {
                          case COLLECTION:
                          case LIST:
                            if (request.getMethod() == HttpMethod.PUT) {
                              entityMetadata.set(propertyName, new ArrayList(), entity);
                            }
                            addToCollection(propertyName, entity, entityMetadata, ArrayList.class, childEntity);
                            break;
                          case SET:
                            if (request.getMethod() == HttpMethod.PUT) {
                              entityMetadata.set(propertyName, new HashSet(), entity);
                            }
                            addToCollection(propertyName, entity, entityMetadata, HashSet.class, childEntity);
                            break;
                          case MAP:
                            model.addAttribute(STATUS, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                            return;
                        }
                      } else if (attr instanceof SingularAttribute) {
                        entityMetadata.set(propertyName, childEntity, entity);
                      }
                    }
                    repo.save(entity);
                    if (request.getMethod() == HttpMethod.PUT) {
                      model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
                    } else {
                      model.addAttribute(STATUS, HttpStatus.CREATED);
                    }
                  } else if (incomingMediaType.equals(jsonMediaType)) {
                    final List<Map<String, String>> incoming = readIncoming(request, incomingMediaType, List.class);
                    for (Map<String, String> link : incoming) {
                      String sLinkUri = link.get("href");
                      Object childEntity = resolveTopLevelResource(sLinkUri);

                      if (attr instanceof PluralAttribute) {
                        PluralAttribute plAttr = (PluralAttribute) attr;
                        switch (plAttr.getCollectionType()) {
                          case COLLECTION:
                          case LIST:
                            if (request.getMethod() == HttpMethod.PUT) {
                              entityMetadata.set(propertyName, new ArrayList(), entity);
                            }
                            addToCollection(propertyName, entity, entityMetadata, ArrayList.class, childEntity);
                            break;
                          case SET:
                            if (request.getMethod() == HttpMethod.PUT) {
                              entityMetadata.set(propertyName, new HashSet(), entity);
                            }
                            addToCollection(propertyName, entity, entityMetadata, HashSet.class, childEntity);
                            break;
                          case MAP:
                            if (request.getMethod() == HttpMethod.PUT) {
                              entityMetadata.set(propertyName, new HashMap(), entity);
                            }
                            addToMap(propertyName, entity, entityMetadata, link.get("rel"), childEntity);
                            break;
                        }
                      } else if (attr instanceof SingularAttribute) {
                        entityMetadata.set(propertyName, childEntity, entity);
                      }
                    }
                    repo.save(entity);
                    if (request.getMethod() == HttpMethod.PUT) {
                      model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
                    } else {
                      model.addAttribute(STATUS, HttpStatus.CREATED);
                    }
                  }
                } catch (IOException e) {
                  model.addAttribute(STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
                } catch (InstantiationException e) {
                  model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
                } catch (IllegalAccessException e) {
                  model.addAttribute(STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
                }
              } else {
                model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
              }
            }
            return;
          }

          // List resources
          default:
        }
      }
    } else {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    model.addAttribute(STATUS, HttpStatus.OK);

    Map<String, List<Link>> resource = new HashMap<String, List<Link>>();
    List<Link> links = new ArrayList<Link>();
    for (String name : repositoryMetadata.repositoryNames()) {
      links.add(new SimpleLink(name,
                               UriComponentsBuilder.fromUri(baseUri)
                                   .pathSegment(name)
                                   .build()
                                   .toUri())
      );
    }
    resource.put(LINKS, links);

    model.addAttribute(RESOURCE, resource);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(method = RequestMethod.DELETE)
  public void delete(ServerHttpRequest request, Model model) {
    if (validBaseUri(request.getURI())) {

      URI relativeUri = baseUri.relativize(request.getURI());
      Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);
      if (LOG.isDebugEnabled()) {
        LOG.debug("uris: " + uris);
      }

      int uriCnt = uris.size();
      if (uriCnt > 0) {
        String repoName = UriUtils.path(uris.get(0));
        CrudRepository repo = repositoryMetadata.repositoryFor(repoName);
        if (null == repo) {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
          return;
        }
        EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repo);
        Class<?> domainClass = entityInfo.getJavaType();
        Class<? extends Serializable> idType = entityInfo.getIdType();
        JpaEntityMetadata entityMetadata = repositoryMetadata.entityMetadataFor(domainClass);

        switch (uriCnt) {

          case HAS_RESOURCE_ID: {
            String resourceId = UriUtils.path(uris.get(1));
            Serializable serId = conversionService.convert(resourceId, idType);
            repo.delete(serId);
            model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
            return;
          }

          case HAS_SECOND_LEVEL_ID: {
            String resourceId = UriUtils.path(uris.get(1));
            Serializable serId = conversionService.convert(resourceId, idType);

            Object entity = repo.findOne(serId);

            String propertyName = UriUtils.path(uris.get(2));
            Attribute attr = entityMetadata.linkedAttributes().get(propertyName);
            if (null != attr && null != entity) {
              Object childEntity = resolveSecondLevelResource(request.getURI().toString());
              if (attr instanceof PluralAttribute) {
                PluralAttribute plAttr = (PluralAttribute) attr;
                switch (plAttr.getCollectionType()) {
                  case COLLECTION:
                  case LIST:
                  case SET:
                    removeFromCollection(propertyName, entity, entityMetadata, childEntity);
                    break;
                  case MAP:
                    removeFromMap(propertyName, entity, entityMetadata, childEntity);
                    break;
                }
              } else {
                entityMetadata.set(propertyName, null, entity);
              }

              repo.save(entity);

              model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
              return;
            } else {
              model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
              return;
            }
          }

        }
      }
    } else {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    model.addAttribute(STATUS, HttpStatus.OK);

    Map<String, List<Link>> resource = new HashMap<String, List<Link>>();
    List<Link> links = new ArrayList<Link>();
    for (String name : repositoryMetadata.repositoryNames()) {
      links.add(new SimpleLink(name,
                               UriComponentsBuilder.fromUri(baseUri)
                                   .pathSegment(name)
                                   .build()
                                   .toUri())
      );
    }
    resource.put(LINKS, links);

    model.addAttribute(RESOURCE, resource);
  }


  private boolean validBaseUri(URI requestUri) {
    String path = baseUri.relativize(requestUri).getPath();
    return !StringUtils.hasText(path) || path.charAt(0) != '/';
  }

  @SuppressWarnings({"unchecked"})
  private void addSelfLink(Map<String, Object> model, String... pathComponents) {
    List<Link> links = (List<Link>) model.get(LINKS);
    if (null == links) {
      links = new ArrayList<Link>();
      model.put(LINKS, links);
    }
    URI selfUri = UriComponentsBuilder.fromUri(baseUri)
        .pathSegment(pathComponents)
        .build()
        .toUri();
    links.add(new SimpleLink(SELF, selfUri));
  }

  @SuppressWarnings({"unchecked"})
  private <V extends Serializable> V stringToSerializable(String s, Class<V> targetType) {
    if (ClassUtils.isAssignable(targetType, String.class)) {
      return (V) s;
    } else {
      return conversionService.convert(s, targetType);
    }
  }

  @SuppressWarnings({"unchecked"})
  private <V> V readIncoming(HttpInputMessage request, MediaType incomingMediaType, Class<V> targetType) throws IOException {
    for (HttpMessageConverter converter : httpMessageConverters) {
      if (converter.canRead(targetType, incomingMediaType)) {
        return (V) converter.read(targetType, request);
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  private Object resolveTopLevelResource(String uri) {
    URI href = URI.create(uri);
    if (validBaseUri(href)) {
      URI relativeUri = baseUri.relativize(href);
      Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);

      if (uris.size() > 1) {
        String repoName = UriUtils.path(uris.get(0));
        String sId = UriUtils.path(uris.get(1));

        CrudRepository repo = repositoryMetadata.repositoryFor(repoName);
        EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repo);
        Class<? extends Serializable> idType = entityInfo.getIdType();

        Serializable serId = stringToSerializable(sId, idType);

        return repo.findOne(serId);
      }
    }
    return null;

  }

  @SuppressWarnings({"unchecked"})
  private Object resolveSecondLevelResource(String uri) {
    URI href = URI.create(uri);
    if (validBaseUri(href)) {
      URI relativeUri = baseUri.relativize(href);
      Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);

      if (uris.size() > 3) {
        String topLevelRepoName = UriUtils.path(uris.get(0));
        CrudRepository topLevelRepo = repositoryMetadata.repositoryFor(topLevelRepoName);
        EntityInformation topLevelEntityInfo = repositoryMetadata.entityInfoFor(topLevelRepo);
        JpaEntityMetadata topLevelEntityMetadata = repositoryMetadata.entityMetadataFor(topLevelEntityInfo.getJavaType());

        String propertyName = UriUtils.path(uris.get(2));
        Attribute attr = topLevelEntityMetadata.linkedAttributes().get(propertyName);
        if (null != attr) {
          CrudRepository secondLevelRepo;
          if (attr instanceof PluralAttribute) {
            secondLevelRepo = repositoryMetadata.repositoryFor(((PluralAttribute) attr).getElementType().getJavaType());
          } else {
            secondLevelRepo = repositoryMetadata.repositoryFor(attr.getJavaType());
          }
          EntityInformation secondLevelEntityInfo = repositoryMetadata.entityInfoFor(secondLevelRepo);
          Class<? extends Serializable> secondLevelIdType = secondLevelEntityInfo.getIdType();
          Serializable secondLevelId = stringToSerializable(UriUtils.path(uris.get(3)), secondLevelIdType);

          return secondLevelRepo.findOne(secondLevelId);
        }
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  private <V extends Collection> void addToCollection(String name,
                                                      Object entity,
                                                      JpaEntityMetadata metadata,
                                                      Class<V> containerClass,
                                                      Object obj)
      throws IllegalAccessException,
             InstantiationException {
    Collection c = (V) metadata.get(name, entity);
    if (null == c) {
      c = containerClass.newInstance();
      metadata.set(name, c, entity);
    }
    c.add(obj);
  }

  public void removeFromCollection(String name,
                                   Object entity,
                                   JpaEntityMetadata metadata,
                                   Object obj) {
    Collection c = (Collection) metadata.get(name, entity);
    if (null != c) {
      c.remove(obj);
    }
  }

  @SuppressWarnings({"unchecked"})
  private void addToMap(String name,
                        Object entity,
                        JpaEntityMetadata metadata,
                        Object key,
                        Object obj)
      throws IllegalAccessException,
             InstantiationException {
    Map m = (Map) metadata.get(name, entity);
    if (null == m) {
      m = new HashMap();
      metadata.set(name, m, entity);
    }
    m.put(key, obj);
  }

  @SuppressWarnings({"unchecked"})
  public void removeFromMap(String name,
                            Object entity,
                            JpaEntityMetadata metadata,
                            Object obj) {
    Map<Object, Object> m = (Map<Object, Object>) metadata.get(name, entity);
    if (null != m) {
      LOG.debug("obj: " + obj);
      for (Map.Entry<Object, Object> entry : m.entrySet()) {
        LOG.debug("key: " + entry.getKey());
        LOG.debug("value: " + entry.getValue());
        LOG.debug("remove? " + (entry.getValue() == obj || entry.getValue().equals(obj)));
        if (entry.getValue() == obj || entry.getValue().equals(obj)) {
          m.remove(entry.getKey());
        }
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  private Map<String, Object> extractPropertiesLinkAware(final Object entity,
                                                         final JpaEntityMetadata entityMetadata,
                                                         final String... pathSegs) {
    final Map<String, Object> entityDto = new HashMap<String, Object>();

    entityMetadata.doWithEmbedded(new Handler<Attribute, Void>() {
      @Override public Void handle(Attribute attr) {
        String name = attr.getName();
        Object val = entityMetadata.get(name, entity);
        if (null != val) {
          entityDto.put(name, val);
        }
        return null;
      }
    });

    entityMetadata.doWithLinked(new Handler<Attribute, Void>() {
      @Override public Void handle(Attribute attr) {
        String name = attr.getName();
        URI uri = UriComponentsBuilder.fromUri(baseUri)
            .pathSegment(pathSegs)
            .pathSegment(name)
            .build()
            .toUri();
        Link l = new SimpleLink(name, uri);
        List<Link> links = (List<Link>) entityDto.get(LINKS);
        if (null == links) {
          links = new ArrayList<Link>();
          entityDto.put(LINKS, links);
        }
        links.add(l);
        return null;
      }
    });

    return entityDto;
  }

}
