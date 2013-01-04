package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.invoke.MethodParameterConversionService;
import org.springframework.data.rest.repository.support.ResourceMappingUtils;
import org.springframework.data.rest.webmvc.support.BaseUriLinkBuilder;
import org.springframework.data.rest.webmvc.support.ConstraintViolationExceptionMessage;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.data.rest.webmvc.support.JsonpResponse;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 */
public class AbstractRepositoryRestController implements ApplicationContextAware {

  static final    Resource<?>            EMPTY_RESOURCE      = new Resource<Object>(Collections.emptyList());
  static final    Resources<Resource<?>> EMPTY_RESOURCES     = new Resources<Resource<?>>(Collections.<Resource<?>>emptyList());
  static final    Iterable<Resource<?>>  EMPTY_RESOURCE_LIST = Collections.emptyList();
  static final    TypeDescriptor         STRING_TYPE         = TypeDescriptor.valueOf(String.class);
  protected final Logger                 LOG                 = LoggerFactory.getLogger(getClass());
  protected final Repositories                     repositories;
  protected final RepositoryRestConfiguration      config;
  protected final DomainClassConverter             domainClassConverter;
  protected final ConversionService                conversionService;
  protected final MethodParameterConversionService methodParameterConversionService;
  protected       ApplicationContext               applicationContext;

