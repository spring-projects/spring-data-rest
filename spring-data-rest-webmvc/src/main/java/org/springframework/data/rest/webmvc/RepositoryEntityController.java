/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
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
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * @author Jeroen Reijn
 */
@RepositoryRestController
class RepositoryEntityController extends AbstractRepositoryRestController implements ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}";
	private static final List<String> ACCEPT_PATCH_HEADERS = Arrays.asList(//
			RestMediaTypes.MERGE_PATCH_JSON.toString(), //
			RestMediaTypes.JSON_PATCH_JSON.toString(), //
			MediaType.APPLICATION_JSON_VALUE);

	private static final String ACCEPT_HEADER = "Accept";
	private static final String LINK_HEADER = "Link";

	private final RepositoryEntityLinks entityLinks;
	private final RepositoryRestConfiguration config;
	private final HttpHeadersPreparer headersPreparer;
	private final ResourceStatus resourceStatus;

	private ApplicationEventPublisher publisher;

	/**
	 * Creates a new {@link RepositoryEntityController} for the given {@link Repositories},
	 * {@link RepositoryRestConfiguration}, {@link RepositoryEntityLinks}, {@link PagedResourcesAssembler},
	 * {@link ConversionService} and {@link AuditableBeanWrapperFactory}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param auditableBeanWrapperFactory must not be {@literal null}.
	 */
	@Autowired
	public RepositoryEntityController(Repositories repositories, RepositoryRestConfiguration config,
			RepositoryEntityLinks entityLinks, PagedResourcesAssembler<Object> assembler,
			HttpHeadersPreparer headersPreparer) {

		super(assembler);

		this.entityLinks = entityLinks;
		this.config = config;
		this.headersPreparer = headersPreparer;
		this.resourceStatus = ResourceStatus.of(headersPreparer);
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

		headers.setAllow(supportedMethods.getMethodsFor(ResourceType.COLLECTION).toSet());

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
	public ResponseEntity<?> headCollectionResource(RootResourceInformation resourceInformation,
			DefaultedPageable pageable) throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.HEAD, ResourceType.COLLECTION);

		RepositoryInvoker invoker = resourceInformation.getInvoker();

		if (null == invoker) {
			throw new ResourceNotFoundException();
		}

		Links links = Links.of(getDefaultSelfLink()) //
				.and(getCollectionResourceLinks(resourceInformation, pageable));

		HttpHeaders headers = new HttpHeaders();
		headers.add(LINK_HEADER, links.toString());

		return new ResponseEntity<Object>(headers, HttpStatus.NO_CONTENT);
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
	public CollectionModel<?> getCollectionResource(@QuerydslPredicate RootResourceInformation resourceInformation,
			DefaultedPageable pageable, Sort sort, PersistentEntityResourceAssembler assembler)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.COLLECTION);

		RepositoryInvoker invoker = resourceInformation.getInvoker();

		if (null == invoker) {
			throw new ResourceNotFoundException();
		}

		Iterable<?> results = pageable.getPageable() != null //
				? invoker.invokeFindAll(pageable.getPageable()) //
				: invoker.invokeFindAll(sort);

		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		Optional<Link> baseLink = Optional.of(getDefaultSelfLink());

		return toCollectionModel(results, assembler, metadata.getDomainType(), baseLink)
				.add(getCollectionResourceLinks(resourceInformation, pageable));
	}

	private Links getCollectionResourceLinks(RootResourceInformation resourceInformation, DefaultedPageable pageable) {

		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();

		Links links = Links
				.of(Link.of(ProfileController.getPath(this.config, metadata), ProfileResourceProcessor.PROFILE_REL));

		return searchMappings.isExported() //
				? links.and(entityLinks.linkFor(metadata.getDomainType()).slash(searchMappings.getPath())
						.withRel(searchMappings.getRel()))
				: links;
	}

	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET,
			produces = { "application/x-spring-data-compact+json", "text/uri-list" })
	public CollectionModel<?> getCollectionResourceCompact(@QuerydslPredicate RootResourceInformation resourceinformation,
			DefaultedPageable pageable, Sort sort, PersistentEntityResourceAssembler assembler)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		CollectionModel<?> resources = getCollectionResource(resourceinformation, pageable, sort, assembler);

		Links links = resources.getContent().stream() //
				.map(PersistentEntityResource.class::cast) //
				.map(it -> resourceLink(resourceinformation, it)) //
				.reduce(resources.getLinks(), Links::and, Links::and);

		CollectionModel<?> model = resources instanceof PagedModel //
				? PagedModel.empty(((PagedModel<?>) resources).getMetadata()) //
				: CollectionModel.empty();

		return model.add(links);
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
	public ResponseEntity<RepresentationModel<?>> postCollectionResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, PersistentEntityResourceAssembler assembler,
			@RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
			throws HttpRequestMethodNotSupportedException {

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

		headers.setAllow(supportedMethods.getMethodsFor(ResourceType.ITEM).toSet());
		headers.put("Accept-Patch", ACCEPT_PATCH_HEADERS);

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * <code>HEAD /{repository}/{id}</code>
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

		return getItemResource(resourceInformation, id).map(it -> {

			Links links = assembler.toModel(it).getLinks();

			HttpHeaders headers = headersPreparer.prepareHeaders(resourceInformation.getPersistentEntity(), it);
			headers.add(LINK_HEADER, links.toString());

			return new ResponseEntity<Object>(headers, HttpStatus.NO_CONTENT);

		}).orElseThrow(() -> new ResourceNotFoundException());
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
	public ResponseEntity<EntityModel<?>> getItemResource(RootResourceInformation resourceInformation,
			@BackendId Serializable id, final PersistentEntityResourceAssembler assembler, @RequestHeader HttpHeaders headers)
			throws HttpRequestMethodNotSupportedException {

		return getItemResource(resourceInformation, id).map(it -> {

			PersistentEntity<?, ?> entity = resourceInformation.getPersistentEntity();

			return resourceStatus.getStatusAndHeaders(headers, it, entity).toResponseEntity(//
					() -> assembler.toFullResource(it));

		}).orElseThrow(() -> new ResourceNotFoundException());
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
	public ResponseEntity<? extends RepresentationModel<?>> putItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler,
			ETag eTag, @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
			throws HttpRequestMethodNotSupportedException {

		resourceInformation.verifySupportedMethod(HttpMethod.PUT, ResourceType.ITEM);

		if (payload.isNew()) {
			resourceInformation.verifyPutForCreation();
		}

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Object objectToSave = payload.getContent();
		eTag.verify(resourceInformation.getPersistentEntity(), objectToSave);

		return payload.isNew() ? createAndReturn(objectToSave, invoker, assembler, config.returnBodyOnCreate(acceptHeader))
				: saveAndReturn(objectToSave, invoker, PUT, assembler, config.returnBodyOnUpdate(acceptHeader));
	}

	/**
	 * <code>PATCH /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
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
	public ResponseEntity<RepresentationModel<?>> patchItemResource(RootResourceInformation resourceInformation,
			PersistentEntityResource payload, @BackendId Serializable id, PersistentEntityResourceAssembler assembler,
			ETag eTag, @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		resourceInformation.verifySupportedMethod(HttpMethod.PATCH, ResourceType.ITEM);

		Object domainObject = payload.getContent();

		eTag.verify(resourceInformation.getPersistentEntity(), domainObject);

		return saveAndReturn(domainObject, resourceInformation.getInvoker(), PATCH, assembler,
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
		Optional<Object> domainObj = invoker.invokeFindById(id);

		return domainObj.map(it -> {

			PersistentEntity<?, ?> entity = resourceInformation.getPersistentEntity();

			eTag.verify(entity, it);

			publisher.publishEvent(new BeforeDeleteEvent(it));
			invoker.invokeDeleteById(entity.getIdentifierAccessor(it).getIdentifier());
			publisher.publishEvent(new AfterDeleteEvent(it));

			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);

		}).orElseThrow(() -> new ResourceNotFoundException());
	}

	/**
	 * Merges the given incoming object into the given domain object.
	 *
	 * @param domainObject
	 * @param invoker
	 * @param httpMethod
	 * @return
	 */
	private ResponseEntity<RepresentationModel<?>> saveAndReturn(Object domainObject, RepositoryInvoker invoker,
			HttpMethod httpMethod, PersistentEntityResourceAssembler assembler, boolean returnBody) {

		publisher.publishEvent(new BeforeSaveEvent(domainObject));
		Object obj = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterSaveEvent(obj));

		PersistentEntityResource resource = assembler.toFullResource(obj);
		HttpHeaders headers = headersPreparer.prepareHeaders(Optional.of(resource));

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
	private ResponseEntity<RepresentationModel<?>> createAndReturn(Object domainObject, RepositoryInvoker invoker,
			PersistentEntityResourceAssembler assembler, boolean returnBody) {

		publisher.publishEvent(new BeforeCreateEvent(domainObject));
		Object savedObject = invoker.invokeSave(domainObject);
		publisher.publishEvent(new AfterCreateEvent(savedObject));

		Optional<PersistentEntityResource> resource = Optional
				.ofNullable(returnBody ? assembler.toFullResource(savedObject) : null);

		HttpHeaders headers = headersPreparer.prepareHeaders(resource);
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

		String selfLink = assembler.getExpandedSelfLink(source).getHref();
		headers.setLocation(UriTemplate.of(selfLink).expand());
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
	private Optional<Object> getItemResource(RootResourceInformation resourceInformation, Serializable id)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.ITEM);

		return resourceInformation.getInvoker().invokeFindById(id);
	}
}
