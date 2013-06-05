package org.springframework.data.rest.webmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.BaseUriAwareResource;
import org.springframework.data.rest.repository.PagingAndSorting;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.invoke.MethodParameterConversionService;
import org.springframework.data.rest.repository.support.ResourceMappingUtils;
import org.springframework.data.rest.webmvc.support.BaseUriLinkBuilder;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage;
import org.springframework.data.rest.webmvc.support.ValidationExceptionHandler;
import org.springframework.hateoas.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.springframework.data.rest.core.util.UriUtils.buildUri;

/**
 * @author Jon Brisbin
 */
@SuppressWarnings({"rawtypes"})
public class AbstractRepositoryRestController implements ApplicationContextAware,
																												 InitializingBean {

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
	protected final EntityLinks                      entityLinks;
	protected       ApplicationContext               applicationContext;
	@Autowired(required = false)
	protected       ValidationExceptionHandler       handler;
	@Autowired(required = false)
	protected       PlatformTransactionManager       txMgr;
	protected       TransactionTemplate              txTmpl;

	@Autowired
	public AbstractRepositoryRestController(Repositories repositories,
																					RepositoryRestConfiguration config,
																					DomainClassConverter domainClassConverter,
																					ConversionService conversionService,
																					EntityLinks entityLinks) {
		this.repositories = repositories;
		this.config = config;
		this.domainClassConverter = domainClassConverter;
		this.conversionService = conversionService;
		this.entityLinks = entityLinks;
		this.methodParameterConversionService = new MethodParameterConversionService(conversionService);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (null != txMgr) {
			txTmpl = new TransactionTemplate(txMgr);
			txTmpl.afterPropertiesSet();
		}
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
	 * @return
	 */
	@ExceptionHandler({
												InvocationTargetException.class,
												IllegalArgumentException.class,
												ClassCastException.class,
												ConversionFailedException.class
										})
	@ResponseBody
	public ResponseEntity handleMiscFailures(Throwable t) {
		if (null != t.getCause() && t.getCause() instanceof ResourceNotFoundException) {
			return notFound();
		}
		return badRequest(t);
	}

	//	@ExceptionHandler({
	//			                  RuntimeException.class
	//	                  })
	//	@ResponseBody
	//	public ResponseEntity maybeHandleValidationException(Locale locale,
	//	                                                     RuntimeException ex) {
	//		if(ResourceNotFoundException.class.isAssignableFrom(ex.getClass())) {
	//			return handleNotFound();
	//		}
	//
	//		if(null != handler) {
	//			return handler.handleValidationException(ex,
	//			                                         applicationContext,
	//			                                         locale);
	//		} else {
	//			return response(null,
	//			                ex,
	//			                HttpStatus.BAD_REQUEST);
	//		}
	//	}

	@ExceptionHandler({
												RepositoryConstraintViolationException.class
										})
	@ResponseBody
	public ResponseEntity handleRepositoryConstraintViolationException(Locale locale,
																																		 RepositoryConstraintViolationException rcve) {
		return response(null,
										new RepositoryConstraintViolationExceptionMessage(rcve, applicationContext, locale),
										HttpStatus.BAD_REQUEST);
	}

	/**
	 * Send a 409 Conflict in case of concurrent modification.
	 *
	 * @param ex
	 * @return
	 */
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

	public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(T throwable,
																																							HttpStatus status) {
		return errorResponse(null, throwable, status);
	}

	public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(HttpHeaders headers,
																																							T throwable,
																																							HttpStatus status) {
		if (null != throwable && null != throwable.getMessage()) {
			LOG.error(throwable.getMessage(), throwable);
			return response(headers, new ExceptionMessage(throwable), status);
		} else {
			return response(headers, null, status);
		}
	}

	public <T> ResponseEntity<T> response(HttpHeaders headers, T body, HttpStatus status) {
		HttpHeaders hdrs = new HttpHeaders();
		if (null != headers) {
			hdrs.putAll(headers);
		}
		return new ResponseEntity<T>(body, hdrs, status);
	}

	public <R extends Resource<?>> ResponseEntity<Resource<?>> resourceResponse(HttpHeaders headers,
																																							R resource,
																																							HttpStatus status) {
		HttpHeaders hdrs = new HttpHeaders();
		if (null != headers) {
			hdrs.putAll(headers);
		}
		return new ResponseEntity<Resource<?>>(resource, hdrs, status);
	}

	protected void addQueryParameters(HttpServletRequest request,
																		UriComponentsBuilder builder) {
		for (Enumeration<String> names = request.getParameterNames(); names.hasMoreElements(); ) {
			String name = names.nextElement();
			String value = request.getParameter(name);
			if (name.equals(config.getPageParamName()) || name.equals(config.getLimitParamName())) {
				continue;
			}

			builder.queryParam(name, value);
		}
	}

	protected Link searchLink(RepositoryRestRequest repoRequest,
														int pageIncrement,
														String method,
														String rel) {
		PagingAndSorting pageSort = repoRequest.getPagingAndSorting();
		UriComponentsBuilder ucb = UriComponentsBuilder.fromUri(
				entityLinks.linkFor(repoRequest.getPersistentEntity().getType())
									 .slash("search")
									 .slash(method)
									 .toUri()
		);
		ucb.queryParam(config.getPageParamName(), Math.max(pageSort.getPageNumber() + pageIncrement, 1))
			 .queryParam(config.getLimitParamName(), pageSort.getPageSize());

		addQueryParameters(repoRequest.getRequest(), ucb);

		return new Link(ucb.build().toString(), rel);
	}

	protected Link entitiesPageLink(RepositoryRestRequest repoRequest,
																	int pageIncrement,
																	String rel) {
		PagingAndSorting pageSort = repoRequest.getPagingAndSorting();
		UriComponentsBuilder ucb = UriComponentsBuilder.fromUri(
				entityLinks.linkFor(repoRequest.getPersistentEntity().getType())
									 .toUri()
		);
		if (null != repoRequest.getRequest().getParameter(config.getPageParamName())) {
			ucb.queryParam(config.getPageParamName(), Math.max(pageSort.getPageNumber() + pageIncrement, 1))
				 .queryParam(config.getLimitParamName(), pageSort.getPageSize());
		}

		addQueryParameters(repoRequest.getRequest(), ucb);

		return new Link(ucb.build().toString(), rel);
	}

	protected List<Link> queryMethodLinks(URI baseUri, Class<?> domainType) {
		List<Link> links = new ArrayList<Link>();
		RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(domainType);
		ResourceMapping repoMapping = ResourceMappingUtils.merge(
				repoInfo.getRepositoryInterface(),
				config.getResourceMappingForRepository(repoInfo.getRepositoryInterface())
		);
		for (Method method : repoInfo.getQueryMethods()) {
			LinkBuilder linkBuilder = BaseUriLinkBuilder.create(buildUri(baseUri, repoMapping.getPath(), "search"));
			ResourceMapping methodMapping = ResourceMappingUtils.merge(method,
																																 repoMapping.getResourceMappingFor(method.getName()));
			if (!methodMapping.isExported()) {
				continue;
			}
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

	@SuppressWarnings({"unchecked"})
	protected Resources resultToResources(RepositoryRestRequest repoRequest,
																				Object result,
																				List<Link> links,
																				Link prevLink,
																				Link nextLink) {
		if (result instanceof Page) {
			Page page = (Page) result;
			PagedResources.PageMetadata pageMeta = pageMetadata(page);
			if (page.hasPreviousPage() && null != prevLink) {
				links.add(prevLink);
			}
			if (page.hasNextPage() && null != nextLink) {
				links.add(nextLink);
			}
			if (page.hasContent()) {
				return entitiesToResources(repoRequest, links, page);
			} else {
				return new PagedResources(Collections.emptyList(), pageMeta, links);
			}
		} else if (result instanceof Iterable) {
			return entitiesToResources(repoRequest, links, (Iterable) result);
		} else if (null == result) {
			return new Resources(EMPTY_RESOURCE_LIST);
		} else {
			PersistentEntityResource per = PersistentEntityResource.wrap(
					repoRequest.getPersistentEntity(),
					result,
					repoRequest.getBaseUri()
			);
			BeanWrapper wrapper = BeanWrapper.create(result, conversionService);
			Link selfLink = entityLinks.linkForSingleResource(
					result.getClass(),
					wrapper.getProperty(repoRequest.getPersistentEntity().getIdProperty())
			)
																 .withSelfRel();
			per.add(selfLink);
			return new Resources(Collections.singletonList(per), links);
		}
	}

	@SuppressWarnings({"unchecked"})
	protected Resources entitiesToResources(RepositoryRestRequest repoRequest, List<Link> links, Page page) {
		PagedResources.PageMetadata pageMeta = pageMetadata(page);
		Resources<Object> resource = (Resources<Object>) entitiesToResources(repoRequest, links, page.getContent());
		return new PagedResources<Object>(resource.getContent(), pageMeta, resource.getLinks());
	}

	@SuppressWarnings({"unchecked"})
	protected Resources entitiesToResources(RepositoryRestRequest repoRequest, List<Link> links, Iterable entities) {
		List<Resource<?>> resources = new ArrayList<Resource<?>>();
		for (Object obj : entities) {
			if (null == obj) {
				resources.add(null);
				continue;
			}

			PersistentEntity persistentEntity = repositories.getPersistentEntity(obj.getClass());
			if (null == persistentEntity) {
				resources.add(new BaseUriAwareResource<Object>(obj)
													.setBaseUri(repoRequest.getBaseUri()));
				continue;
			}

			BeanWrapper wrapper = BeanWrapper.create(obj, conversionService);
			PersistentEntityResource per = PersistentEntityResource.wrap(persistentEntity, obj, repoRequest.getBaseUri());
			Link selfLink = entityLinks.linkForSingleResource(persistentEntity.getType(),
																												wrapper.getProperty(persistentEntity.getIdProperty()))
																 .withSelfRel();
			per.add(selfLink);
			resources.add(per);
		}

		return new Resources(resources, links);
	}

	protected PagedResources.PageMetadata pageMetadata(Page page) {
		return new PagedResources.PageMetadata(
				page.getNumberOfElements(),
				page.getNumber() + 1,
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

}