  @Autowired
  public AbstractRepositoryRestController(Repositories repositories,
                                          RepositoryRestConfiguration config,
                                          DomainClassConverter domainClassConverter,
                                          ConversionService conversionService) {
    this.repositories = repositories;
    this.config = config;
    this.domainClassConverter = domainClassConverter;
    this.conversionService = conversionService;
    this.methodParameterConversionService = new MethodParameterConversionService(conversionService);
  }

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @ExceptionHandler({
                        NullPointerException.class
                    })
  @ResponseBody
  public ResponseEntity<?> handleNPE(NullPointerException npe) {
    return errorResponse(npe, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({
                        ResourceNotFoundException.class
                    })
  @ResponseBody
  public ResponseEntity<?> handleNotFound() {
    return notFound();
  }

  @ExceptionHandler({
                        NoSuchMethodError.class,
                        HttpRequestMethodNotSupportedException.class
                    })
  @ResponseBody
  public ResponseEntity<?> handleNoSuchMethod() {
    return errorResponse(null, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler({
                        HttpMessageNotReadableException.class,
                        HttpMessageNotWritableException.class
                    })
  @ResponseBody
  public ResponseEntity<ExceptionMessage> handleNotReadable(HttpMessageNotReadableException e) {
    return badRequest(e);
  }

  /**
   * Handle failures commonly thrown from code tries to read incoming data and convert or cast it to the right type.
   *
   * @param t
   *
   * @return
   *
   * @throws java.io.IOException
   */
  @ExceptionHandler({
                        InvocationTargetException.class,
                        IllegalArgumentException.class,
                        ClassCastException.class,
                        ConversionFailedException.class
                    })
  @ResponseBody
  public ResponseEntity<ExceptionMessage> handleMiscFailures(Throwable t) {
    return badRequest(t);
  }

  @ExceptionHandler({
                        ConstraintViolationException.class
                    })
  @ResponseBody
  public ResponseEntity handleConstraintViolationException(ConstraintViolationException cve) {
    return response(null,
                    new ConstraintViolationExceptionMessage(cve, applicationContext),
                    HttpStatus.CONFLICT);
  }

  @ExceptionHandler({
                        RepositoryConstraintViolationException.class
                    })
  @ResponseBody
  public ResponseEntity handleRepositoryConstraintViolationException(RepositoryConstraintViolationException rcve) {
    return response(null,
                    new RepositoryConstraintViolationExceptionMessage(rcve, applicationContext),
                    HttpStatus.CONFLICT);
  }

  /**
   * Send a 409 Conflict in case of concurrent modification.
   *
   * @param ex
   *
   * @return
   */
  @SuppressWarnings({"unchecked"})
  @ExceptionHandler({
                        OptimisticLockingFailureException.class,
                        DataIntegrityViolationException.class
                    })
  @ResponseBody
  public ResponseEntity handleConflict(Exception ex) {
    return errorResponse(null, ex, HttpStatus.CONFLICT);
  }

  protected <T> ResponseEntity<T> notFound() {
    return notFound(null, null);
  }

  protected <T> ResponseEntity<T> notFound(HttpHeaders headers, T body) {
    return response(headers, body, HttpStatus.NOT_FOUND);
  }

  protected <T extends Throwable> ResponseEntity<ExceptionMessage> badRequest(T throwable) {
    return badRequest(null, throwable);
  }

  protected <T extends Throwable> ResponseEntity<ExceptionMessage> badRequest(HttpHeaders headers, T throwable) {
    return errorResponse(headers, throwable, HttpStatus.BAD_REQUEST);
  }

  public <T extends Throwable> ResponseEntity<ExceptionMessage> internalServerError(T throwable) {
    return internalServerError(null, throwable);
  }

  public <T extends Throwable> ResponseEntity<ExceptionMessage> internalServerError(HttpHeaders headers, T throwable) {
    return errorResponse(headers, throwable, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(T throwable,
                                                                              HttpStatus status) {
    return errorResponse(null, throwable, status);
  }

  public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(HttpHeaders headers,
                                                                              T throwable,
                                                                              HttpStatus status) {
    LOG.error(throwable.getMessage(), throwable);
    return response(headers, new ExceptionMessage(throwable), status);
  }

  public <T> ResponseEntity<T> response(HttpHeaders headers, T body, HttpStatus status) {
    HttpHeaders hdrs = new HttpHeaders();
    if(null != headers) {
      hdrs.putAll(headers);
    }
    return new ResponseEntity<T>(body, hdrs, status);
  }

  public <R extends Resource<?>> ResponseEntity<Resource<?>> resourceResponse(HttpHeaders headers,
                                                                              R resource,
                                                                              HttpStatus status) {
    HttpHeaders hdrs = new HttpHeaders();
    if(null != headers) {
      hdrs.putAll(headers);
    }
    return new ResponseEntity<Resource<?>>(resource, hdrs, status);
  }

  protected <T> JsonpResponse<T> jsonpWrapResponse(RepositoryRestRequest repoRequest,
                                                   T response,
                                                   HttpStatus status) {
    return jsonpWrapResponse(repoRequest, response, null, status);
  }

  protected <T> JsonpResponse<T> jsonpWrapResponse(RepositoryRestRequest repoRequest,
                                                   ResponseEntity<T> response) {
    return jsonpWrapResponse(repoRequest,
                             response.getBody(),
                             response.getHeaders(),
                             response.getStatusCode());
  }

  protected <T> JsonpResponse<T> jsonpWrapResponse(RepositoryRestRequest repoRequest,
                                                   T response,
                                                   HttpHeaders headers,
                                                   HttpStatus status) {
    String callback = repoRequest.getRequest().getParameter(config.getJsonpParamName());
    String errback = repoRequest.getRequest().getParameter(config.getJsonpOnErrParamName());
    ResponseEntity<T> newResponse;
    if(null != headers) {
      newResponse = new ResponseEntity<T>(response, headers, status);
    } else {
      newResponse = new ResponseEntity<T>(response, status);
    }
    return new JsonpResponse<T>(newResponse,
                                (null != callback ? callback : config.getJsonpParamName()),
                                (null != errback ? errback : config.getJsonpOnErrParamName()));
  }

  protected List<Link> queryMethodLinks(URI baseUri, Class<?> domainType) {
    List<Link> links = new ArrayList<Link>();
    RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(domainType);
    ResourceMapping repoMapping = ResourceMappingUtils.merge(
        repoInfo.getRepositoryInterface(),
        config.getResourceMappingForRepository(repoInfo.getRepositoryInterface())
    );
    for(Method method : repoInfo.getQueryMethods()) {
      LinkBuilder linkBuilder = BaseUriLinkBuilder.create(buildUri(baseUri, repoMapping.getPath(), "search"));
      ResourceMapping methodMapping = ResourceMappingUtils.merge(method,
                                                                 repoMapping.getResourceMappingFor(method.getName()));
      links.add(linkBuilder.slash(methodMapping.getPath())
                           .withRel(repoMapping.getRel() + "." + methodMapping.getRel()));
    }
    return links;
  }

  protected Link resourceLink(RepositoryRestRequest repoRequest, Resource resource) {
    ResourceMapping repoMapping = repoRequest.getRepositoryResourceMapping();
    ResourceMapping entityMapping = repoRequest.getPersistentEntityResourceMapping();

    Link selfLink = resource.getLink("self");
    String rel = repoMapping.getRel() + "." + entityMapping.getRel();
    return new Link(selfLink.getHref(), rel);
  }

}
