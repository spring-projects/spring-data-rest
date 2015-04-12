/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.springframework.http.HttpMethod.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.AfterDeleteEvent;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeDeleteEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.core.mapping.SupportedHttpMethods;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.data.rest.webmvc.support.ETagDoesntMatchException;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Jeremy Rickard
 */
@RepositoryRestController
class RepositoryEntityController extends AbstractRepositoryRestController implements ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}";
	private static final List<String> ACCEPT_PATCH_HEADERS = Arrays.asList(//
			RestMediaTypes.MERGE_PATCH_JSON.toString(), //
			RestMediaTypes.JSON_PATCH_JSON.toString(), //
			MediaType.APPLICATION_JSON_VALUE);

	private static final String ACCEPT_HEADER = "Accept";

	private final RepositoryEntityLinks entityLinks;
	private final RepositoryRestConfiguration config;
	private final ConversionService conversionService;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RepositoryEntityController(Repositories repositories, RepositoryRestConfiguration config,
			RepositoryEntityLinks entityLinks, PagedResourcesAssembler<Object> assembler,
			@Qualifier("defaultConversionService") ConversionService conversionService,
			AuditableBeanWrapperFactory auditableBeanWrapperFactory) {

		super(assembler, auditableBeanWrapperFactory);

		this.entityLinks = entityLinks;
		this.config = config;
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	/**
	 * <code>OPTIONS /{repository}</code>.
	 * 
	 * @param information
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.OPTIONS)
	public ResponseEntity<?> optionsForCollectionResource(RootResourceInformation information) {

		HttpHeaders headers = new HttpHeaders();
		SupportedHttpMethods supportedMethods = information.getSupportedMethods();

		headers.setAllow(supportedMethods.getMethodsFor(ResourceType.COLLECTION));

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * <code>HEAD /{repository}</code>
	 * 
	 * @param resourceInformation
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.HEAD)
	public ResponseEntity<?> headCollectionResource(RootResourceInformation resourceInformation)
			throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.HEAD, ResourceType.COLLECTION);

		RepositoryInvoker invoker = resourceInformation.getInvoker();

		if (null == invoker) {
			throw new ResourceNotFoundException();
		}

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	/**
	 * <code>GET /{repository}</code> - Returns the collection resource (paged or unpaged).
	 * 
	 * @param resourceInformation
	 * @param pageable
	 * @param sort
	 * @param assembler
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public Resources<?> getCollectionResource(final RootResourceInformation resourceInformation,
			DefaultedPageable pageable, Sort sort, PersistentEntityResourceAssembler assembler)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.COLLECTION);

		RepositoryInvoker invoker = resourceInformation.getInvoker();

		if (null == invoker) {
			throw new ResourceNotFoundException();
		}

		Iterable<?> results;

		if (pageable.getPageable() != null) {
			results = invoker.invokeFindAll(pageable.getPageable());
		} else {
			results = invoker.invokeFindAll(sort);
		}

		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();
		List<Link> links = new ArrayList<Link>();

		if (searchMappings.isExported()) {
			links.add(entityLinks.linkFor(metadata.getDomainType()).slash(searchMappings.getPath())
					.withRel(searchMappings.getRel()));
		}

		Link baseLink = entityLinks.linkToPagedResource(resourceInformation.getDomainType(), pageable.isDefault() ? null
				: pageable.getPageable());

		Resources<?> result = toResources(results, assembler, baseLink);
		result.add(links);
		return result;
	}

	@ResponseBody
	@SuppressWarnings({ "unchecked" })
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = {
			"application/x-spring-data-compact+json", "text/uri-list" })
	public Resources<?> getCollectionResourceCompact(RootResourceInformation repoRequest, DefaultedPageable pageable,
			Sort sort, PersistentEntityResourceAssembler assembler) throws ResourceNotFoundException,
			HttpRequestMethodNotSupportedException {

		Resources<?> resources = getCollectionResource(repoRequest, pageable, sort, assembler);
		List<Link> links = new ArrayList<Link>(resources.getLinks());

		for (Resource<?> resource : ((Resources<Resource<?>>) resources).getContent()) {
			PersistentEntityResource persistentEntityResource = (PersistentEntityResource) resource;
			links.add(resourceLink(repoRequest, persistentEntityResource));
		}
		if (resources instanceof PagedResources) {
			return new PagedResources<Object>(Collections.emptyList(), ((PagedResources<?>) resources).getMetadata(), links);
		} else {
			return new Resources<Object>(Collections.emptyList(), links);
		}
	}

	/**
	 * <code>POST /{repository}</code> - Creates a new entity instances from the collection resource.
	 * 
	 * @param resourceInformation
	 * @param payload
	 * @param assembler
	 * @param acceptHeader
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST)
	public ResponseEntity<ResourceSupport> postCollectionResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, PersistentEntityResourceAssembler assembler, @RequestHeader(
					value = ACCEPT_HEADER, required = false) String acceptHeader) throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.POST, ResourceType.COLLECTION);

		return createAndReturn(payload.getContent(), resourceInformation.getInvoker(), assembler,
				config.returnBodyOnCreate(acceptHeader));
	}

	/**
	 * <code>OPTIONS /{repository}/{id}<code>
	 * 
	 * @param information
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.OPTIONS)
	public ResponseEntity<?> optionsForItemResource(RootResourceInformation information) {

		HttpHeaders headers = new HttpHeaders();
		SupportedHttpMethods supportedMethods = information.getSupportedMethods();

		headers.setAllow(supportedMethods.getMethodsFor(ResourceType.ITEM));
		headers.put("Accept-Patch", ACCEPT_PATCH_HEADERS);

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * <code>HEAD /{repsoitory}/{id}</code>
	 * 
	 * @param resourceInformation
	 * @param id
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.HEAD)
	public ResponseEntity<?> headForItemResource(RootResourceInformation resourceInformation, @BackendId Serializable id,
			PersistentEntityResourceAssembler assembler) throws HttpRequestMethodNotSupportedException {

		Object domainObject = getItemResource(resourceInformation, id);

		if (domainObject == null) {
			throw new ResourceNotFoundException();
		}

		PersistentEntityResource resource = assembler.toResource(domainObject);

		return new ResponseEntity<Object>(prepareHeaders(resource), HttpStatus.NO_CONTENT);
	}

	/**
	 * <code>GET /{repository}/{id}</code> - Returns a single entity.
	 * 
	 * @param resourceInformation
	 * @param id
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Resource<?>> getItemResource(RootResourceInformation resourceInformation,
			@BackendId Serializable id, PersistentEntityResourceAssembler assembler,
			@RequestHeader MultiValueMap<String, String> rawHeaders) throws HttpRequestMethodNotSupportedException {

		Object domainObj = getItemResource(resourceInformation, id);

		if (domainObj == null) {
			return new ResponseEntity<Resource<?>>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.putAll(rawHeaders);

		// Check ETag for If-Non-Match

		List<String> ifNoneMatch = headers.getIfNoneMatch();
		ETag eTag = ifNoneMatch.isEmpty() ? ETag.NO_ETAG : ETag.from(ifNoneMatch.get(0));
		PersistentEntity<?, ?> entity = resourceInformation.getPersistentEntity();

		if (eTag.matches(entity, domainObj)) {
			return new ResponseEntity<Resource<?>>(prepareHeaders(entity, domainObj), HttpStatus.NOT_MODIFIED);
		}

		// Check last modification for If-Modfied-Since

		if (headers.getIfModifiedSince() != -1) {

			AuditableBeanWrapper wrapper = getAuditableBeanWrapper(domainObj);
			long current = wrapper.getLastModifiedDate().getTimeInMillis() / 1000 * 1000;

			if (current <= headers.getIfModifiedSince()) {
				return new ResponseEntity<Resource<?>>(prepareHeaders(entity, domainObj), HttpStatus.NOT_MODIFIED);
			}
		}

		PersistentEntityResource resource = assembler.toFullResource(domainObj);
		return new ResponseEntity<Resource<?>>(resource, prepareHeaders(resource), HttpStatus.OK);
	}

	/**
	 * <code>PUT /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
	 * 
	 * @param resourceInformation
	 * @param payload
	 * @param id
	 * @param assembler
	 * @param eTag
	 * @param acceptHeader
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PUT)
	public ResponseEntity<? extends ResourceSupport> putItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler,
			ETag eTag, @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
			throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.PUT, ResourceType.ITEM);

		// Force ID on unmarshalled object
		PersistentPropertyAccessor incomingWrapper = new ConvertingPropertyAccessor(payload.getPropertyAccessor(),
				conversionService);
		incomingWrapper.setProperty(payload.getPersistentEntity().getIdProperty(), id);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Object objectToSave = incomingWrapper.getBean();

		Object domainObject = invoker.invokeFindOne(id);

		eTag.verify(resourceInformation.getPersistentEntity(), domainObject);

		return domainObject == null ? createAndReturn(objectToSave, invoker, assembler,
				config.returnBodyOnCreate(acceptHeader)) : saveAndReturn(objectToSave, invoker, PUT, assembler,
				config.returnBodyOnUpdate(acceptHeader));
	}

	/**
	 * <code>PUT /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
	 *
	 * @param resourceInformation
	 * @param payload
	 * @param id
	 * @param assembler
	 * @param eTag,
	 * @param acceptHeader
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 * @throws ResourceNotFoundException
	 * @throws ETagDoesntMatchException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PATCH)
	public ResponseEntity<ResourceSupport> patchItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler,
			ETag eTag, @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		resourceInformation.verifySupportedMethod(HttpMethod.PATCH, ResourceType.ITEM);

		Object domainObject = resourceInformation.getInvoker().invokeFindOne(id);

		if (domainObject == null) {
			throw new ResourceNotFoundException();
		}

		eTag.verify(resourceInformation.getPersistentEntity(), domainObject);

		return saveAndReturn(payload.getContent(), resourceInformation.getInvoker(), PATCH, assembler,
				config.returnBodyOnUpdate(acceptHeader));
	}

	/**
	 * <code>DELETE /{repository}/{id}</code> - Deletes the entity backing the item resource.
	 *
	 * @param resourceInformation
	 * @param id
	 * @param eTag
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws HttpRequestMethodNotSupportedException
	 * @throws ETagDoesntMatchException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteItemResource(RootResourceInformation resourceInformation, @BackendId Serializable id,
			ETag eTag) throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.DELETE, ResourceType.ITEM);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Object domainObj = invoker.invokeFindOne(id);

		if (domainObj == null) {
			throw new ResourceNotFoundException();
		}

		eTag.verify(resourceInformation.getPersistentEntity(), domainObj);

		publisher.publishEvent(new BeforeDeleteEvent(domainObj));
		invoker.invokeDelete(id);
		publisher.publishEvent(new AfterDeleteEvent(domainObj));

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	/**
	 * Merges the given incoming object into the given domain object.
	 *
	 * @param domainObject
	 * @param invoker
	 * @param httpMethod
	 * @return
	 */
	private ResponseEntity<ResourceSupport> saveAndReturn(Object domainObject, RepositoryInvoker invoker,
			HttpMethod httpMethod, PersistentEntityResourceAssembler assembler, boolean returnBody) {

		publisher.publishEvent(new BeforeSaveEvent(domainObject));
		Object obj = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterSaveEvent(domainObject));

		PersistentEntityResource resource = assembler.toFullResource(obj);
		HttpHeaders headers = prepareHeaders(resource);

		if (PUT.equals(httpMethod)) {
			addLocationHeader(headers, assembler, obj);
		}

		if (returnBody) {
			return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, resource);
		} else {
			return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT, headers);
		}
	}

	/**
	 * Triggers the creation of the domain object and renders it into the response if needed.
	 * 
	 * @param domainObject
	 * @param invoker
	 * @return
	 */
	private ResponseEntity<ResourceSupport> createAndReturn(Object domainObject, RepositoryInvoker invoker,
			PersistentEntityResourceAssembler assembler, boolean returnBody) {

		publisher.publishEvent(new BeforeCreateEvent(domainObject));
		Object savedObject = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterCreateEvent(savedObject));

		PersistentEntityResource resource = returnBody ? assembler.toFullResource(savedObject) : null;

		HttpHeaders headers = prepareHeaders(resource);
		addLocationHeader(headers, assembler, savedObject);

		return ControllerUtils.toResponseEntity(HttpStatus.CREATED, headers, resource);
	}

	/**
	 * Sets the location header pointing to the resource representing the given instance. Will make sure we properly
	 * expand the URI template potentially created as self link.
	 * 
	 * @param headers must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 */
	private void addLocationHeader(HttpHeaders headers, PersistentEntityResourceAssembler assembler, Object source) {

		String selfLink = assembler.getSelfLinkFor(source).getHref();
		headers.setLocation(new UriTemplate(selfLink).expand());
	}

	/**
	 * Returns the object backing the item resource for the given {@link RootResourceInformation} and id.
	 * 
	 * @param resourceInformation
	 * @param id
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 * @throws {@link ResourceNotFoundException}
	 */
	private Object getItemResource(RootResourceInformation resourceInformation, Serializable id)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.ITEM);

		return resourceInformation.getInvoker().invokeFindOne(id);
	}
}
