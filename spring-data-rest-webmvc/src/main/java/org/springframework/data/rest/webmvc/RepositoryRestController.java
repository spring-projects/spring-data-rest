package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.core.Handler;
import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.data.rest.core.convert.DelegatingConversionService;
import org.springframework.data.rest.core.util.UriUtils;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.EntityMetadata;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryExporterSupport;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.RepositoryQueryMethod;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.data.rest.repository.context.AfterDeleteEvent;
import org.springframework.data.rest.repository.context.AfterLinkSaveEvent;
import org.springframework.data.rest.repository.context.AfterSaveEvent;
import org.springframework.data.rest.repository.context.BeforeDeleteEvent;
import org.springframework.data.rest.repository.context.BeforeLinkSaveEvent;
import org.springframework.data.rest.repository.context.BeforeSaveEvent;
import org.springframework.format.support.DefaultFormattingConversionService;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Controller
public class RepositoryRestController
    extends RepositoryExporterSupport<RepositoryRestController>
    implements ApplicationContextAware,
               InitializingBean {

  public static final String STATUS = "status";
  public static final String HEADERS = "headers";
  public static final String LOCATION = "Location";
  public static final String RESOURCE = "resource";
  public static final String SELF = "self";
  public static final String LINKS = "_links";

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryRestController.class);

  private ApplicationContext applicationContext;

  private MediaType uriListMediaType = MediaType.parseMediaType("text/uri-list");
  private MediaType jsonMediaType = MediaType.parseMediaType("application/x-spring-data+json");
  private DelegatingConversionService conversionService = new DelegatingConversionService(
      new DefaultFormattingConversionService()
  );
  private List<HttpMessageConverter<?>> httpMessageConverters = Collections.emptyList();
  private Map<String, Handler<Object, Object>> resourceHandlers = Collections.emptyMap();
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setConversionService(ConversionService conversionService) {
    if (null != conversionService) {
      this.conversionService.addConversionServices(conversionService);
    }
  }

  public ConversionService conversionService() {
    return conversionService;
  }

  public RepositoryRestController conversionService(ConversionService conversionService) {
    setConversionService(conversionService);
    return this;
  }

  public List<HttpMessageConverter<?>> getHttpMessageConverters() {
    return httpMessageConverters;
  }

  public void setHttpMessageConverters(List<HttpMessageConverter<?>> httpMessageConverters) {
    Assert.notNull(httpMessageConverters);
    this.httpMessageConverters = httpMessageConverters;
  }

  public List<HttpMessageConverter<?>> httpMessageConverters() {
    return httpMessageConverters;
  }

  public RepositoryRestController httpMessageConverters(List<HttpMessageConverter<?>> httpMessageConverters) {
    setHttpMessageConverters(httpMessageConverters);
    return this;
  }

  public Map<String, Handler<Object, Object>> getResourceHandlers() {
    return resourceHandlers;
  }

  public RepositoryRestController setResourceHandlers(Map<String, Handler<Object, Object>> resourceHandlers) {
    this.resourceHandlers = resourceHandlers;
    return this;
  }

  public Map<String, Handler<Object, Object>> resourceHandlers() {
    return resourceHandlers;
  }

  public RepositoryRestController resourceHandlers(Map<String, Handler<Object, Object>> resourceHandlers) {
    setResourceHandlers(resourceHandlers);
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

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
    for (ConversionService convsvc : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                                                                                    ConversionService.class).values()) {
      conversionService.addConversionServices(convsvc);
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public ModelAndView listRepositories(UriComponentsBuilder uriBuilder) {
    URI baseUri = uriBuilder.build().toUri();

    Links links = new Links();
    for (RepositoryExporter repoExporter : repositoryExporters) {
      for (String name : (Set<String>) repoExporter.repositoryNames()) {
        RepositoryMetadata repoMeta = repoExporter.repositoryMetadataFor(name);
        String rel = repoMeta.rel();
        URI path = buildUri(baseUri, name);
        links.add(new SimpleLink(rel, path));
      }
    }

    Map model = new HashMap();
    model.put(STATUS, HttpStatus.OK);
    model.put(RESOURCE, links);
    return new ModelAndView(viewName("list_links"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public ModelAndView listEntities(UriComponentsBuilder uriBuilder,
                                   @PathVariable String repository) {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Links links = new Links();

    Iterator iter = repoMeta.repository().findAll().iterator();
    while (iter.hasNext()) {
      Object o = iter.next();
      Serializable id = (Serializable) repoMeta.entityMetadata().idAttribute().get(o);
      links.add(new SimpleLink(repoMeta.rel() + "." + o.getClass().getSimpleName() + "." + id.toString(),
                               buildUri(baseUri, repository, id.toString())));
    }
    links.add(new SimpleLink(repoMeta.rel() + ".search",
                             buildUri(baseUri, repository, "search")));

    Map model = new HashMap();
    model.put(STATUS, HttpStatus.OK);
    model.put(RESOURCE, links);
    return new ModelAndView(viewName("list_entities"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/search",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public ModelAndView listQueryMethods(UriComponentsBuilder uriBuilder,
                                       @PathVariable String repository) {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Links links = new Links();

    for (Map.Entry<String, RepositoryQueryMethod> entry : ((Map<String, RepositoryQueryMethod>) repoMeta.queryMethods())
        .entrySet()) {
      String rel = repoMeta.rel() + "." + entry.getKey();
      URI path = buildUri(baseUri, repository, "search", entry.getKey());
      RestResource resourceAnno = entry.getValue().method().getAnnotation(RestResource.class);
      if (null != resourceAnno) {
        if (StringUtils.hasText(resourceAnno.path())) {
          path = buildUri(baseUri, repository, "search", resourceAnno.path());
        }
        if (StringUtils.hasText(resourceAnno.rel())) {
          rel = repoMeta.rel() + "." + resourceAnno.rel();
        }
      }
      links.add(new SimpleLink(rel, path));
    }

    Map model = new HashMap();
    model.put(STATUS, HttpStatus.OK);
    model.put(RESOURCE, links);
    return new ModelAndView(viewName("list_queries"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/search/{query}",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public ModelAndView query(WebRequest request,
                            UriComponentsBuilder uriBuilder,
                            @PathVariable String repository,
                            @PathVariable String query) {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Repository repo = repoMeta.repository();
    RepositoryQueryMethod queryMethod = repoMeta.queryMethod(query);

    Map model = new HashMap();
    Class<?>[] paramTypes = queryMethod.paramTypes();
    String[] paramNames = queryMethod.paramNames();
    Object[] paramVals = new Object[paramTypes.length];
    for (int i = 0; i < paramVals.length; i++) {
      String queryVal = request.getParameter(paramNames[i]);
      if (String.class.isAssignableFrom(paramTypes[i])) {
        // Param type is a String
        paramVals[i] = queryVal;
      } else if (Pageable.class.isAssignableFrom(paramTypes[i])) {
        // Handle paging
      } else if (Sort.class.isAssignableFrom(paramTypes[i])) {
        // Handle sorting
      } else if (conversionService.canConvert(String.class, paramTypes[i])) {
        // There's a converter from String -> param type
        paramVals[i] = conversionService.convert(queryVal, paramTypes[i]);
      } else {
        // Param type isn't a "simple" type or no converter exists, try JSON
        try {
          paramVals[i] = objectMapper.readValue(queryVal, paramTypes[i]);
        } catch (IOException e) {
          throw new IllegalArgumentException(e);
        }
      }
    }

    try {
      Object result = queryMethod.method().invoke(repo, paramVals);
      if (result instanceof Collection) {
        Collection coll = new ArrayList();
        for (Object o : (Collection) result) {
          RepositoryMetadata elemRepoMeta = repositoryMetadataFor(o.getClass());
          if (null != elemRepoMeta) {
            String id = elemRepoMeta.entityMetadata().idAttribute().get(o).toString();
            String rel = elemRepoMeta.rel() + "." + elemRepoMeta.entityMetadata().type().getSimpleName();
            URI path = buildUri(baseUri, repository, id);
            coll.add(new SimpleLink(rel, path));
          } else {
            coll.add(o);
          }
        }

        model.put(RESOURCE, coll);
        model.put(STATUS, HttpStatus.OK);
      } else {
        RepositoryMetadata elemRepoMeta = repositoryMetadataFor(result.getClass());
        if (null != elemRepoMeta) {
          String id = elemRepoMeta.entityMetadata().idAttribute().get(result).toString();
          String rel = elemRepoMeta.rel() + "." + elemRepoMeta.entityMetadata().type().getSimpleName();
          URI path = buildUri(baseUri, repository, id);
          Link link = new SimpleLink(rel, path);
          model.put(RESOURCE, link);
        } else {
          model.put(RESOURCE, result);
        }
        model.put(STATUS, HttpStatus.OK);
      }
    } catch (IllegalAccessException e) {
      throw new DataRetrievalFailureException(e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new DataRetrievalFailureException(e.getMessage(), e);
    }

    return new ModelAndView(viewName("query_results"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.POST,
      produces = {
          "application/json"
      }
  )
  public ModelAndView create(ServerHttpRequest request,
                             HttpServletRequest servletRequest,
                             UriComponentsBuilder uriBuilder,
                             @PathVariable String repository) throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    CrudRepository repo = repoMeta.repository();
    MediaType incomingMediaType = request.getHeaders().getContentType();
    final Object incoming = readIncoming(request, incomingMediaType, repoMeta.entityMetadata().type());
    if (null == incoming) {
      model.put(STATUS, HttpStatus.NOT_ACCEPTABLE);
    } else {
      if (null != applicationContext) {
        applicationContext.publishEvent(new BeforeSaveEvent(incoming));
      }
      Object savedEntity = repo.save(incoming);
      if (null != applicationContext) {
        applicationContext.publishEvent(new AfterSaveEvent(savedEntity));
      }
      String sId = repoMeta.entityMetadata().idAttribute().get(savedEntity).toString();

      URI selfUri = buildUri(baseUri, repository, sId);

      HttpHeaders headers = new HttpHeaders();
      headers.set(LOCATION, selfUri.toString());

      model.put(HEADERS, headers);
      model.put(STATUS, HttpStatus.CREATED);
      if (null != servletRequest.getParameter("returnBody") && "true".equals(servletRequest.getParameter("returnBody"))) {
        Map<String, Object> entityDto = extractPropertiesLinkAware(repoMeta.rel(),
                                                                   savedEntity,
                                                                   repoMeta.entityMetadata(),
                                                                   buildUri(baseUri, repository, sId));
        addSelfLink(baseUri, entityDto, repository, sId);
        model.put(RESOURCE, entityDto);
      }
    }
    return new ModelAndView(viewName("after_create"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.GET,
      produces = {
          "application/json"
      }
  )
  public ModelAndView entity(ServerHttpRequest request,
                             UriComponentsBuilder uriBuilder,
                             @PathVariable String repository,
                             @PathVariable String id) {
    URI baseUri = uriBuilder.build().toUri();

    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      HttpHeaders headers = new HttpHeaders();
      if (null != repoMeta.entityMetadata().versionAttribute()) {
        Object version = repoMeta.entityMetadata().versionAttribute().get(entity);
        if (null != version) {
          List<String> etags = request.getHeaders().getIfNoneMatch();
          for (String etag : etags) {
            if (("\"" + version.toString() + "\"").equals(etag)) {
              model.put(STATUS, HttpStatus.NOT_MODIFIED);
              return new ModelAndView(viewName("empty"), model);
            }
          }
          headers.set("ETag", "\"" + version.toString() + "\"");
        }
      }
      Map<String, Object> entityDto = extractPropertiesLinkAware(repoMeta.rel(),
                                                                 entity,
                                                                 repoMeta.entityMetadata(),
                                                                 buildUri(baseUri, repository, id));
      addSelfLink(baseUri, entityDto, repository, id);

      model.put(HEADERS, headers);
      model.put(STATUS, HttpStatus.OK);
      model.put(RESOURCE, entityDto);
    }

    return new ModelAndView(viewName("entity"), model);
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
  public ModelAndView createOrUpdate(ServerHttpRequest request,
                                     UriComponentsBuilder uriBuilder,
                                     @PathVariable String repository,
                                     @PathVariable String id)
      throws IOException,
             IllegalAccessException,
             InstantiationException {
    URI baseUri = uriBuilder.build().toUri();

    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    Class<?> domainType = repoMeta.entityMetadata().type();

    final MediaType incomingMediaType = request.getHeaders().getContentType();
    final Object incoming = readIncoming(request, incomingMediaType, domainType);
    if (null == incoming) {
      throw new HttpMessageNotReadableException("Could not create an instance of " + domainType.getSimpleName() + " from input.");
    } else {
      repoMeta.entityMetadata().idAttribute().set(serId, incoming);
      if (request.getMethod() == HttpMethod.POST) {
        if (null != applicationContext) {
          applicationContext.publishEvent(new BeforeSaveEvent(incoming));
        }
        Object savedEntity = repo.save(incoming);
        if (null != applicationContext) {
          applicationContext.publishEvent(new AfterSaveEvent(savedEntity));
        }
        URI selfUri = buildUri(baseUri, repository, id);
        HttpHeaders headers = new HttpHeaders();
        headers.set(LOCATION, selfUri.toString());
        model.put(HEADERS, headers);
        model.put(STATUS, HttpStatus.CREATED);
      } else {
        Object entity = repo.findOne(serId);
        if (null == entity) {
          model.put(STATUS, HttpStatus.NOT_FOUND);
        } else {
          for (AttributeMetadata attrMeta : (Collection<AttributeMetadata>) repoMeta.entityMetadata()
              .embeddedAttributes()
              .values()) {
            Object incomingVal = attrMeta.get(incoming);
            if (null != incomingVal) {
              attrMeta.set(incomingVal, entity);
            }
          }
          if (null != applicationContext) {
            applicationContext.publishEvent(new BeforeSaveEvent(entity));
          }
          Object savedEntity = repo.save(entity);
          if (null != applicationContext) {
            applicationContext.publishEvent(new AfterSaveEvent(savedEntity));
          }
          model.put(STATUS, HttpStatus.NO_CONTENT);
        }
      }
    }

    return new ModelAndView(viewName("empty"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.DELETE
  )
  public ModelAndView deleteEntity(@PathVariable String repository,
                                   @PathVariable String id) {
    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      if (null != applicationContext) {
        applicationContext.publishEvent(new BeforeDeleteEvent(entity));
      }
      repo.delete(serId);
      if (null != applicationContext) {
        applicationContext.publishEvent(new AfterDeleteEvent(entity));
      }
      model.put(STATUS, HttpStatus.NO_CONTENT);
    }

    return new ModelAndView(viewName("empty"), model);
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
  public ModelAndView propertyOfEntity(UriComponentsBuilder uriBuilder,
                                       @PathVariable String repository,
                                       @PathVariable String id,
                                       @PathVariable String property) {
    URI baseUri = uriBuilder.build().toUri();

    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null == attrMeta) {
        model.put(STATUS, HttpStatus.NOT_FOUND);
      } else {
        Class<?> attrType = attrMeta.elementType();
        if (null == attrType) {
          attrType = attrMeta.type();
        }

        RepositoryMetadata propRepoMeta = repositoryMetadataFor(attrType);
        model.put(STATUS, HttpStatus.OK);
        Object propVal = attrMeta.get(entity);
        AttributeMetadata idAttr = propRepoMeta.entityMetadata().idAttribute();
        if (null != propVal) {
          Links links = new Links();
          if (propVal instanceof Collection) {
            for (Object o : (Collection) propVal) {
              String propValId = idAttr.get(o).toString();
              String rel = repository + "."
                  + entity.getClass().getSimpleName() + "."
                  + attrType.getSimpleName() + "."
                  + propValId;
              URI path = buildUri(baseUri, repository, id, property, propValId);
              links.add(new SimpleLink(rel, path));
            }
          } else if (propVal instanceof Map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) propVal).entrySet()) {
              String propValId = idAttr.get(entry.getValue()).toString();
              URI path = buildUri(baseUri, repository, id, property, propValId);
              Object oKey = entry.getKey();
              String sKey;
              if (ClassUtils.isAssignable(oKey.getClass(), String.class)) {
                sKey = (String) oKey;
              } else {
                sKey = conversionService.convert(oKey, String.class);
              }
              String rel = repository + "." + entity.getClass().getSimpleName() + "." + sKey;
              links.add(new SimpleLink(rel, path));
            }
          } else {
            String propValId = idAttr.get(propVal).toString();
            String rel = repository + "." + entity.getClass().getSimpleName() + "." + property;
            URI path = buildUri(baseUri, repository, id, property, propValId);
            links.add(new SimpleLink(rel, path));
          }
          model.put(RESOURCE, links);
        } else {
          model.put(STATUS, HttpStatus.NOT_FOUND);
        }
      }
    }

    return new ModelAndView(viewName("entity_property"), model);
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
  public ModelAndView updatePropertyOfEntity(final ServerHttpRequest request,
                                             UriComponentsBuilder uriBuilder,
                                             @PathVariable String repository,
                                             @PathVariable String id,
                                             final @PathVariable String property) throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    final Map model = new HashMap();
    final RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      final AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null == attrMeta) {
        model.put(STATUS, HttpStatus.NOT_FOUND);
      } else {
        Object linked = attrMeta.get(entity);
        final AtomicReference<String> rel = new AtomicReference<String>();
        Handler<Object, Void> entityHandler = new Handler<Object, Void>() {
          @Override public Void handle(Object linkedEntity) {
            if (attrMeta.isCollectionLike()) {
              Collection c = new ArrayList();
              Collection current = attrMeta.asCollection(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                c.addAll(current);
              }
              c.add(linkedEntity);
              attrMeta.set(c, entity);
            } else if (attrMeta.isSetLike()) {
              Set s = new HashSet();
              Set current = attrMeta.asSet(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                s.addAll(current);
              }
              s.add(linkedEntity);
              attrMeta.set(s, entity);
            } else if (attrMeta.isMapLike()) {
              Map m = new HashMap();
              Map current = attrMeta.asMap(entity);
              if (request.getMethod() == HttpMethod.POST && null != current) {
                m.putAll(current);
              }
              String key = rel.get();
              if (null == key) {
                model.put(STATUS, HttpStatus.NOT_ACCEPTABLE);
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
          final Map<String, List<Map<String, String>>> incoming = readIncoming(request,
                                                                               incomingMediaType,
                                                                               Map.class);
          for (Map<String, String> link : incoming.get(LINKS)) {
            String sLinkUri = link.get("href");
            Object o = resolveTopLevelResource(baseUri, sLinkUri);
            rel.set(link.get("rel"));
            if (null != o) {
              entityHandler.handle(o);
            }
          }
        }

        if (null != applicationContext) {
          applicationContext.publishEvent(new BeforeSaveEvent(entity));
          applicationContext.publishEvent(new BeforeLinkSaveEvent(entity, linked));
        }
        Object savedEntity = repo.save(entity);
        if (null != applicationContext) {
          linked = attrMeta.get(savedEntity);
          applicationContext.publishEvent(new AfterLinkSaveEvent(savedEntity, linked));
          applicationContext.publishEvent(new AfterSaveEvent(savedEntity));
        }

        if (request.getMethod() == HttpMethod.PUT) {
          model.put(STATUS, HttpStatus.NO_CONTENT);
        } else {
          model.put(STATUS, HttpStatus.CREATED);
        }
      }
    }

    return new ModelAndView(viewName("empty"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}",
      method = {
          RequestMethod.DELETE
      }
  )
  public ModelAndView clearLinks(@PathVariable String repository,
                                 @PathVariable String id,
                                 @PathVariable String property) {
    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        Object linked = attrMeta.get(entity);
        attrMeta.set(null, entity);

        if (null != applicationContext) {
          applicationContext.publishEvent(new BeforeLinkSaveEvent(entity, linked));
        }
        Object savedEntity = repo.save(entity);
        if (null != applicationContext) {
          applicationContext.publishEvent(new AfterLinkSaveEvent(savedEntity, null));
        }

        model.put(STATUS, HttpStatus.NO_CONTENT);
      } else {
        model.put(STATUS, HttpStatus.NOT_FOUND);
      }
    }

    return new ModelAndView(viewName("empty"), model);
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
  public ModelAndView linkedEntity(UriComponentsBuilder uriBuilder,
                                   @PathVariable String repository,
                                   @PathVariable String id,
                                   @PathVariable String property,
                                   @PathVariable String linkedId) {
    URI baseUri = uriBuilder.build().toUri();

    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne(serId);
    if (null != entity) {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor(attrMeta);
        if (null != linkedRepoMeta) {
          CrudRepository linkedRepo = linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable(linkedId,
                                                       (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                           .idAttribute()
                                                           .type());
          Object linkedEntity = linkedRepo.findOne(sChildId);
          if (null != linkedEntity) {
            Map<String, Object> entityDto = extractPropertiesLinkAware(linkedRepoMeta.rel(),
                                                                       linkedEntity,
                                                                       linkedRepoMeta.entityMetadata(),
                                                                       buildUri(baseUri,
                                                                                linkedRepoMeta.name(),
                                                                                linkedId));
            URI selfUri = addSelfLink(baseUri, entityDto, linkedRepoMeta.name(), linkedId);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Location", selfUri.toString());
            model.put(HEADERS, headers);
            model.put(STATUS, HttpStatus.OK);
            model.put(RESOURCE, entityDto);
            return new ModelAndView(viewName("linked_entity"), model);
          }
        }
      }
    }

    model.put(STATUS, HttpStatus.NOT_FOUND);
    return new ModelAndView(viewName("empty"), model);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{linkedId}",
      method = {
          RequestMethod.DELETE
      }
  )
  public ModelAndView deleteLink(@PathVariable String repository,
                                 @PathVariable String id,
                                 @PathVariable String property,
                                 @PathVariable String linkedId) {
    Map model = new HashMap();
    RepositoryMetadata repoMeta = repositoryMetadataFor(repository);
    Serializable serId = stringToSerializable(id,
                                              (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                  .idAttribute()
                                                  .type());
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne(serId);
    if (null == entity) {
      model.put(STATUS, HttpStatus.NOT_FOUND);
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(property);
      if (null != attrMeta) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor(attrMeta);
        if (null != linkedRepoMeta) {
          CrudRepository linkedRepo = linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable(linkedId,
                                                       (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                           .idAttribute()
                                                           .type());
          Object linkedEntity = linkedRepo.findOne(sChildId);
          if (null != linkedEntity) {
            // Remove linked entity from relationship based on property type
            if (attrMeta.isCollectionLike()) {
              Collection c = attrMeta.asCollection(entity);
              if (null != c) {
                c.remove(linkedEntity);
              }
            } else if (attrMeta.isSetLike()) {
              Set s = attrMeta.asSet(entity);
              if (null != s) {
                s.remove(linkedEntity);
              }
            } else if (attrMeta.isMapLike()) {
              Object keyToRemove = null;
              Map<Object, Object> m = attrMeta.asMap(entity);
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

            model.put(STATUS, HttpStatus.NO_CONTENT);
            return new ModelAndView(viewName("empty"), model);
          }
        }
      }
    }

    model.put(STATUS, HttpStatus.NOT_FOUND);
    return new ModelAndView(viewName("empty"), model);
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(OptimisticLockingFailureException.class)
  @ResponseBody
  public ResponseEntity handleLockingFailure(OptimisticLockingFailureException ex) throws IOException {
    LOG.error(ex.getMessage(), ex);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map m = new HashMap();
    m.put("message", ex.getMessage());
    return new ResponseEntity(objectMapper.writeValueAsBytes(m), headers, HttpStatus.CONFLICT);
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(RepositoryConstraintViolationException.class)
  public Model handleValidationFailure(RepositoryConstraintViolationException ex) throws IOException {
    LOG.error(ex.getMessage(), ex);
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

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ResponseEntity handleMessageConversionFailure(HttpMessageNotReadableException ex) throws IOException {
    LOG.error(ex.getMessage(), ex);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map m = new HashMap();
    m.put("message", ex.getMessage());
    return new ResponseEntity(objectMapper.writeValueAsBytes(m), headers, HttpStatus.BAD_REQUEST);
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
      CrudRepository repo = repoMeta.repository();
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
  private Map<String, Object> extractPropertiesLinkAware(String repoRel,
                                                         Object entity,
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
      URI uri = buildUri(baseUri, attrName);
      Link l = new SimpleLink(repoRel + "." + entity.getClass().getSimpleName() + "." + attrName, uri);
      List<Link> links = (List<Link>) entityDto.get(LINKS);
      if (null == links) {
        links = new ArrayList<Link>();
        entityDto.put(LINKS, links);
      }
      links.add(l);
    }

    return entityDto;
  }

  private String viewName(String name) {
    return "org.springframework.data.rest." + name;
  }

}
