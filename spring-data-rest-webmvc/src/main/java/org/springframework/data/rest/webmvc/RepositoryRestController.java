package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.Handler;
import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.data.rest.core.util.UriUtils;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.EntityMetadata;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryExporterSupport;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.context.AfterDeleteEvent;
import org.springframework.data.rest.repository.context.AfterLinkSaveEvent;
import org.springframework.data.rest.repository.context.AfterSaveEvent;
import org.springframework.data.rest.repository.context.BeforeDeleteEvent;
import org.springframework.data.rest.repository.context.BeforeLinkSaveEvent;
import org.springframework.data.rest.repository.context.BeforeSaveEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@Controller
public class RepositoryRestController
    extends RepositoryExporterSupport<RepositoryRestController>
    implements ApplicationEventPublisherAware,
               InitializingBean {

  public static final String STATUS = "status";
  public static final String HEADERS = "headers";
  public static final String LOCATION = "Location";
  public static final String RESOURCE = "resource";
  public static final String SELF = "self";
  public static final String LINKS = "_links";

  private ApplicationEventPublisher eventPublisher;

  private MediaType uriListMediaType = MediaType.parseMediaType("text/uri-list");
  private MediaType jsonMediaType = MediaType.parseMediaType("application/x-spring-data+json");
  private ConversionService conversionService = new DefaultConversionService();
  private List<HttpMessageConverter<?>> httpMessageConverters = Collections.emptyList();
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
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
    for (RepositoryExporter repoMeta : repositoryExporters) {
      for (String name : (Set<String>) repoMeta.repositoryNames()) {
        links.add(new SimpleLink(name, buildUri(baseUri, name)));
      }
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

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Links links = new Links();

    Iterator iter = ((CrudRepository) repoMeta.repository()).findAll().iterator();
    while (iter.hasNext()) {
      Object o = iter.next();
      Serializable id = (Serializable) repoMeta.entityMetadata().idAttribute().get(o);
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
                     Model model) throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    MediaType incomingMediaType = request.getHeaders().getContentType();
    final Object incoming = readIncoming(request, incomingMediaType, repoMeta.entityMetadata().type());
    if (null == incoming) {
      model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
    } else {
      if (null != eventPublisher) {
        eventPublisher.publishEvent(new BeforeSaveEvent(incoming));
      }
      Object savedEntity = repo.save(incoming);
      if (null != eventPublisher) {
        eventPublisher.publishEvent(new AfterSaveEvent(savedEntity));
      }
      String sId = repoMeta.entityMetadata().idAttribute().get(savedEntity).toString();

      URI selfUri = buildUri(baseUri, repository, sId);

      HttpHeaders headers = new HttpHeaders();
      headers.set(LOCATION, selfUri.toString());

      model.addAttribute(HEADERS, headers);
      model.addAttribute(STATUS, HttpStatus.CREATED);
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

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      HttpHeaders headers = new HttpHeaders();
      Object version = repoMeta.entityMetadata().versionAttribute().get(entity);
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
                                                                 repoMeta.entityMetadata(),
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
                             Model model)
      throws IOException,
             IllegalAccessException,
             InstantiationException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    Object entity = null;
    Class<?> domainType = repoMeta.entityMetadata().type();
    switch (request.getMethod()) {
      case POST:
        entity = domainType.newInstance();
        break;
      case PUT:
        entity = repo.findOne(serId);
        break;
    }

    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final MediaType incomingMediaType = request.getHeaders().getContentType();
      final Object incoming = readIncoming(request, incomingMediaType, domainType);
      if (null == incoming) {
        throw new HttpMessageNotReadableException("Could not create an instance of " + domainType.getSimpleName() + " from input.");
      } else {
        repoMeta.entityMetadata().idAttribute().set(serId, incoming);
        if (request.getMethod() == HttpMethod.POST) {
          if (null != eventPublisher) {
            eventPublisher.publishEvent(new BeforeSaveEvent(incoming));
          }
          Object savedEntity = repo.save(incoming);
          if (null != eventPublisher) {
            eventPublisher.publishEvent(new AfterSaveEvent(savedEntity));
          }
          URI selfUri = buildUri(baseUri, repository, id);
          HttpHeaders headers = new HttpHeaders();
          headers.set(LOCATION, selfUri.toString());
          model.addAttribute(HEADERS, headers);
          model.addAttribute(STATUS, HttpStatus.CREATED);
        } else {
          if (null != eventPublisher) {
            eventPublisher.publishEvent(new BeforeSaveEvent(incoming));
          }
          Object savedEntity = repo.save(incoming);
          if (null != eventPublisher) {
            eventPublisher.publishEvent(new AfterSaveEvent(savedEntity));
          }
          model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
        }
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
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();

    if (null != eventPublisher) {
      eventPublisher.publishEvent(new BeforeDeleteEvent(serId));
    }
    repo.delete(serId);
    if (null != eventPublisher) {
      eventPublisher.publishEvent(new AfterDeleteEvent(serId));
    }

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

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null == attrMeta) {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      } else {
        Class<?> attrType = attrMeta.elementType();
        if (null == attrType) {
          attrType = attrMeta.type();
        }

        RepositoryMetadata propRepoMeta = repositoryMetadataFor(attrType);
        model.addAttribute(STATUS, HttpStatus.OK);
        Object propVal = attrMeta.get(entity);
        AttributeMetadata idAttr = propRepoMeta.entityMetadata().idAttribute();
        if (null != propVal) {
          Links links = new Links();
          if (propVal instanceof Collection) {
            for (Object o : (Collection) propVal) {
              String propValId = idAttr.get(o).toString();
              URI uri = buildUri(baseUri, repository, id, property, propValId);
              links.add(new SimpleLink(attrType.getSimpleName(), uri));
            }
          } else if (propVal instanceof Map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) propVal).entrySet()) {
              String propValId = idAttr.get(entry.getValue()).toString();
              URI uri = buildUri(baseUri, repository, id, property, propValId);
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
            String propValId = idAttr.get(propVal).toString();
            URI uri = buildUri(baseUri, repository, id, property, propValId);
            links.add(new SimpleLink(property, uri));
          }
          model.addAttribute(RESOURCE, links);
        } else {
          model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
        }
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
                          final Model model) throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    final RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null == attrMeta) {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      } else {
        Object linked = attrMeta.get(entity);
        final AtomicReference<String> rel = new AtomicReference<String>();
        Handler<Object, Void> entityHandler = new Handler<Object, Void>() {
          @Override public Void handle(Object linkedEntity) {
            if (attrMeta.isCollectionLike()) {
              Collection c = new ArrayList();
              Collection current = (Collection) attrMeta.get(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                c.addAll(current);
              }
              c.add(linkedEntity);
              attrMeta.set(c, entity);
            } else if (attrMeta.isSetLike()) {
              Set s = new HashSet();
              Set current = (Set) attrMeta.get(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                s.addAll(current);
              }
              s.add(linkedEntity);
              attrMeta.set(s, entity);
            } else if (attrMeta.isMapLike()) {
              Map m = new HashMap();
              Map current = (Map) attrMeta.get(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                m.putAll(current);
              }
              String key = rel.get();
              if (null == key) {
                model.addAttribute(STATUS, HttpStatus.NOT_ACCEPTABLE);
                return null;
              } else {
                m.put(rel.get(), linkedEntity);
                attrMeta.set(m, entity);
              }
            } else {
              attrMeta.set(linkedEntity, entity);
            }
            return null;
          }
        };
        MediaType incomingMediaType = request.getHeaders().getContentType();
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

        if (null != eventPublisher) {
          eventPublisher.publishEvent(new BeforeSaveEvent(entity));
          eventPublisher.publishEvent(new BeforeLinkSaveEvent(entity, linked));
        }
        Object savedEntity = repo.save(entity);
        if (null != eventPublisher) {
          linked = attrMeta.get(savedEntity);
          eventPublisher.publishEvent(new AfterLinkSaveEvent(savedEntity, linked));
          eventPublisher.publishEvent(new AfterSaveEvent(savedEntity));
        }

        if (request.getMethod() == HttpMethod.PUT) {
          model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
        } else {
          model.addAttribute(STATUS, HttpStatus.CREATED);
        }
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
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        Object linked = attrMeta.get(entity);
        attrMeta.set(null, entity);

        if (null != eventPublisher) {
          eventPublisher.publishEvent(new BeforeLinkSaveEvent(entity, linked));
        }
        Object savedEntity = repo.save(entity);
        if (null != eventPublisher) {
          eventPublisher.publishEvent(new AfterLinkSaveEvent(savedEntity, null));
        }

        model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
      } else {
        model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{linkedId}",
      method = {
          RequestMethod.GET
      },
      produces = {
          "application/json"
      }
  )
  public void linkedEntity(UriComponentsBuilder uriBuilder,
                           @PathVariable String repository,
                           @PathVariable String id,
                           @PathVariable String property,
                           @PathVariable String linkedId,
                           Model model) {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null != entity) {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor(attrMeta);
        if (null != linkedRepoMeta) {
          CrudRepository linkedRepo = (CrudRepository) linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable(linkedId,
                                                       (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                           .idAttribute()
                                                           .type());
          Object linkedEntity = linkedRepo.findOne(sChildId);
          if (null != linkedEntity) {
            Map<String, Object> entityDto = extractPropertiesLinkAware(linkedEntity,
                                                                       linkedRepoMeta.entityMetadata(),
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
      value = "/{repository}/{id}/{property}/{linkedId}",
      method = {
          RequestMethod.DELETE
      }
  )
  public void deleteLink(@PathVariable String repository,
                         @PathVariable String id,
                         @PathVariable String property,
                         @PathVariable String linkedId,
                         Model model) {
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = (CrudRepository) repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor(attrMeta);
        if (null != linkedRepoMeta) {
          CrudRepository linkedRepo = (CrudRepository) linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable(linkedId,
                                                       (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                           .idAttribute()
                                                           .type());
          Object linkedEntity = linkedRepo.findOne(sChildId);
          if (null != linkedEntity) {
            // Remove linked entity from relationship based on property type
            if (attrMeta.isCollectionLike()) {
              Collection c = (Collection) attrMeta.get(entity);
              if (null != c) {
                c.remove(linkedEntity);
              }
            } else if (attrMeta.isSetLike()) {
              Set s = (Set) attrMeta.get(entity);
              if (null != s) {
                s.remove(linkedEntity);
              }
            } else if (attrMeta.isMapLike()) {
              Object keyToRemove = null;
              Map<Object, Object> m = (Map) attrMeta.get(entity);
              if (null != m) {
                for (Map.Entry<Object, Object> entry : m.entrySet()) {
                  Object val = entry.getValue();
                  if (null != val && val.equals(linkedEntity)) {
                    keyToRemove = entry.getKey();
                    break;
                  }
                }
                if (null != keyToRemove) {
                  m.remove(keyToRemove);
                }
              }
            } else {
              attrMeta.set(linkedEntity, entity);
            }

            model.addAttribute(STATUS, HttpStatus.NO_CONTENT);
            return;
          }
        }
      }
    }

    model.addAttribute(STATUS, HttpStatus.NOT_FOUND);
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(OptimisticLockingFailureException.class)
  @ResponseBody
  public ResponseEntity handleLockingFailure(OptimisticLockingFailureException ex) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map m = new HashMap();
    m.put("message", ex.getMessage());
    return new ResponseEntity(objectMapper.writeValueAsBytes(m), headers, HttpStatus.CONFLICT);
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(RepositoryConstraintViolationException.class)
  public Model handleValidationFailure(RepositoryConstraintViolationException ex) throws IOException {
    Model model = new ExtendedModelMap();
    model.addAttribute(STATUS, HttpStatus.BAD_REQUEST);

    Map m = new HashMap();
    List<String> errors = new ArrayList<String>();
    for (FieldError fe : ex.getErrors().getFieldErrors()) {
      errors.add(fe.getDefaultMessage());
    }
    m.put("errors", errors);

    model.addAttribute(RESOURCE, m);
    return model;
  }

  private static URI buildUri(URI baseUri, String... pathSegments) {
    return UriComponentsBuilder.fromUri(baseUri).pathSegment(pathSegments).build().toUri();
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

      RepositoryMetadata repoMeta = repositoryMetadataFor(repoName);
      CrudRepository repo = (CrudRepository) repoMeta.repository();
      if (null == repo) {
        return null;
      }
      EntityMetadata entityMeta = repoMeta.entityMetadata();
      if (null == entityMeta) {
        return null;
      }
      Class<? extends Serializable> idType = (Class<? extends Serializable>) entityMeta.idAttribute().type();

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
  private Map<String, Object> extractPropertiesLinkAware(Object entity,
                                                         EntityMetadata<AttributeMetadata> entityMetadata,
                                                         URI baseUri) {
    final Map<String, Object> entityDto = new HashMap<String, Object>();

    for (Map.Entry<String, AttributeMetadata> attrMeta : entityMetadata.embeddedAttributes().entrySet()) {
      String name = attrMeta.getKey();
      Object val = attrMeta.getValue().get(entity);
      if (null != val) {
        entityDto.put(name, val);
      }
    }

    for (String attrName : entityMetadata.linkedAttributes().keySet()) {
      URI uri = UriComponentsBuilder.fromUri(baseUri)
          .pathSegment(attrName)
          .build()
          .toUri();
      Link l = new SimpleLink(attrName, uri);
      List<Link> links = (List<Link>) entityDto.get(LINKS);
      if (null == links) {
        links = new ArrayList<Link>();
        entityDto.put(LINKS, links);
      }
      links.add(l);
    }

    return entityDto;
  }

}
