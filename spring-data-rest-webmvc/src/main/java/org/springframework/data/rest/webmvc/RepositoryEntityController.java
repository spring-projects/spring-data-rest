/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.AfterDeleteEvent;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeDeleteEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RepositoryRestController
class RepositoryEntityController extends AbstractRepositoryRestController implements ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}";
	private static final List<String> ACCEPT_PATCH_HEADERS = Arrays.asList(//
			RestMediaTypes.MERGE_PATCH_JSON.toString(), //
			RestMediaTypes.JSON_PATCH_JSON.toString(), //
			MediaType.APPLICATION_JSON_VALUE);

	private final RepositoryEntityLinks entityLinks;
	private final RepositoryRestConfiguration config;
	private final ConversionService conversionService;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RepositoryEntityController(Repositories repositories, RepositoryRestConfiguration config,
			RepositoryEntityLinks entityLinks, PagedResourcesAssembler<Object> assembler,
			@Qualifier("defaultConversionService") ConversionService conversionService) {

		super(assembler);

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
		headers.setAllow(information.getSupportedMethods(ResourceType.COLLECTION));

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

		Resources<?> resources = resultToResources(results, assembler, baseLink);
		resources.add(links);
		return resources;
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
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST)
	public ResponseEntity<ResourceSupport> postCollectionResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, PersistentEntityResourceAssembler assembler)
			throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.POST, ResourceType.COLLECTION);

		return createAndReturn(payload.getContent(), resourceInformation.getInvoker(), assembler);
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
		headers.setAllow(information.getSupportedMethods(ResourceType.ITEM));
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
	public ResponseEntity<?> headForItemResource(RootResourceInformation resourceInformation, @BackendId Serializable id)
			throws HttpRequestMethodNotSupportedException {

		if (getItemResource(resourceInformation, id) != null) {
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		}

		throw new ResourceNotFoundException();
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
			@BackendId Serializable id, PersistentEntityResourceAssembler assembler)
			throws HttpRequestMethodNotSupportedException {

		Object domainObj = getItemResource(resourceInformation, id);

		if (domainObj == null) {
			return new ResponseEntity<Resource<?>>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Resource<?>>(assembler.toFullResource(domainObj), HttpStatus.OK);
	}

	/**
	 * <code>PUT /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
	 * 
	 * @param resourceInformation
	 * @param payload
	 * @param id
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PUT)
	public ResponseEntity<? extends ResourceSupport> putItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler)
			throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.PUT, ResourceType.ITEM);

		// Force ID on unmarshalled object
		BeanWrapper<Object> incomingWrapper = BeanWrapper.create(payload.getContent(), conversionService);
		incomingWrapper.setProperty(payload.getPersistentEntity().getIdProperty(), id);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Object objectToSave = incomingWrapper.getBean();

		return invoker.invokeFindOne(id) == null ? createAndReturn(objectToSave, invoker, assembler) : saveAndReturn(
				objectToSave, invoker, PUT, assembler);
	}

	/**
	 * <code>PUT /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
	 * 
	 * @param resourceInformation
	 * @param payload
	 * @param id
	 * @return
	 * @throws HttpRequestMethodNotSupportedException
	 * @throws ResourceNotFoundException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PATCH)
	public ResponseEntity<ResourceSupport> patchItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		resourceInformation.verifySupportedMethod(HttpMethod.PATCH, ResourceType.ITEM);

		if (resourceInformation.getInvoker().invokeFindOne(id) == null) {
			throw new ResourceNotFoundException();
		}

		return saveAndReturn(payload.getContent(), resourceInformation.getInvoker(), PATCH, assembler);
	}

	/**
	 * <code>DELETE /{repository}/{id}</code> - Deletes the entity backing the item resource.
	 * 
	 * @param resourceInformation
	 * @param id
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteItemResource(RootResourceInformation resourceInformation, @BackendId Serializable id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.DELETE, ResourceType.ITEM);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Object domainObj = invoker.invokeFindOne(id);

		if (domainObj == null) {
			throw new ResourceNotFoundException();
		}

		publisher.publishEvent(new BeforeDeleteEvent(domainObj));
		invoker.invokeDelete(id);
		publisher.publishEvent(new AfterDeleteEvent(domainObj));

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	/**
	 * Merges the given incoming object into the given domain object.
	 * 
	 * @param incoming
	 * @param domainObject
	 * @param invoker
	 * @param httpMethod
	 * @return
	 */
	private ResponseEntity<ResourceSupport> saveAndReturn(Object domainObject, RepositoryInvoker invoker,
			HttpMethod httpMethod, PersistentEntityResourceAssembler assembler) {

		publisher.publishEvent(new BeforeSaveEvent(domainObject));
		Object obj = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterSaveEvent(domainObject));

		HttpHeaders headers = new HttpHeaders();

		if (PUT.equals(httpMethod)) {
			addLocationHeader(headers, assembler, obj);
		}

		if (config.isReturnBodyOnUpdate()) {
			return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, assembler.toFullResource(obj));
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
			PersistentEntityResourceAssembler assembler) {

		publisher.publishEvent(new BeforeCreateEvent(domainObject));
		Object savedObject = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterCreateEvent(savedObject));

		HttpHeaders headers = new HttpHeaders();
		addLocationHeader(headers, assembler, savedObject);

		PersistentEntityResource resource = config.isReturnBodyOnCreate() ? assembler.toFullResource(savedObject) : null;
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

		RepositoryInvoker repoMethodInvoker = resourceInformation.getInvoker();

		if (!repoMethodInvoker.exposesFindOne()) {
			throw new ResourceNotFoundException();
		}

		return repoMethodInvoker.invokeFindOne(id);
	}
}
