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
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.web.bind.annotation.PathVariable;
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
  public static final String LOCATION = "Location";
  public static final String RESOURCE = "resource";
  public static final String SELF = "self";
  public static final String LINKS = "_links";

  public static final int HAS_RESOURCE = 1;
  public static final int HAS_RESOURCE_ID = 2;
  public static final int HAS_SECOND_LEVEL_RESOURCE = 3;
  public static final int HAS_SECOND_LEVEL_ID = 4;

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryRestController.class);

  private MediaType uriListMediaType = MediaType.parseMediaType("text/uri-list");
  private MediaType jsonMediaType = MediaType.parseMediaType("application/x-spring-data+json");
  private JpaRepositoryMetadata repositoryMetadata;
  private Map<CrudRepository, TypeMetaCacheEntry> typeMetaCache = new ConcurrentHashMap<CrudRepository, TypeMetaCacheEntry>();
  private ConversionService conversionService = new DefaultConversionService();
  private List<HttpMessageConverter<?>> httpMessageConverters;

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

  @RequestMapping(
      value = "/",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public void listRepositories(UriComponentsBuilder uriBuilder,
                               Model model) {
    URI baseUri = uriBuilder.build().toUri();

    Links links = new Links();
    for (String name : repositoryMetadata.repositoryNames()) {
      links.add(new SimpleLink(name, buildUri(baseUri, name)));
    }

    model.addAttribute(STATUS, HttpStatus.OK);
    model.addAttribute(RESOURCE, links);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public void listEntities(UriComponentsBuilder uriBuilder,
                           @PathVariable String repository,
                           Model model) {
    URI baseUri = uriBuilder.build().toUri();

    final CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Links links = new Links();

    Iterator iter = repo.findAll().iterator();
    while (iter.hasNext()) {
      Object o = iter.next();
      Serializable id = typeMeta.entityInfo.getId(o);
      links.add(new SimpleLink(o.getClass().getSimpleName(), buildUri(baseUri, repository, id.toString())));
    }

    model.addAttribute(STATUS, HttpStatus.OK);
    model.addAttribute(RESOURCE, links);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.POST,
      produces = {
          "application/json"
      }
  )
  public void create(ServerHttpRequest request,
                     UriComponentsBuilder uriBuilder,
                     @PathVariable String repository,
                     Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_IMPLEMENTED);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    MediaType incomingMediaType = request.getHeaders().getContentType();
    try {
      final Object incoming = readIncoming(request, incomingMediaType, typeMeta.domainClass);
      if (null == incoming) {
        model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
      } else {
        Object savedEntity = repo.save(incoming);
        String sId = typeMeta.entityInfo.getId(savedEntity).toString();

        URI selfUri = buildUri(baseUri, repository, sId);

        HttpHeaders headers = new HttpHeaders();
        headers.set(LOCATION, selfUri.toString());

        model.addAttribute(HEADERS, headers);
        model.addAttribute(STATUS, HttpStatus.CREATED);
      }
    } catch (IOException e) {
      model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
      LOG.error(e.getMessage(), e);
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public void entity(ServerHttpRequest request,
                     UriComponentsBuilder uriBuilder,
                     @PathVariable String repository,
                     @PathVariable String id,
                     Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      HttpHeaders headers = new HttpHeaders();
      Object version = typeMeta.entityMetadata.version(entity);
      if (null != version) {
        List<String> etags = request.getHeaders().getIfNoneMatch();
        for (String etag : etags) {
          if (("\"" + version.toString() + "\"").equals(etag)) {
            model.addAttribute(STATUS, HttpStatus.NOT_MODIFIED);
            return;
          }
        }
        headers.set("ETag", "\"" + version.toString() + "\"");
      }
      Map<String, Object> entityDto = extractPropertiesLinkAware(entity,
                                                                 typeMeta.entityMetadata,
                                                                 UriComponentsBuilder.fromUri(baseUri)
                                                                     .pathSegment(repository, id)
                                                                     .build()
                                                                     .toUri());
      addSelfLink(baseUri, entityDto, repository, id);

      model.addAttribute(HEADERS, headers);
      model.addAttribute(STATUS, HttpStatus.OK);
      model.addAttribute(RESOURCE, entityDto);
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = {
          RequestMethod.PUT,
          RequestMethod.POST
      },
      consumes = {
          "application/json"
      },
      produces = {
          "application/json"
      }
  )
  public void createOrUpdate(ServerHttpRequest request,
                             UriComponentsBuilder uriBuilder,
                             @PathVariable String repository,
                             @PathVariable String id,
                             Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_IMPLEMENTED);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    Object entity = null;
    switch (request.getMethod()) {
      case POST:
        try {
          entity = typeMeta.domainClass.newInstance();
        } catch (InstantiationException e) {
          model.addAttribute(STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
          LOG.error(e.getMessage(), e);
          return;
        } catch (IllegalAccessException e) {
          model.addAttribute(STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
          LOG.error(e.getMessage(), e);
          return;
        }
        break;
      case PUT:
        entity = repo.findOne(serId);
        break;
    }

    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final MediaType incomingMediaType = request.getHeaders().getContentType();
      try {
        if (request.getMethod() == HttpMethod.POST) {
          final Object incoming = readIncoming(request, incomingMediaType, typeMeta.domainClass);
          if (null == incoming) {
            model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
          } else {
            typeMeta.entityMetadata.id(serId, incoming);
            Object savedEntity = repo.save(entity);
            String savedId = typeMeta.entityInfo.getId(savedEntity).toString();
            URI selfUri = buildUri(baseUri, repository, savedId);
            HttpHeaders headers = new HttpHeaders();
            headers.set(LOCATION, selfUri.toString());
            model.addAttribute(HEADERS, headers);
            model.addAttribute(STATUS, HttpStatus.CREATED);
          }
        } else {
          final Map incoming = readIncoming(request, incomingMediaType, Map.class);
          if (null == incoming) {
            model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
          } else {
            for (Map.Entry<String, Attribute> entry : typeMeta.entityMetadata.embeddedAttributes().entrySet()) {
              String name = entry.getKey();
              if (incoming.containsKey(name)) {
                typeMeta.entityMetadata.set(name, incoming.get(name), entity);
              }
            }
            repo.save(entity);
            model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
          }
        }

      } catch (IOException e) {
        model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);
        LOG.error(e.getMessage(), e);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.DELETE
  )
  public void deleteEntity(@PathVariable String repository,
                           @PathVariable String id,
                           Model model) {
    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);
    Serializable serId = stringToSerializable(id, typeMeta.idType);

    repo.delete(serId);

    model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
  }


  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}",
      method = RequestMethod.GET,
      produces = {
          "application/json",
          "text/uri-list"
      }
  )
  public void propertyOfEntity(UriComponentsBuilder uriBuilder,
                               @PathVariable String repository,
                               @PathVariable String id,
                               @PathVariable String property,
                               Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      Attribute attr = typeMeta.entityType.getAttribute(property);
      if (null != attr) {
        Class<?> childType;
        if (attr instanceof PluralAttribute) {
          childType = ((PluralAttribute) attr).getElementType().getJavaType();
        } else {
          childType = attr.getJavaType();
        }

        CrudRepository childRepo = repositoryMetadata.repositoryFor(childType);
        if (null == childRepo) {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
          return;
        }

        model.addAttribute(STATUS, HttpStatus.OK);

        TypeMetaCacheEntry childTypeMeta = typeMetaEntry(childRepo);

        Object child = typeMeta.entityMetadata.get(property, entity);
        if (null != child) {
          Links links = new Links();
          if (child instanceof Collection) {
            for (Object o : (Collection) child) {
              String childId = childTypeMeta.entityInfo.getId(o).toString();
              URI uri = buildUri(baseUri, repository, id, property, childId);
              links.add(new SimpleLink(childType.getSimpleName(), uri));
            }
          } else if (child instanceof Map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) child).entrySet()) {
              String childId = childTypeMeta.entityInfo.getId(entry.getValue()).toString();
              URI uri = buildUri(baseUri, repository, id, property, childId);
              Object oKey = entry.getKey();
              String sKey;
              if (ClassUtils.isAssignable(oKey.getClass(), String.class)) {
                sKey = (String) oKey;
              } else {
                sKey = conversionService.convert(oKey, String.class);
              }
              links.add(new SimpleLink(sKey, uri));
            }
          } else {
            String childId = childTypeMeta.entityInfo.getId(child).toString();
            URI uri = buildUri(baseUri, repository, id, property, childId);
            links.add(new SimpleLink(property, uri));
          }
          model.addAttribute(RESOURCE, links);
        } else {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
        }
      } else {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}",
      method = {
          RequestMethod.PUT,
          RequestMethod.POST
      },
      consumes = {
          "application/json",
          "text/uri-list"
      },
      produces = {
          "application/json",
          "text/uri-list"
      }
  )
  public void updateLinks(final ServerHttpRequest request,
                          UriComponentsBuilder uriBuilder,
                          @PathVariable String repository,
                          @PathVariable String id,
                          final @PathVariable String property,
                          final Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final Attribute attr = typeMeta.entityMetadata.linkedAttributes().get(property);
      if (null != attr) {
        final AtomicReference<String> rel = new AtomicReference<String>();
        Handler<Object, Void> entityHandler = new Handler<Object, Void>() {
          @Override public Void handle(Object childEntity) {
            if (attr instanceof PluralAttribute) {
              PluralAttribute plAttr = (PluralAttribute) attr;
              switch (plAttr.getCollectionType()) {
                case COLLECTION:
                case LIST: {
                  Collection c = new ArrayList();
                  Collection current = (Collection) typeMeta.entityMetadata.get(property, entity);
                  if (request.getMethod() == HttpMethod.POST && null != current) {
                    c.addAll(current);
                  }
                  c.add(childEntity);
                  typeMeta.entityMetadata.set(property, c, entity);
                }
                break;
                case SET: {
                  Set s = new HashSet();
                  Set current = (Set) typeMeta.entityMetadata.get(property, entity);
                  if (request.getMethod() == HttpMethod.POST && null != current) {
                    s.addAll(current);
                  }
                  s.add(childEntity);
                  typeMeta.entityMetadata.set(property, s, entity);
                }
                break;
                case MAP: {
                  Map m = new HashMap();
                  Map current = (Map) typeMeta.entityMetadata.get(property, entity);
                  if (request.getMethod() == HttpMethod.POST && null != current) {
                    m.putAll(current);
                  }
                  String key = rel.get();
                  if (null == key) {
                    model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
                    return null;
                  } else {
                    m.put(rel.get(), childEntity);
                    typeMeta.entityMetadata.set(property, m, entity);
                  }
                }
                break;
              }
            } else if (attr instanceof SingularAttribute) {
              typeMeta.entityMetadata.set(property, childEntity, entity);
            }

            return null;
          }
        };
        MediaType incomingMediaType = request.getHeaders().getContentType();
        try {
          if (uriListMediaType.equals(incomingMediaType)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getBody()));
            String line;
            while (null != (line = in.readLine())) {
              String sLinkUri = line.trim();
              Object o = resolveTopLevelResource(baseUri, sLinkUri);
              if (null != o) {
                entityHandler.handle(o);
              }
            }
          } else if (jsonMediaType.equals(incomingMediaType)) {
            final Map<String, List<Map<String, String>>> incoming = readIncoming(request, incomingMediaType, Map.class);
            for (Map<String, String> link : incoming.get(LINKS)) {
              String sLinkUri = link.get("href");
              Object o = resolveTopLevelResource(baseUri, sLinkUri);
              rel.set(link.get("rel"));
              if (null != o) {
                entityHandler.handle(o);
              }
            }
          }

          repo.save(entity);

          if (request.getMethod() == HttpMethod.PUT) {
            model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
          } else {
            model.addAttribute(STATUS, HttpStatus.CREATED);
          }
        } catch (IOException e) {
          model.addAttribute(STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
          LOG.error(e.getMessage(), e);
        }
      } else {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}",
      method = {
          RequestMethod.DELETE
      }
  )
  public void clearLinks(@PathVariable String repository,
                         @PathVariable String id,
                         @PathVariable String property,
                         Model model) {
    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final Attribute attr = typeMeta.entityMetadata.linkedAttributes().get(property);
      if (null != attr) {
        typeMeta.entityMetadata.set(property, null, entity);

        repo.save(entity);

        model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
      } else {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{childId}",
      method = {
          RequestMethod.GET
      },
      produces = {
          "application/json"
      }
  )
  public void childEntity(UriComponentsBuilder uriBuilder,
                          @PathVariable String repository,
                          @PathVariable String id,
                          @PathVariable String property,
                          @PathVariable String childId,
                          Model model) {
    URI baseUri = uriBuilder.build().toUri();

    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    final Object entity = repo.findOne(serId);
    if (null != entity) {
      final Attribute attr = typeMeta.entityMetadata.linkedAttributes().get(property);
      if (null != attr) {
        // Find child entity
        CrudRepository childRepo = repositoryFromAttribute(attr);
        if (null != childRepo) {
          TypeMetaCacheEntry childTypeMeta = typeMetaEntry(childRepo);
          Serializable sChildId = stringToSerializable(childId, childTypeMeta.idType);
          Object childEntity = childRepo.findOne(sChildId);
          if (null != childEntity) {
            Map<String, Object> entityDto = extractPropertiesLinkAware(childEntity,
                                                                       childTypeMeta.entityMetadata,
                                                                       baseUri);
            URI selfUri = addSelfLink(baseUri, entityDto, repository, id);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Location", selfUri.toString());
            model.addAttribute(HEADERS, headers);
            model.addAttribute(STATUS, HttpStatus.OK);
            model.addAttribute(RESOURCE, entityDto);
            return;
          }
        }
      }
    }

    model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{childId}",
      method = {
          RequestMethod.DELETE
      }
  )
  public void deleteLink(@PathVariable String repository,
                         @PathVariable String id,
                         @PathVariable String property,
                         @PathVariable String childId,
                         Model model) {
    CrudRepository repo = repositoryMetadata.repositoryFor(repository);
    if (null == repo) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      return;
    }

    final TypeMetaCacheEntry typeMeta = typeMetaEntry(repo);

    Serializable serId = stringToSerializable(id, typeMeta.idType);
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final Attribute attr = typeMeta.entityMetadata.linkedAttributes().get(property);
      if (null != attr) {
        // Find child entity
        CrudRepository childRepo = repositoryFromAttribute(attr);
        if (null != childRepo) {
          TypeMetaCacheEntry childTypeMeta = typeMetaEntry(childRepo);
          Serializable sChildId = stringToSerializable(childId, childTypeMeta.idType);
          Object childEntity = childRepo.findOne(sChildId);
          if (null != childEntity) {
            // Remove child entity from relationship based on property type
            if (attr instanceof PluralAttribute) {
              PluralAttribute plAttr = (PluralAttribute) attr;
              switch (plAttr.getCollectionType()) {
                case COLLECTION:
                case LIST:
                  Collection c = (Collection) typeMeta.entityMetadata.get(property, entity);
                  if (null != c) {
                    c.remove(childEntity);
                  }
                  break;
                case SET:
                  Set s = (Set) typeMeta.entityMetadata.get(property, entity);
                  if (null != s) {
                    s.remove(childEntity);
                  }
                  break;
                case MAP:
                  Object keyToRemove = null;
                  Map<Object, Object> m = (Map) typeMeta.entityMetadata.get(property, entity);
                  if (null != m) {
                    for (Map.Entry<Object, Object> entry : m.entrySet()) {
                      Object val = entry.getValue();
                      if (null != val && val.equals(childEntity)) {
                        keyToRemove = entry.getKey();
                        break;
                      }
                    }
                    if (null != keyToRemove) {
                      m.remove(keyToRemove);
                    }
                  }
                  break;
              }
            } else if (attr instanceof SingularAttribute) {
              typeMeta.entityMetadata.set(property, childEntity, entity);
            }

            model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
          }
        } else {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
        }
      }
    }
  }

  private static URI buildUri(URI baseUri, String... pathSegments) {
    return UriComponentsBuilder.fromUri(baseUri).pathSegment(pathSegments).build().toUri();
  }

  private TypeMetaCacheEntry typeMetaEntry(CrudRepository repo) {
    TypeMetaCacheEntry entry = typeMetaCache.get(repo);
    if (null == entry) {
      entry = new TypeMetaCacheEntry(repo);
      typeMetaCache.put(repo, entry);
    }
    return entry;
  }

  @SuppressWarnings({"unchecked"})
  private CrudRepository repositoryFromAttribute(Attribute attr) {
    CrudRepository repo = null;
    if (attr instanceof PluralAttribute) {
      repo = repositoryMetadata.repositoryFor(((PluralAttribute) attr).getElementType().getJavaType());
    } else {
      repo = repositoryMetadata.repositoryFor(attr.getJavaType());
    }
    return repo;
  }

  @SuppressWarnings({"unchecked"})
  private URI addSelfLink(URI baseUri, Map<String, Object> model, String... pathComponents) {
    List<Link> links = (List<Link>) model.get(LINKS);
    if (null == links) {
      links = new ArrayList<Link>();
      model.put(LINKS, links);
    }
    URI selfUri = buildUri(baseUri, pathComponents);
    links.add(new SimpleLink(SELF, selfUri));
    return selfUri;
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
  private Object resolveTopLevelResource(URI baseUri, String uri) {
    URI href = URI.create(uri);

    URI relativeUri = baseUri.relativize(href);
    Stack<URI> uris = UriUtils.explode(baseUri, relativeUri);

    if (uris.size() > 1) {
      String repoName = UriUtils.path(uris.get(0));
      String sId = UriUtils.path(uris.get(1));

      CrudRepository repo = repositoryMetadata.repositoryFor(repoName);
      if (null == repo) {
        return null;
      }
      EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repo);
      if (null == entityInfo) {
        return null;
      }
      Class<? extends Serializable> idType = entityInfo.getIdType();

      Serializable serId = stringToSerializable(sId, idType);

      return repo.findOne(serId);
    }

    return null;
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
  private Map<String, Object> extractPropertiesLinkAware(final Object entity,
                                                         final JpaEntityMetadata entityMetadata,
                                                         final URI baseUri) {
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

  private class TypeMetaCacheEntry {
    EntityInformation entityInfo;
    Class<?> domainClass;
    Class<? extends Serializable> idType;
    EntityType entityType;
    JpaEntityMetadata entityMetadata;

    @SuppressWarnings({"unchecked"})
    private TypeMetaCacheEntry(CrudRepository repo) {
      entityInfo = repositoryMetadata.entityInfoFor(repo);
      domainClass = entityInfo.getJavaType();
      idType = entityInfo.getIdType();
      entityType = repositoryMetadata.entityTypeFor(domainClass);
      entityMetadata = repositoryMetadata.entityMetadataFor(domainClass);
    }
  }

}
