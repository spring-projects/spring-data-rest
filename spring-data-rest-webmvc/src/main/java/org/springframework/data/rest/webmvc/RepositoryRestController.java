package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
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
import org.springframework.data.rest.repository.RepositoryNotFoundException;
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
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class RepositoryRestController
    extends RepositoryExporterSupport<RepositoryRestController>
    implements ApplicationContextAware,
               InitializingBean {

  public static final String LOCATION = "Location";
  public static final String SELF = "self";
  public static final String LINKS = "_links";
  public static final Charset DEFAULT_CHARSET = Charset.forName( "UTF-8" );

  private static final Logger LOG = LoggerFactory.getLogger( RepositoryRestController.class );
  private static final HttpHeaders EMPTY_HEADERS = new HttpHeaders();
  private static final List<MediaType> ALL_TYPES = Arrays.asList( MediaType.ALL );
  private static final MediaType URI_LIST = new MediaType( "text",
                                                           "uri-list",
                                                           UriListHttpMessageConverter.DEFAULT_CHARSET );

  private ApplicationContext applicationContext;

  @Autowired(required = false)
  private DelegatingConversionService conversionService = new DelegatingConversionService(
      new DefaultFormattingConversionService()
  );
  @Autowired(required = false)
  private List<HttpMessageConverter> httpMessageConverters = new ArrayList<HttpMessageConverter>();
  private SortedSet<MediaType> availableMediaTypes = new TreeSet<MediaType>();
  @Autowired(required = false)
  private RepositoryRestConfiguration config = RepositoryRestConfiguration.DEFAULT;
  private Map<String, Handler<Object, Object>> resourceHandlers = Collections.emptyMap();
  private ObjectMapper objectMapper = new ObjectMapper();

  {
    List<HttpMessageConverter> httpMessageConverters = new ArrayList<HttpMessageConverter>();
    httpMessageConverters.add( 0, new StringHttpMessageConverter() );
    httpMessageConverters.add( 0, new ByteArrayHttpMessageConverter() );
    httpMessageConverters.add( 0, new FormHttpMessageConverter() );
    httpMessageConverters.add( 0, new UriListHttpMessageConverter() );
    httpMessageConverters.add( 0, JacksonUtil.createJacksonHttpMessageConverter( objectMapper ) );

    setHttpMessageConverters( httpMessageConverters );
  }

  @Override public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setConversionService( ConversionService conversionService ) {
    if ( null != conversionService ) {
      this.conversionService.addConversionServices( conversionService );
    }
  }

  public ConversionService conversionService() {
    return conversionService;
  }

  public RepositoryRestController conversionService( ConversionService conversionService ) {
    setConversionService( conversionService );
    return this;
  }

  public List<HttpMessageConverter> getHttpMessageConverters() {
    return httpMessageConverters;
  }

  @SuppressWarnings({"unchecked"})
  public void setHttpMessageConverters( List<HttpMessageConverter> httpMessageConverters ) {
    Assert.notNull( httpMessageConverters );
    this.httpMessageConverters = httpMessageConverters;
    this.availableMediaTypes.clear();
    for ( HttpMessageConverter conv : httpMessageConverters ) {
      availableMediaTypes.addAll( conv.getSupportedMediaTypes() );
    }
    for ( HttpMessageConverter conv : config.getCustomConverters() ) {
      availableMediaTypes.addAll( conv.getSupportedMediaTypes() );
    }
  }

  public List<HttpMessageConverter> httpMessageConverters() {
    return httpMessageConverters;
  }

  public RepositoryRestController httpMessageConverters( List<HttpMessageConverter> httpMessageConverters ) {
    setHttpMessageConverters( httpMessageConverters );
    return this;
  }

  public RepositoryRestConfiguration getRepositoryRestConfig() {
    return config;
  }

  public RepositoryRestController setRepositoryRestConfig( RepositoryRestConfiguration config ) {
    this.config = config;
    return this;
  }

  public Map<String, Handler<Object, Object>> getResourceHandlers() {
    return resourceHandlers;
  }

  public RepositoryRestController setResourceHandlers( Map<String, Handler<Object, Object>> resourceHandlers ) {
    this.resourceHandlers = resourceHandlers;
    return this;
  }

  public Map<String, Handler<Object, Object>> resourceHandlers() {
    return resourceHandlers;
  }

  public RepositoryRestController resourceHandlers( Map<String, Handler<Object, Object>> resourceHandlers ) {
    setResourceHandlers( resourceHandlers );
    return this;
  }

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
    for ( ConversionService convsvc : BeanFactoryUtils.beansOfTypeIncludingAncestors( applicationContext,
                                                                                      ConversionService.class )
        .values() ) {
      conversionService.addConversionServices( convsvc );
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> listRepositories( ServletServerHttpRequest request,
                                             UriComponentsBuilder uriBuilder )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    Links links = new Links();
    for ( RepositoryExporter repoExporter : repositoryExporters ) {
      for ( String name : (Set<String>) repoExporter.repositoryNames() ) {
        RepositoryMetadata repoMeta = repoExporter.repositoryMetadataFor( name );
        String rel = repoMeta.rel();
        URI path = buildUri( baseUri, name );
        links.add( new SimpleLink( rel, path ) );
      }
    }

    return negotiateResponse( request, HttpStatus.OK, EMPTY_HEADERS, links );
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> listEntities( ServletServerHttpRequest request,
                                         PagingAndSorting pageSort,
                                         UriComponentsBuilder uriBuilder,
                                         @PathVariable String repository )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );

    Page page = null;
    Iterator iter;
    if ( repoMeta.repository() instanceof PagingAndSortingRepository ) {
      page = ((PagingAndSortingRepository) repoMeta.repository()).findAll( pageSort );
      iter = page.iterator();
    } else {
      iter = repoMeta.repository().findAll().iterator();
    }

    Map<String, Object> resultMap = new HashMap<String, Object>();
    Links links = new Links();
    resultMap.put( LINKS, links.getLinks() );
    List resultList = new ArrayList();
    resultMap.put( "results", resultList );

    boolean returnLinks = shouldReturnLinks( request.getServletRequest().getHeader( "Accept" ) );
    if ( null != iter ) {
      while ( iter.hasNext() ) {
        Object o = iter.next();
        Serializable id = (Serializable) repoMeta.entityMetadata().idAttribute().get( o );
        if ( returnLinks ) {
          links.add( new SimpleLink( repoMeta.rel() + "." + o.getClass().getSimpleName(),
                                     buildUri( baseUri, repository, id.toString() ) ) );
        } else {
          Map<String, Object> entityDto = extractPropertiesLinkAware( repoMeta.rel(),
                                                                      o,
                                                                      repoMeta.entityMetadata(),
                                                                      buildUri( baseUri, repository, id.toString() ) );
          addSelfLink( baseUri, entityDto, repository, id.toString() );
          resultList.add( entityDto );
        }
      }
      links.add( new SimpleLink( repoMeta.rel() + ".search",
                                 buildUri( baseUri, repository, "search" ) ) );
    }

    // Add paging links
    if ( null != page ) {
      resultMap.put( "totalCount", page.getTotalElements() );
      resultMap.put( "totalPages", page.getTotalPages() );
      resultMap.put( "currentPage", page.getNumber() + 1 );
      // Copy over parameters
      UriComponentsBuilder urib = UriComponentsBuilder.fromUri( baseUri ).pathSegment( repository );
      for ( String name : ((Map<String, Object>) request.getServletRequest().getParameterMap()).keySet() ) {
        if ( !config.getPageParamName().equals( name ) && !config.getLimitParamName().equals( name )
            && !config.getSortParamName().equals( name ) ) {
          urib.queryParam( name, request.getServletRequest().getParameter( name ) );
        }
      }

      URI nextPrevBase = urib.build().toUri();
      maybeAddPrevNextLink(
          nextPrevBase,
          repoMeta,
          pageSort,
          page,
          !page.isFirstPage() && page.hasPreviousPage(),
          page.getNumber(),
          "prev",
          links
      );
      maybeAddPrevNextLink(
          nextPrevBase,
          repoMeta,
          pageSort,
          page,
          !page.isLastPage() && page.hasNextPage(),
          page.getNumber() + 2,
          "next",
          links
      );
    }

    return negotiateResponse( request, HttpStatus.OK, EMPTY_HEADERS, resultMap );
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/search",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> listQueryMethods( ServletServerHttpRequest request,
                                             UriComponentsBuilder uriBuilder,
                                             @PathVariable String repository )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Links links = new Links();

    for ( Map.Entry<String, RepositoryQueryMethod> entry : ((Map<String, RepositoryQueryMethod>) repoMeta.queryMethods())
        .entrySet() ) {
      String rel = repoMeta.rel() + "." + entry.getKey();
      URI path = buildUri( baseUri, repository, "search", entry.getKey() );
      RestResource resourceAnno = entry.getValue().method().getAnnotation( RestResource.class );
      if ( null != resourceAnno ) {
        if ( StringUtils.hasText( resourceAnno.path() ) ) {
          path = buildUri( baseUri, repository, "search", resourceAnno.path() );
        }
        if ( StringUtils.hasText( resourceAnno.rel() ) ) {
          rel = repoMeta.rel() + "." + resourceAnno.rel();
        }
      }
      links.add( new SimpleLink( rel, path ) );
    }

    return negotiateResponse( request, HttpStatus.OK, EMPTY_HEADERS, links );
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/search/{query}",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> query( ServletServerHttpRequest request,
                                  PagingAndSorting pageSort,
                                  UriComponentsBuilder uriBuilder,
                                  @PathVariable String repository,
                                  @PathVariable String query )
      throws InvocationTargetException,
             IllegalAccessException,
             IOException {
    URI baseUri = uriBuilder.build().toUri();
    Page page = null;

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Repository repo = repoMeta.repository();
    RepositoryQueryMethod queryMethod = repoMeta.queryMethod( query );
    if ( null == queryMethod ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    }

    Class<?>[] paramTypes = queryMethod.paramTypes();
    String[] paramNames = queryMethod.paramNames();
    Object[] paramVals = new Object[paramTypes.length];
    for ( int i = 0; i < paramVals.length; i++ ) {
      String queryVal = request.getServletRequest().getParameter( paramNames[i] );
      if ( String.class.isAssignableFrom( paramTypes[i] ) ) {
        // Param type is a String
        paramVals[i] = queryVal;
      } else if ( Pageable.class.isAssignableFrom( paramTypes[i] ) ) {
        // Handle paging
        paramVals[i] = pageSort;
      } else if ( Sort.class.isAssignableFrom( paramTypes[i] ) ) {
        // Handle sorting
        paramVals[i] = (null != pageSort ? pageSort.getSort() : null);
      } else if ( conversionService.canConvert( String.class, paramTypes[i] ) ) {
        // There's a converter from String -> param type
        paramVals[i] = conversionService.convert( queryVal, paramTypes[i] );
      } else {
        // Param type isn't a "simple" type or no converter exists, try JSON
        try {
          paramVals[i] = objectMapper.readValue( queryVal, paramTypes[i] );
        } catch ( IOException e ) {
          throw new IllegalArgumentException( e );
        }
      }
    }

    Object result = queryMethod.method().invoke( repo, paramVals );
    Iterator iter;
    if ( result instanceof Collection ) {
      iter = ((Collection) result).iterator();
    } else if ( result instanceof Page ) {
      page = (Page) result;
      iter = page.iterator();
    } else {
      List l = new ArrayList();
      l.add( result );
      iter = l.iterator();
    }

    Map<String, Object> resultMap = new HashMap<String, Object>();
    Links links = new Links();
    resultMap.put( LINKS, links.getLinks() );
    List resultList = new ArrayList();
    resultMap.put( "results", resultList );

    boolean returnLinks = shouldReturnLinks( request.getServletRequest().getHeader( "Accept" ) );
    while ( iter.hasNext() ) {
      Object obj = iter.next();
      RepositoryMetadata elemRepoMeta = repositoryMetadataFor( obj.getClass() );
      if ( null != elemRepoMeta ) {
        String id = elemRepoMeta.entityMetadata().idAttribute().get( obj ).toString();
        if ( returnLinks ) {
          String rel = elemRepoMeta.rel() + "." + elemRepoMeta.entityMetadata().type().getSimpleName();
          URI path = buildUri( baseUri, repository, id );
          links.add( new SimpleLink( rel, path ) );
        } else {
          Map<String, Object> entityDto = extractPropertiesLinkAware( repoMeta.rel(),
                                                                      obj,
                                                                      repoMeta.entityMetadata(),
                                                                      buildUri( baseUri, repository, id ) );
          addSelfLink( baseUri, entityDto, repository, id );
          resultList.add( entityDto );
        }
      }
    }

    // Add paging links
    if ( null != page ) {
      resultMap.put( "totalCount", page.getTotalElements() );
      resultMap.put( "totalPages", page.getTotalPages() );
      resultMap.put( "currentPage", page.getNumber() + 1 );
      // Copy over search parameters
      UriComponentsBuilder urib = UriComponentsBuilder.fromUri( baseUri ).pathSegment( repository, "search", query );
      for ( String name : ((Map<String, Object>) request.getServletRequest().getParameterMap()).keySet() ) {
        if ( !config.getPageParamName().equals( name ) && !config.getLimitParamName().equals( name )
            && !config.getSortParamName().equals( name ) ) {
          urib.queryParam( name, request.getServletRequest().getParameter( name ) );
        }
      }

      URI nextPrevBase = urib.build().toUri();
      maybeAddPrevNextLink(
          nextPrevBase,
          repoMeta,
          pageSort,
          page,
          !page.isFirstPage() && page.hasPreviousPage(),
          page.getNumber(),
          "prev",
          links
      );
      maybeAddPrevNextLink(
          nextPrevBase,
          repoMeta,
          pageSort,
          page,
          !page.isLastPage() && page.hasNextPage(),
          page.getNumber() + 2,
          "next",
          links
      );
    } else {
      resultMap.put( "totalCount", resultList.size() );
    }

    return negotiateResponse( request, HttpStatus.OK, EMPTY_HEADERS, resultMap );
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}",
      method = RequestMethod.POST
  )
  @ResponseBody
  public ResponseEntity<?> create( ServletServerHttpRequest request,
                                   HttpServletRequest servletRequest,
                                   UriComponentsBuilder uriBuilder,
                                   @PathVariable String repository )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    CrudRepository repo = repoMeta.repository();
    MediaType incomingMediaType = request.getHeaders().getContentType();
    final Object incoming = readIncoming( request, incomingMediaType, repoMeta.entityMetadata().type() );
    if ( null == incoming ) {
      return negotiateResponse( request, HttpStatus.BAD_REQUEST, EMPTY_HEADERS, null );
    } else {
      if ( null != applicationContext ) {
        applicationContext.publishEvent( new BeforeSaveEvent( incoming ) );
      }
      Object savedEntity = repo.save( incoming );
      if ( null != applicationContext ) {
        applicationContext.publishEvent( new AfterSaveEvent( savedEntity ) );
      }
      String sId = repoMeta.entityMetadata().idAttribute().get( savedEntity ).toString();

      URI selfUri = buildUri( baseUri, repository, sId );

      HttpHeaders headers = new HttpHeaders();
      headers.set( LOCATION, selfUri.toString() );

      Object body = null;
      if ( null != servletRequest.getParameter( "returnBody" ) && "true".equals( servletRequest.getParameter(
          "returnBody" ) ) ) {
        Map<String, Object> entityDto = extractPropertiesLinkAware( repoMeta.rel(),
                                                                    savedEntity,
                                                                    repoMeta.entityMetadata(),
                                                                    buildUri( baseUri, repository, sId ) );
        addSelfLink( baseUri, entityDto, repository, sId );
        body = entityDto;
      }
      return negotiateResponse( request, HttpStatus.CREATED, headers, body );
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> entity( ServletServerHttpRequest request,
                                   UriComponentsBuilder uriBuilder,
                                   @PathVariable String repository,
                                   @PathVariable String id )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      HttpHeaders headers = new HttpHeaders();
      if ( null != repoMeta.entityMetadata().versionAttribute() ) {
        Object version = repoMeta.entityMetadata().versionAttribute().get( entity );
        if ( null != version ) {
          List<String> etags = request.getHeaders().getIfNoneMatch();
          for ( String etag : etags ) {
            if ( ("\"" + version.toString() + "\"").equals( etag ) ) {
              return negotiateResponse( request, HttpStatus.NOT_MODIFIED, EMPTY_HEADERS, null );
            }
          }
          headers.set( "ETag", "\"" + version.toString() + "\"" );
        }
      }
      Map<String, Object> entityDto = extractPropertiesLinkAware( repoMeta.rel(),
                                                                  entity,
                                                                  repoMeta.entityMetadata(),
                                                                  buildUri( baseUri, repository, id ) );
      addSelfLink( baseUri, entityDto, repository, id );

      return negotiateResponse( request, HttpStatus.OK, headers, entityDto );
    }

  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = {
          RequestMethod.PUT,
          RequestMethod.POST
      }
  )
  @ResponseBody
  public ResponseEntity<?> createOrUpdate( ServletServerHttpRequest request,
                                           UriComponentsBuilder uriBuilder,
                                           @PathVariable String repository,
                                           @PathVariable String id )
      throws IOException,
             IllegalAccessException,
             InstantiationException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    Class<?> domainType = repoMeta.entityMetadata().type();

    final MediaType incomingMediaType = request.getHeaders().getContentType();
    final Object incoming = readIncoming( request, incomingMediaType, domainType );
    if ( null == incoming ) {
      throw new HttpMessageNotReadableException( "Could not create an instance of " + domainType.getSimpleName() + " from input." );
    } else {
      repoMeta.entityMetadata().idAttribute().set( serId, incoming );
      if ( request.getMethod() == HttpMethod.POST ) {
        if ( null != applicationContext ) {
          applicationContext.publishEvent( new BeforeSaveEvent( incoming ) );
        }
        Object savedEntity = repo.save( incoming );
        if ( null != applicationContext ) {
          applicationContext.publishEvent( new AfterSaveEvent( savedEntity ) );
        }
        URI selfUri = buildUri( baseUri, repository, id );
        HttpHeaders headers = new HttpHeaders();
        headers.set( LOCATION, selfUri.toString() );
        boolean returnBody = true;
        if ( null != request.getServletRequest().getParameter( "returnBody" ) ) {
          returnBody = Boolean.parseBoolean( request.getServletRequest().getParameter( "returnBody" ) );
        }
        return negotiateResponse( request, HttpStatus.CREATED, headers, (returnBody ? savedEntity : null) );
      } else {
        Object entity = repo.findOne( serId );
        if ( null == entity ) {
          return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
        } else {
          for ( AttributeMetadata attrMeta : (Collection<AttributeMetadata>) repoMeta.entityMetadata()
              .embeddedAttributes()
              .values() ) {
            Object incomingVal = attrMeta.get( incoming );
            if ( null != incomingVal ) {
              attrMeta.set( incomingVal, entity );
            }
          }
          if ( null != applicationContext ) {
            applicationContext.publishEvent( new BeforeSaveEvent( entity ) );
          }
          Object savedEntity = repo.save( entity );
          if ( null != applicationContext ) {
            applicationContext.publishEvent( new AfterSaveEvent( savedEntity ) );
          }
          return negotiateResponse( request, HttpStatus.NO_CONTENT, EMPTY_HEADERS, null );
        }
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}",
      method = RequestMethod.DELETE
  )
  @ResponseBody
  public ResponseEntity<?> deleteEntity( ServletServerHttpRequest request,
                                         @PathVariable String repository,
                                         @PathVariable String id )
      throws IOException {
    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      if ( null != applicationContext ) {
        applicationContext.publishEvent( new BeforeDeleteEvent( entity ) );
      }
      repo.delete( serId );
      if ( null != applicationContext ) {
        applicationContext.publishEvent( new AfterDeleteEvent( entity ) );
      }
      return negotiateResponse( request, HttpStatus.NO_CONTENT, EMPTY_HEADERS, null );
    }
  }


  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}",
      method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> propertyOfEntity( ServletServerHttpRequest request,
                                             UriComponentsBuilder uriBuilder,
                                             @PathVariable String repository,
                                             @PathVariable String id,
                                             @PathVariable String property )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute( property );
      if ( null == attrMeta ) {
        return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
      } else {
        Class<?> attrType = attrMeta.elementType();
        if ( null == attrType ) {
          attrType = attrMeta.type();
        }

        RepositoryMetadata propRepoMeta = repositoryMetadataFor( attrType );
        Object propVal = attrMeta.get( entity );
        AttributeMetadata idAttr = propRepoMeta.entityMetadata().idAttribute();
        if ( null != propVal ) {
          Links links = new Links();
          if ( propVal instanceof Collection ) {
            for ( Object o : (Collection) propVal ) {
              String propValId = idAttr.get( o ).toString();
              String rel = repository + "."
                  + entity.getClass().getSimpleName() + "."
                  + attrType.getSimpleName();
              URI path = buildUri( baseUri, repository, id, property, propValId );
              links.add( new SimpleLink( rel, path ) );
            }
          } else if ( propVal instanceof Map ) {
            for ( Map.Entry<Object, Object> entry : ((Map<Object, Object>) propVal).entrySet() ) {
              String propValId = idAttr.get( entry.getValue() ).toString();
              URI path = buildUri( baseUri, repository, id, property, propValId );
              Object oKey = entry.getKey();
              String sKey;
              if ( ClassUtils.isAssignable( oKey.getClass(), String.class ) ) {
                sKey = (String) oKey;
              } else {
                sKey = conversionService.convert( oKey, String.class );
              }
              String rel = repository + "." + entity.getClass().getSimpleName() + "." + sKey;
              links.add( new SimpleLink( rel, path ) );
            }
          } else {
            String propValId = idAttr.get( propVal ).toString();
            String rel = repository + "." + entity.getClass().getSimpleName() + "." + property;
            URI path = buildUri( baseUri, repository, id, property, propValId );
            links.add( new SimpleLink( rel, path ) );
          }
          return negotiateResponse( request, HttpStatus.OK, EMPTY_HEADERS, links );
        } else {
          return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
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
      }
  )
  @ResponseBody
  public ResponseEntity<?> updatePropertyOfEntity( final ServletServerHttpRequest request,
                                                   UriComponentsBuilder uriBuilder,
                                                   @PathVariable String repository,
                                                   @PathVariable String id,
                                                   final @PathVariable String property ) throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    final RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      final AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute( property );
      if ( null == attrMeta ) {
        return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
      } else {
        Object linked = attrMeta.get( entity );
        final AtomicReference<String> rel = new AtomicReference<String>();
        Handler<Object, ResponseEntity<?>> entityHandler = new Handler<Object, ResponseEntity<?>>() {
          @Override public ResponseEntity<?> handle( Object linkedEntity ) {
            if ( attrMeta.isCollectionLike() ) {
              Collection c = new ArrayList();
              Collection current = attrMeta.asCollection( entity );
              if ( request.getMethod() == HttpMethod.POST && null != current ) {
                c.addAll( current );
              }
              c.add( linkedEntity );
              attrMeta.set( c, entity );
            } else if ( attrMeta.isSetLike() ) {
              Set s = new HashSet();
              Set current = attrMeta.asSet( entity );
              if ( request.getMethod() == HttpMethod.POST && null != current ) {
                s.addAll( current );
              }
              s.add( linkedEntity );
              attrMeta.set( s, entity );
            } else if ( attrMeta.isMapLike() ) {
              Map m = new HashMap();
              Map current = attrMeta.asMap( entity );
              if ( request.getMethod() == HttpMethod.POST && null != current ) {
                m.putAll( current );
              }
              String key = rel.get();
              if ( null == key ) {
                try {
                  return negotiateResponse( request, HttpStatus.NOT_ACCEPTABLE, EMPTY_HEADERS, null );
                } catch ( IOException e ) {
                  throw new RuntimeException( e );
                }
              } else {
                m.put( rel.get(), linkedEntity );
                attrMeta.set( m, entity );
              }
            } else {
              attrMeta.set( linkedEntity, entity );
            }
            return null;
          }
        };
        MediaType incomingMediaType = request.getHeaders().getContentType();
        if ( incomingMediaType.getSubtype().startsWith( "uri-list" ) ) {
          BufferedReader in = new BufferedReader( new InputStreamReader( request.getBody() ) );
          String line;
          while ( null != (line = in.readLine()) ) {
            String sLinkUri = line.trim();
            Object o = resolveTopLevelResource( baseUri, sLinkUri );
            if ( null != o ) {
              ResponseEntity<?> possibleResponse = entityHandler.handle( o );
              if ( null != possibleResponse ) {
                return possibleResponse;
              }
            }
          }
        } else {
          final Map<String, List<Map<String, String>>> incoming = readIncoming( request,
                                                                                incomingMediaType,
                                                                                Map.class );
          for ( Map<String, String> link : incoming.get( LINKS ) ) {
            String sLinkUri = link.get( "href" );
            Object o = resolveTopLevelResource( baseUri, sLinkUri );
            rel.set( link.get( "rel" ) );
            if ( null != o ) {
              ResponseEntity<?> possibleResponse = entityHandler.handle( o );
              if ( null != possibleResponse ) {
                return possibleResponse;
              }
            }
          }
        }

        if ( null != applicationContext ) {
          applicationContext.publishEvent( new BeforeSaveEvent( entity ) );
          applicationContext.publishEvent( new BeforeLinkSaveEvent( entity, linked ) );
        }
        Object savedEntity = repo.save( entity );
        if ( null != applicationContext ) {
          linked = attrMeta.get( savedEntity );
          applicationContext.publishEvent( new AfterLinkSaveEvent( savedEntity, linked ) );
          applicationContext.publishEvent( new AfterSaveEvent( savedEntity ) );
        }

        if ( request.getMethod() == HttpMethod.PUT ) {
          return negotiateResponse( request, HttpStatus.NO_CONTENT, EMPTY_HEADERS, null );
        } else {
          return negotiateResponse( request, HttpStatus.CREATED, EMPTY_HEADERS, null );
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
  @ResponseBody
  public ResponseEntity<?> clearLinks( ServletServerHttpRequest request,
                                       @PathVariable String repository,
                                       @PathVariable String id,
                                       @PathVariable String property )
      throws IOException {
    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute( property );
      if ( null != attrMeta ) {
        Object linked = attrMeta.get( entity );
        attrMeta.set( null, entity );

        if ( null != applicationContext ) {
          applicationContext.publishEvent( new BeforeLinkSaveEvent( entity, linked ) );
        }
        Object savedEntity = repo.save( entity );
        if ( null != applicationContext ) {
          applicationContext.publishEvent( new AfterLinkSaveEvent( savedEntity, null ) );
        }

        return negotiateResponse( request, HttpStatus.NO_CONTENT, EMPTY_HEADERS, null );
      } else {
        return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{linkedId}",
      method = {
          RequestMethod.GET
      }
  )
  @ResponseBody
  public ResponseEntity<?> linkedEntity( ServletServerHttpRequest request,
                                         UriComponentsBuilder uriBuilder,
                                         @PathVariable String repository,
                                         @PathVariable String id,
                                         @PathVariable String property,
                                         @PathVariable String linkedId )
      throws IOException {
    URI baseUri = uriBuilder.build().toUri();

    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    final Object entity = repo.findOne( serId );
    if ( null != entity ) {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute( property );
      if ( null != attrMeta ) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor( attrMeta );
        if ( null != linkedRepoMeta ) {
          CrudRepository linkedRepo = linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable( linkedId,
                                                        (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                            .idAttribute()
                                                            .type() );
          Object linkedEntity = linkedRepo.findOne( sChildId );
          if ( null != linkedEntity ) {
            Map<String, Object> entityDto = extractPropertiesLinkAware( linkedRepoMeta.rel(),
                                                                        linkedEntity,
                                                                        linkedRepoMeta.entityMetadata(),
                                                                        buildUri( baseUri,
                                                                                  linkedRepoMeta.name(),
                                                                                  linkedId ) );
            URI selfUri = addSelfLink( baseUri, entityDto, linkedRepoMeta.name(), linkedId );

            HttpHeaders headers = new HttpHeaders();
            headers.add( "Content-Location", selfUri.toString() );
            return negotiateResponse( request, HttpStatus.OK, headers, entityDto );
          }
        }
      }
    }

    return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
  }

  @SuppressWarnings({"unchecked"})
  @RequestMapping(
      value = "/{repository}/{id}/{property}/{linkedId}",
      method = {
          RequestMethod.DELETE
      }
  )
  @ResponseBody
  public ResponseEntity<?> deleteLink( ServletServerHttpRequest request,
                                       @PathVariable String repository,
                                       @PathVariable String id,
                                       @PathVariable String property,
                                       @PathVariable String linkedId ) throws IOException {
    RepositoryMetadata repoMeta = repositoryMetadataFor( repository );
    Serializable serId = stringToSerializable( id,
                                               (Class<? extends Serializable>) repoMeta.entityMetadata()
                                                   .idAttribute()
                                                   .type() );
    CrudRepository repo = repoMeta.repository();
    Object entity = repo.findOne( serId );
    if ( null == entity ) {
      return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
    } else {
      AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute( property );
      if ( null != attrMeta ) {
        // Find linked entity
        RepositoryMetadata linkedRepoMeta = repositoryMetadataFor( attrMeta );
        if ( null != linkedRepoMeta ) {
          CrudRepository linkedRepo = linkedRepoMeta.repository();
          Serializable sChildId = stringToSerializable( linkedId,
                                                        (Class<? extends Serializable>) linkedRepoMeta.entityMetadata()
                                                            .idAttribute()
                                                            .type() );
          Object linkedEntity = linkedRepo.findOne( sChildId );
          if ( null != linkedEntity ) {
            // Remove linked entity from relationship based on property type
            if ( attrMeta.isCollectionLike() ) {
              Collection c = attrMeta.asCollection( entity );
              if ( null != c ) {
                c.remove( linkedEntity );
              }
            } else if ( attrMeta.isSetLike() ) {
              Set s = attrMeta.asSet( entity );
              if ( null != s ) {
                s.remove( linkedEntity );
              }
            } else if ( attrMeta.isMapLike() ) {
              Object keyToRemove = null;
              Map<Object, Object> m = attrMeta.asMap( entity );
              if ( null != m ) {
                for ( Map.Entry<Object, Object> entry : m.entrySet() ) {
                  Object val = entry.getValue();
                  if ( null != val && val.equals( linkedEntity ) ) {
                    keyToRemove = entry.getKey();
                    break;
                  }
                }
                if ( null != keyToRemove ) {
                  m.remove( keyToRemove );
                }
              }
            } else {
              attrMeta.set( linkedEntity, entity );
            }

            return negotiateResponse( request, HttpStatus.NO_CONTENT, EMPTY_HEADERS, null );
          }
        }
      }
    }

    return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
  }

  @ExceptionHandler(RepositoryNotFoundException.class)
  @ResponseBody
  public ResponseEntity handleRepositoryNotFoundFailure( RepositoryNotFoundException e,
                                                         ServletServerHttpRequest request )
      throws IOException {
    if ( LOG.isWarnEnabled() ) {
      LOG.warn( "RepositoryNotFoundException: " + e.getMessage() );
    }
    return negotiateResponse( request, HttpStatus.NOT_FOUND, EMPTY_HEADERS, null );
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(OptimisticLockingFailureException.class)
  @ResponseBody
  public ResponseEntity handleLockingFailure( OptimisticLockingFailureException ex,
                                              ServletServerHttpRequest request )
      throws IOException {
    LOG.error( ex.getMessage(), ex );
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType( MediaType.APPLICATION_JSON );
    Map m = new HashMap();
    m.put( "message", ex.getMessage() );
    return negotiateResponse( request, HttpStatus.CONFLICT, headers, objectMapper.writeValueAsBytes( m ) );
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(RepositoryConstraintViolationException.class)
  @ResponseBody
  public ResponseEntity handleValidationFailure( RepositoryConstraintViolationException ex,
                                                 ServletServerHttpRequest request )
      throws IOException {
    LOG.error( ex.getMessage(), ex );

    Map m = new HashMap();
    List<String> errors = new ArrayList<String>();
    for ( FieldError fe : ex.getErrors().getFieldErrors() ) {
      errors.add( fe.getDefaultMessage() );
    }
    m.put( "errors", errors );

    return negotiateResponse( request, HttpStatus.BAD_REQUEST, EMPTY_HEADERS, m );
  }

  @SuppressWarnings({"unchecked"})
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ResponseEntity handleMessageConversionFailure( HttpMessageNotReadableException ex,
                                                        ServletServerHttpRequest request )
      throws IOException {
    LOG.error( ex.getMessage(), ex );
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType( MediaType.APPLICATION_JSON );
    Map m = new HashMap();
    m.put( "message", ex.getMessage() );
    return negotiateResponse( request, HttpStatus.BAD_REQUEST, headers, objectMapper.writeValueAsBytes( m ) );
  }

  /*
  -----------------------------------
    Internal helper methods
  -----------------------------------
   */
  private static URI buildUri( URI baseUri, String... pathSegments ) {
    return UriComponentsBuilder.fromUri( baseUri ).pathSegment( pathSegments ).build().toUri();
  }

  @SuppressWarnings({"unchecked"})
  private URI addSelfLink( URI baseUri, Map<String, Object> model, String... pathComponents ) {
    List<Link> links = (List<Link>) model.get( LINKS );
    if ( null == links ) {
      links = new ArrayList<Link>();
      model.put( LINKS, links );
    }
    URI selfUri = buildUri( baseUri, pathComponents );
    links.add( new SimpleLink( SELF, selfUri ) );
    return selfUri;
  }

  @SuppressWarnings({"unchecked"})
  private void maybeAddPrevNextLink( URI resourceUri,
                                     RepositoryMetadata repoMeta,
                                     PagingAndSorting pageSort,
                                     Page page,
                                     boolean addIf,
                                     int nextPage,
                                     String rel,
                                     Links links ) {
    if ( null != page && addIf ) {
      UriComponentsBuilder urib = UriComponentsBuilder.fromUri( resourceUri );
      urib.queryParam( config.getPageParamName(), nextPage ); // PageRequest is 0-based, so it's already (page - 1)
      urib.queryParam( config.getLimitParamName(), page.getSize() );
      pageSort.addSortParameters( urib );
      links.add( new SimpleLink( repoMeta.rel() + "." + rel, urib.build().toUri() ) );
    }
  }

  @SuppressWarnings({"unchecked"})
  private <V extends Serializable> V stringToSerializable( String s, Class<V> targetType ) {
    if ( ClassUtils.isAssignable( targetType, String.class ) ) {
      return (V) s;
    } else {
      return conversionService.convert( s, targetType );
    }
  }

  @SuppressWarnings({"unchecked"})
  private Object resolveTopLevelResource( URI baseUri, String uri ) {
    URI href = URI.create( uri );

    URI relativeUri = baseUri.relativize( href );
    Stack<URI> uris = UriUtils.explode( baseUri, relativeUri );

    if ( uris.size() > 1 ) {
      String repoName = UriUtils.path( uris.get( 0 ) );
      String sId = UriUtils.path( uris.get( 1 ) );

      RepositoryMetadata repoMeta = repositoryMetadataFor( repoName );
      CrudRepository repo = repoMeta.repository();
      if ( null == repo ) {
        return null;
      }
      EntityMetadata entityMeta = repoMeta.entityMetadata();
      if ( null == entityMeta ) {
        return null;
      }
      Class<? extends Serializable> idType = (Class<? extends Serializable>) entityMeta.idAttribute().type();

      Serializable serId = stringToSerializable( sId, idType );

      return repo.findOne( serId );
    }

    return null;
  }

  @SuppressWarnings({"unchecked"})
  private <V> V readIncoming( HttpInputMessage request, MediaType incomingMediaType, Class<V> targetType ) throws IOException {
    for ( HttpMessageConverter converter : httpMessageConverters ) {
      if ( converter.canRead( targetType, incomingMediaType ) ) {
        return (V) converter.read( targetType, request );
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  private Map<String, Object> extractPropertiesLinkAware( String repoRel,
                                                          Object entity,
                                                          EntityMetadata<AttributeMetadata> entityMetadata,
                                                          URI baseUri ) {
    final Map<String, Object> entityDto = new HashMap<String, Object>();

    for ( Map.Entry<String, AttributeMetadata> attrMeta : entityMetadata.embeddedAttributes().entrySet() ) {
      String name = attrMeta.getKey();
      Object val = attrMeta.getValue().get( entity );
      if ( null != val ) {
        entityDto.put( name, val );
      }
    }

    for ( String attrName : entityMetadata.linkedAttributes().keySet() ) {
      URI uri = buildUri( baseUri, attrName );
      Link l = new SimpleLink( repoRel + "." + entity.getClass().getSimpleName() + "." + attrName, uri );
      List<Link> links = (List<Link>) entityDto.get( LINKS );
      if ( null == links ) {
        links = new ArrayList<Link>();
        entityDto.put( LINKS, links );
      }
      links.add( l );
    }

    return entityDto;
  }

  private String viewName( String name ) {
    return "org.springframework.data.rest." + name;
  }

  private boolean shouldReturnLinks( String acceptHeader ) {
    if ( null != acceptHeader ) {
      List<MediaType> accept = MediaType.parseMediaTypes( acceptHeader );
      for ( MediaType mt : accept ) {
        if ( mt.getSubtype().startsWith( "x-spring-data-verbose" ) ) {
          return false;
        } else if ( mt.getSubtype().startsWith( "x-spring-data-compact" ) ) {
          return true;
        } else if ( mt.getSubtype().equals( "uri-list" ) ) {
          return true;
        }
      }
    }
    return false;
  }

  private ResponseEntity<?> noConverterFoundError( Class<?> fromResponseType ) {
    return new ResponseEntity<String>(
        String.format( "{\"message\": \"No converter found for class <%s>\"}", fromResponseType ),
        HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  @SuppressWarnings({"unchecked"})
  private ResponseEntity<byte[]> negotiateResponse( final ServletServerHttpRequest request,
                                                    final HttpStatus status,
                                                    final HttpHeaders headers,
                                                    final Object resource ) throws IOException {

    String jsonpParam = request.getServletRequest().getParameter( config.getJsonpParamName() );
    String jsonpOnErrParam = null;
    if ( null != config.getJsonpOnErrParamName() ) {
      jsonpOnErrParam = request.getServletRequest().getParameter( config.getJsonpOnErrParamName() );
    }

    HttpStatus responseStatus = status;
    byte[] responseBody = null;
    if ( null != resource ) {
      List<MediaType> acceptableTypes = new ArrayList<MediaType>();

      if ( !request.getHeaders().getAccept().isEmpty() &&
          !Arrays.equals(
              request.getHeaders().getAccept().toArray(),
              ALL_TYPES.toArray()
          ) ) {
        acceptableTypes.addAll( request.getHeaders().getAccept() );
      } else {
        acceptableTypes.add( MediaType.APPLICATION_JSON );
      }

      for ( MediaType acceptType : acceptableTypes ) {
        HttpMessageConverter converterToUse = null;
        for ( HttpMessageConverter conv : config.getCustomConverters() ) {
          if ( conv.canWrite( resource.getClass(), acceptType ) ) {
            converterToUse = conv;
            break;
          }
        }
        if ( null == converterToUse ) {
          for ( HttpMessageConverter conv : httpMessageConverters ) {
            if ( conv.canWrite( resource.getClass(), acceptType ) ) {
              converterToUse = conv;
              break;
            }
          }
        }

        if ( null != converterToUse ) {
          final ByteArrayOutputStream bout = new ByteArrayOutputStream();
          converterToUse.write( resource, acceptType, new HttpOutputMessage() {
            @Override public OutputStream getBody() throws IOException {
              return bout;
            }

            @Override public HttpHeaders getHeaders() {
              return headers;
            }
          } );

          if ( null != jsonpParam || null != jsonpOnErrParam ) {
            headers.setContentType( JacksonUtil.APPLICATION_JAVASCRIPT );
          }
          responseBody = bout.toByteArray();
        } else {
          responseStatus = HttpStatus.NOT_ACCEPTABLE;
          headers.setContentType( MediaType.TEXT_PLAIN );
          StringBuilder sb = new StringBuilder();
          if ( null != jsonpOnErrParam ) {
            sb.append( "\"" );
          }
          for ( MediaType mt : availableMediaTypes ) {
            sb.append( mt.toString() ).append( '\n' );
          }
          if ( null != jsonpOnErrParam ) {
            sb.append( "\"" );
          }
          responseBody = sb.toString().getBytes();
        }
      }
    }

    if ( responseStatus.value() > 400 && (null != jsonpOnErrParam) ) {
      String jsonp = jsonpOnErrParam + "(" + responseStatus.value() + "," + (null == responseBody ? "null" : new String(
          responseBody )) + ")";
      responseBody = jsonp.getBytes();
      responseStatus = HttpStatus.OK;
    } else if ( null != jsonpParam ) {
      String jsonp = jsonpParam + "(" + (null == responseBody ? "null" : new String( responseBody )) + ")";
      responseBody = jsonp.getBytes();
    }

    if ( null == responseBody ) {
      headers.setContentLength( 0 );
    } else {
      headers.setContentLength( responseBody.length );
    }


    return new ResponseEntity<byte[]>( responseBody, headers, responseStatus );
  }

}
