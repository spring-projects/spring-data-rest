package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.context.AfterLinkDeleteEvent;
import org.springframework.data.rest.repository.context.AfterLinkSaveEvent;
import org.springframework.data.rest.repository.context.BeforeLinkDeleteEvent;
import org.springframework.data.rest.repository.context.BeforeLinkSaveEvent;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.webmvc.support.JsonpResponse;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 */
@Controller
@RequestMapping("/{repository}/{id}/{property}")
public class RepositoryPropertyReferenceController extends AbstractRepositoryRestController {

  public RepositoryPropertyReferenceController(Repositories repositories,
                                               RepositoryRestConfiguration config,
                                               DomainClassConverter domainClassConverter,
                                               ConversionService conversionService) {
    super(repositories, config, domainClassConverter, conversionService);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {
          "application/json",
          "application/x-spring-data-verbose+json"
      }
  )
  @ResponseBody
  public ResponseEntity<Resource<?>> followPropertyReference(final RepositoryRestRequest repoRequest,
                                                             @PathVariable String id,
                                                             @PathVariable String property)
      throws ResourceNotFoundException, NoSuchMethodException {
    final HttpHeaders headers = new HttpHeaders();
    Function<ReferencedProperty, Resource<?>> handler = new Function<ReferencedProperty, Resource<?>>() {
      @Override public Resource<?> apply(ReferencedProperty prop) {
        if(prop.property.isCollectionLike()) {
          List<Resource<?>> resources = new ArrayList<Resource<?>>();
          PersistentEntity entity = repositories.getPersistentEntity(prop.propertyType);
          for(Object obj : ((Iterable)prop.propertyValue)) {
            PersistentEntityResource per = PersistentEntityResource.wrap(entity, obj, repoRequest.getBaseUri());
            Link selfLink = repoRequest.buildEntitySelfLink(obj, conversionService);
            per.add(selfLink);
            resources.add(per);
          }

          return new Resource<Object>(resources);
        } else if(prop.property.isMap()) {
          Map<Object, Resource<?>> resources = new HashMap<Object, Resource<?>>();
          PersistentEntity entity = repositories.getPersistentEntity(prop.propertyType);
          for(Map.Entry<Object, Object> entry : ((Map<Object, Object>)prop.propertyValue).entrySet()) {
            PersistentEntityResource per = PersistentEntityResource.wrap(entity,
                                                                         entry.getValue(),
                                                                         repoRequest.getBaseUri());
            Link selfLink = repoRequest.buildEntitySelfLink(entry.getValue(), conversionService);
            per.add(selfLink);
            resources.put(entry.getKey(), per);
          }

          return new Resource<Object>(resources);
        } else {
          PersistentEntityResource per = PersistentEntityResource.wrap(repositories.getPersistentEntity(prop.propertyType),
                                                                       prop.propertyValue,
                                                                       repoRequest.getBaseUri());
          Link selfLink = repoRequest.buildEntitySelfLink(prop.propertyValue, conversionService);
          per.add(selfLink);

          headers.set("Content-Location", selfLink.getHref());

          return new Resource<Object>(per);
        }
      }
    };
    Resource<?> responseResource = doWithReferencedProperty(repoRequest,
                                                            id,
                                                            property,
                                                            handler);
    return resourceResponse(headers, responseResource, HttpStatus.OK);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{propertyId}",
      method = RequestMethod.GET,
      produces = {
          "application/json",
          "application/x-spring-data-verbose+json"
      }
  )
  @ResponseBody
  public ResponseEntity<Resource<?>> followPropertyReference(final RepositoryRestRequest repoRequest,
                                                             @PathVariable String id,
                                                             @PathVariable String property,
                                                             final @PathVariable String propertyId)
      throws ResourceNotFoundException, NoSuchMethodException {
    final HttpHeaders headers = new HttpHeaders();
    Function<ReferencedProperty, Resource<?>> handler = new Function<ReferencedProperty, Resource<?>>() {
      @Override public Resource<?> apply(ReferencedProperty prop) {
        if(prop.property.isCollectionLike()) {
          PersistentEntity entity = repositories.getPersistentEntity(prop.propertyType);
          for(Object obj : ((Iterable)prop.propertyValue)) {
            BeanWrapper propValWrapper = BeanWrapper.create(obj, conversionService);
            String sId = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();
            if(propertyId.equals(sId)) {
              PersistentEntityResource per = PersistentEntityResource.wrap(entity, obj, repoRequest.getBaseUri());
              Link selfLink = repoRequest.buildEntitySelfLink(obj, conversionService);
              per.add(selfLink);
              headers.set("Content-Location", selfLink.getHref());
              return new Resource<Object>(per);
            }
          }
        } else if(prop.property.isMap()) {
          PersistentEntity entity = repositories.getPersistentEntity(prop.propertyType);
          for(Map.Entry<Object, Object> entry : ((Map<Object, Object>)prop.propertyValue).entrySet()) {
            BeanWrapper propValWrapper = BeanWrapper.create(entry.getValue(), conversionService);
            String sId = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();
            if(propertyId.equals(sId)) {
              PersistentEntityResource per = PersistentEntityResource.wrap(entity,
                                                                           entry.getValue(),
                                                                           repoRequest.getBaseUri());
              Link selfLink = repoRequest.buildEntitySelfLink(entry.getValue(), conversionService);
              per.add(selfLink);
              headers.set("Content-Location", selfLink.getHref());
              return new Resource<Object>(per, selfLink);
            }
          }
        } else {
          return new Resource<Object>(prop.propertyValue);
        }
        throw new IllegalArgumentException(new ResourceNotFoundException());
      }
    };
    Resource<?> responseResource = doWithReferencedProperty(repoRequest,
                                                            id,
                                                            property,
                                                            handler);
    return resourceResponse(headers, responseResource, HttpStatus.OK);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {
          "application/x-spring-data-compact+json",
          "text/uri-list"
      }
  )
  @ResponseBody
  public ResponseEntity<Resource<?>> followPropertyReferenceCompact(RepositoryRestRequest repoRequest,
                                                                    @PathVariable String id,
                                                                    @PathVariable String property)
      throws ResourceNotFoundException, NoSuchMethodException {
    ResponseEntity<Resource<?>> response = followPropertyReference(repoRequest, id, property);
    if(response.getStatusCode() != HttpStatus.OK) {
      return response;
    }

    ResourceMapping repoMapping = repoRequest.getRepositoryResourceMapping();
    ResourceMapping entityMapping = repoRequest.getPersistentEntityResourceMapping();
    ResourceMapping propMapping = entityMapping.getResourceMappingFor(entityMapping.getNameForPath(property));
    String propRel = (null != propMapping ? propMapping.getRel() : property);

    Resource<?> resource = response.getBody();

    List<Link> links = new ArrayList<Link>();

    URI entityBaseUri = buildUri(repoRequest.getBaseUri(),
                                 repoMapping.getPath(),
                                 id,
                                 property);

    if(resource.getContent() instanceof Iterable) {
      for(Resource<?> res : (Iterable<Resource<?>>)resource.getContent()) {
        Link propLink = propertyReferenceLink(res, entityBaseUri, propRel);
        links.add(propLink);
      }
    } else if(resource.getContent() instanceof Map) {
      for(Map.Entry<Object, Resource<?>> entry : ((Map<Object, Resource<?>>)resource.getContent()).entrySet()) {
        Link l = new Link(entry.getValue().getLink("self").getHref(), conversionService.convert(entry.getKey(),
                                                                                                String.class));
        links.add(l);
      }
    } else {
      links.add(new Link(entityBaseUri.toString(), propRel));
    }

    return resourceResponse(null, new Resource<Object>(EMPTY_RESOURCE_LIST, links), HttpStatus.OK);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {
          "application/javascript"
      }
  )
  @ResponseBody
  public JsonpResponse<?> jsonpFollowPropertyReference(RepositoryRestRequest repoRequest,
                                                       @PathVariable String id,
                                                       @PathVariable String property)
      throws ResourceNotFoundException, NoSuchMethodException {
    return jsonpWrapResponse(repoRequest,
                             followPropertyReference(repoRequest,
                                                     id,
                                                     property),
                             HttpStatus.OK);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{propertyId}",
      method = RequestMethod.GET,
      produces = {
          "application/javascript"
      }
  )
  @ResponseBody
  public JsonpResponse<?> jsonpFollowPropertyReference(RepositoryRestRequest repoRequest,
                                                       @PathVariable String id,
                                                       @PathVariable String property,
                                                       @PathVariable String propertyId)
      throws ResourceNotFoundException, NoSuchMethodException {
    return jsonpWrapResponse(repoRequest,
                             followPropertyReference(repoRequest,
                                                     id,
                                                     property,
                                                     propertyId),
                             HttpStatus.OK);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      method = {
          RequestMethod.POST,
          RequestMethod.PUT
      },
      consumes = {
          "application/json",
          "application/x-spring-data-compact+json",
          "text/uri-list"
      }
  )
  @ResponseBody
  public ResponseEntity<Resource<?>> createPropertyReference(final RepositoryRestRequest repoRequest,
                                                             final @RequestBody Resource<Object> incoming,
                                                             @PathVariable String id,
                                                             @PathVariable String property)
      throws ResourceNotFoundException, NoSuchMethodException {
    final RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
    if(!repoMethodInvoker.hasSaveOne()) {
      throw new NoSuchMethodException();
    }
    Function<ReferencedProperty, Resource<?>> handler = new Function<ReferencedProperty, Resource<?>>() {
      @Override public Resource<?> apply(ReferencedProperty prop) {
        if(prop.property.isCollectionLike()) {
          Collection coll = new ArrayList();
          if("POST".equals(repoRequest.getRequest().getMethod())) {
            coll.addAll((Collection)prop.propertyValue);
          }
          for(Link l : incoming.getLinks()) {
            Object propVal = loadPropertyValue(prop.propertyType, l.getHref());
            coll.add(propVal);
          }
          prop.wrapper.setProperty(prop.property, coll);
        } else if(prop.property.isMap()) {
          Map m = new HashMap();
          if("POST".equals(repoRequest.getRequest().getMethod())) {
            m.putAll((Map)prop.propertyValue);
          }
          for(Link l : incoming.getLinks()) {
            Object propVal = loadPropertyValue(prop.propertyType, l.getHref());
            m.put(l.getRel(), propVal);
          }
          prop.wrapper.setProperty(prop.property, m);
        } else {
          if("POST".equals(repoRequest.getRequest().getMethod())) {
            throw new IllegalStateException(
                "Cannot POST a reference to this singular property since the property type is not a List or a Map.");
          }
          if(incoming.getLinks().size() != 1) {
            throw new IllegalArgumentException(
                "Must send only 1 link to update a property reference that isn't a List or a Map.");
          }
          Object propVal = loadPropertyValue(prop.propertyType, incoming.getLinks().get(0).getHref());
          prop.wrapper.setProperty(prop.property, propVal);
        }

        applicationContext.publishEvent(new BeforeLinkSaveEvent(prop.wrapper.getBean(), prop.propertyValue));
        Object result = repoMethodInvoker.save(prop.wrapper.getBean());
        applicationContext.publishEvent(new AfterLinkSaveEvent(result, prop.propertyValue));
        return null;
      }
    };
    doWithReferencedProperty(repoRequest,
                             id,
                             property,
                             handler);
    return resourceResponse(null, EMPTY_RESOURCE, HttpStatus.NO_CONTENT);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      method = {
          RequestMethod.POST,
          RequestMethod.PUT
      },
      consumes = {
          "application/javascript"
      }
  )
  @ResponseBody
  public JsonpResponse<?> jsonpCreatePropertyReference(final RepositoryRestRequest repoRequest,
                                                       final @RequestBody Resource<Object> incoming,
                                                       @PathVariable String id,
                                                       @PathVariable String property)
      throws ResourceNotFoundException, NoSuchMethodException {
    return jsonpWrapResponse(repoRequest, createPropertyReference(repoRequest,
                                                                  incoming,
                                                                  id,
                                                                  property));
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{propertyId}",
      method = RequestMethod.DELETE
  )
  @ResponseBody
  public ResponseEntity<Resource<?>> deletePropertyReference(final RepositoryRestRequest repoRequest,
                                                             @PathVariable String id,
                                                             @PathVariable String property,
                                                             final @PathVariable String propertyId)
      throws ResourceNotFoundException, NoSuchMethodException {
    final RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
    if(!repoMethodInvoker.hasDeleteOne()) {
      throw new NoSuchMethodException();
    }

    Function<ReferencedProperty, Resource<?>> handler = new Function<ReferencedProperty, Resource<?>>() {
      @Override public Resource<?> apply(ReferencedProperty prop) {
        if(null == prop.propertyValue) {
          return null;
        }
        if(prop.property.isCollectionLike()) {
          Collection coll = new ArrayList();
          for(Object obj : (Collection)prop.propertyValue) {
            BeanWrapper propValWrapper = BeanWrapper.create(obj, conversionService);
            String s = (String)propValWrapper.getProperty(prop.entity.getIdProperty(), String.class, false);
            if(!propertyId.equals(s)) {
              coll.add(obj);
            }
          }
          prop.wrapper.setProperty(prop.property, coll);
        } else if(prop.property.isMap()) {
          Map m = new HashMap();
          for(Map.Entry<Object, Object> entry : ((Map<Object, Object>)prop.propertyValue).entrySet()) {
            BeanWrapper propValWrapper = BeanWrapper.create(entry.getValue(), conversionService);
            String s = (String)propValWrapper.getProperty(prop.entity.getIdProperty(), String.class, false);
            if(!propertyId.equals(s)) {
              m.put(entry.getKey(), entry.getValue());
            }
          }
          prop.wrapper.setProperty(prop.property, m);
        } else {
          prop.wrapper.setProperty(prop.property, null);
        }

        applicationContext.publishEvent(new BeforeLinkDeleteEvent(prop.wrapper.getBean(), prop.propertyValue));
        Object result = repoMethodInvoker.save(prop.wrapper.getBean());
        applicationContext.publishEvent(new AfterLinkDeleteEvent(result, prop.propertyValue));
        return null;
      }
    };
    doWithReferencedProperty(repoRequest,
                             id,
                             property,
                             handler);

    return resourceResponse(null, EMPTY_RESOURCE, HttpStatus.NO_CONTENT);
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{propertyId}",
      method = RequestMethod.DELETE,
      produces = {
          "application/javascript"
      }
  )
  @ResponseBody
  public JsonpResponse<?> jsonpDeletePropertyReference(final RepositoryRestRequest repoRequest,
                                                       @PathVariable String id,
                                                       @PathVariable String property,
                                                       final @PathVariable String propertyId)
      throws ResourceNotFoundException, NoSuchMethodException {
    return jsonpWrapResponse(repoRequest, deletePropertyReference(repoRequest,
                                                                  id,
                                                                  property,
                                                                  propertyId));
  }

  private Link propertyReferenceLink(Resource<?> resource,
                                     URI baseUri,
                                     String rel) {
    Link selfLink = resource.getLink("self");
    String objId = selfLink.getHref().substring(selfLink.getHref().lastIndexOf('/') + 1);
    return new Link(buildUri(baseUri, objId).toString(), rel);
  }

  private Object loadPropertyValue(Class<?> type, String href) {
    String id = href.substring(href.lastIndexOf('/') + 1);
    return domainClassConverter.convert(id,
                                        STRING_TYPE,
                                        TypeDescriptor.valueOf(type));
  }

  @SuppressWarnings({"unchecked"})
  private Resource<?> doWithReferencedProperty(RepositoryRestRequest repoRequest,
                                               String id,
                                               String propertyPath,
                                               Function<ReferencedProperty, Resource<?>> handler)
      throws ResourceNotFoundException, NoSuchMethodException {
    RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
    if(!repoMethodInvoker.hasFindOne()) {
      throw new NoSuchMethodException();
    }

    Object domainObj = domainClassConverter.convert(id,
                                                    STRING_TYPE,
                                                    TypeDescriptor.valueOf(repoRequest.getPersistentEntity()
                                                                                      .getType()));
    if(null == domainObj) {
      throw new ResourceNotFoundException();
    }

    String propertyName = repoRequest.getPersistentEntityResourceMapping().getNameForPath(propertyPath);
    PersistentProperty prop = repoRequest.getPersistentEntity().getPersistentProperty(propertyName);
    if(null == prop) {
      throw new ResourceNotFoundException();
    }

    BeanWrapper wrapper = BeanWrapper.create(domainObj, conversionService);
    Object propVal = wrapper.getProperty(prop);
    if(null == propVal) {
      throw new ResourceNotFoundException();
    }

    return handler.apply(new ReferencedProperty(prop,
                                                propVal,
                                                wrapper));
  }

  private class ReferencedProperty {
    final PersistentEntity        entity;
    final PersistentProperty      property;
    final Class<?>                propertyType;
    final Object                  propertyValue;
    final BeanWrapper             wrapper;
    final RepositoryInformation   propertyRepoInfo;
    final Object                  propertyRepo;
    final RepositoryMethodInvoker repoMethodInvoker;

    private ReferencedProperty(PersistentProperty property,
                               Object propertyValue,
                               BeanWrapper wrapper) {
      this.property = property;
      this.propertyValue = propertyValue;
      this.wrapper = wrapper;
      if(property.isCollectionLike()) {
        this.propertyType = property.getComponentType();
      } else if(property.isMap()) {
        this.propertyType = property.getMapValueType();
      } else {
        this.propertyType = property.getType();
      }
      this.propertyRepoInfo = repositories.getRepositoryInformationFor(propertyType);
      this.entity = repositories.getPersistentEntity(propertyType);
      this.propertyRepo = repositories.getRepositoryFor(entity.getType());
      this.repoMethodInvoker = new RepositoryMethodInvoker(propertyRepo, propertyRepoInfo, entity);
    }
  }

}
