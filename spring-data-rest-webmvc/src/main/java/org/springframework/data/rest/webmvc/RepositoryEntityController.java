/*
 * Copyright 2013 the original author or authors.
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

import static org.springframework.data.rest.webmvc.ControllerUtils.*;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.context.AfterCreateEvent;
import org.springframework.data.rest.repository.context.AfterDeleteEvent;
import org.springframework.data.rest.repository.context.AfterSaveEvent;
import org.springframework.data.rest.repository.context.BeforeCreateEvent;
import org.springframework.data.rest.repository.context.BeforeDeleteEvent;
import org.springframework.data.rest.repository.context.BeforeSaveEvent;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.repository.support.DomainObjectMerger;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RestController
@SuppressWarnings("deprecation")
class RepositoryEntityController extends AbstractRepositoryRestController implements ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}";

	private final EntityLinks entityLinks;
	private final PersistentEntityResourceAssembler<Object> perAssembler;
	private final RepositoryRestConfiguration config;
	private final DomainClassConverter<?> converter;
	private final ConversionService conversionService;
	private final DomainObjectMerger domainObjectMerger;

	private final TransactionOperations txOperations;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RepositoryEntityController(Repositories repositories, RepositoryRestConfiguration config,
			EntityLinks entityLinks, PagedResourcesAssembler<Object> assembler,
			PersistentEntityResourceAssembler<Object> perAssembler, DomainClassConverter<?> converter,
			@Qualifier("defaultConversionService") ConversionService conversionService, DomainObjectMerger domainObjectMerger) {

		super(assembler, perAssembler);

		this.entityLinks = entityLinks;
		this.perAssembler = perAssembler;
		this.config = config;
		this.converter = converter;
		this.conversionService = conversionService;
		this.domainObjectMerger = domainObjectMerger;

		this.txOperations = null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = { "application/json",
			"application/x-spring-data-verbose+json" })
	@ResponseBody
	public Resources<?> listEntities(final RepositoryRestRequest request, Pageable pageable)
			throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();

		Iterable<?> results;
		RepositoryMethodInvoker repoMethodInvoker = request.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker) {
			throw new ResourceNotFoundException();
		}

		if (repoMethodInvoker.hasFindAllPageable()) {
			results = repoMethodInvoker.findAll(pageable);
		} else if (repoMethodInvoker.hasFindAllSorted()) {
			results = repoMethodInvoker.findAll(pageable.getSort());
		} else if (repoMethodInvoker.hasFindAll()) {
			results = repoMethodInvoker.findAll();
		} else {
			throw new ResourceNotFoundException();
		}

		ResourceMapping repoMapping = request.getRepositoryResourceMapping();
		if (!repoMethodInvoker.getQueryMethods().isEmpty()) {
			links.add(entityLinks.linkForSingleResource(request.getPersistentEntity().getType(), "search").withRel(
					repoMapping.getRel() + ".search"));
		}

		Link baseLink = request.getRepositoryLink();
		Resources<?> resources = resultToResources(results, baseLink);
		resources.add(links);
		return resources;
	}

	@SuppressWarnings({ "unchecked" })
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = {
			"application/x-spring-data-compact+json", "text/uri-list" })
	@ResponseBody
	public Resources<?> listEntitiesCompact(final RepositoryRestRequest repoRequest, Pageable pageable)
			throws ResourceNotFoundException {
		Resources<?> resources = listEntities(repoRequest, pageable);
		List<Link> links = new ArrayList<Link>(resources.getLinks());

		for (Resource<?> resource : ((Resources<Resource<?>>) resources).getContent()) {
			PersistentEntityResource<?> persistentEntityResource = (PersistentEntityResource<?>) resource;
			links.add(resourceLink(repoRequest, persistentEntityResource));
		}
		if (resources instanceof PagedResources) {
			return new PagedResources<Object>(Collections.emptyList(), ((PagedResources<?>) resources).getMetadata(), links);
		} else {
			return new Resources<Object>(Collections.emptyList(), links);
		}
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, consumes = { "application/json" }, produces = {
			"application/json", "text/uri-list" })
	@ResponseBody
	public ResponseEntity<Resource<?>> createNewEntity(RepositoryRestRequest repoRequest,
			PersistentEntityResource<?> incoming) {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasSaveOne()) {
			throw new NoSuchMethodError();
		}

		publisher.publishEvent(new BeforeCreateEvent(incoming.getContent()));
		Object obj = repoMethodInvoker.save(incoming.getContent());
		publisher.publishEvent(new AfterCreateEvent(obj));

		Link selfLink = perAssembler.getSelfLinkFor(obj);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(selfLink.getHref()));

		if (config.isReturnBodyOnCreate()) {
			return ControllerUtils.toResponseEntity(headers, perAssembler.toResource(obj), HttpStatus.CREATED);
		} else {
			return ControllerUtils.toResponseEntity(headers, null, HttpStatus.CREATED);
		}
	}

	/**
	 * {@code GET / repository}/{id}}
	 * 
	 * @param repoRequest
	 * @param id
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.GET, produces = { "application/json",
			"application/x-spring-data-compact+json", "text/uri-list" })
	@ResponseBody
	public Resource<?> getSingleEntity(RepositoryRestRequest repoRequest, @PathVariable String id)
			throws ResourceNotFoundException {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasFindOne()) {
			throw new ResourceNotFoundException();
		}

		Object domainObj = converter.convert(id, STRING_TYPE,
				TypeDescriptor.valueOf(repoRequest.getPersistentEntity().getType()));

		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		return perAssembler.toResource(domainObj);
	}

	/**
	 * {@code PUT / repository}/{id}} - Updates an existing entity or creates one at exactly that place.
	 * 
	 * @param repoRequest
	 * @param incoming
	 * @param id
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PUT, consumes = { "application/json" },
			produces = { "application/json", "text/uri-list" })
	@ResponseBody
	public ResponseEntity<Resource<?>> updateEntity(RepositoryRestRequest repoRequest,
			PersistentEntityResource<Object> incoming, @PathVariable String id) throws ResourceNotFoundException {

		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasSaveOne() || !repoMethodInvoker.hasFindOne()) {
			throw new NoSuchMethodError();
		}

		Object domainObj = converter.convert(id, STRING_TYPE,
				TypeDescriptor.valueOf(repoRequest.getPersistentEntity().getType()));
		if (null == domainObj) {
			BeanWrapper<?, Object> incomingWrapper = BeanWrapper.create(incoming.getContent(), conversionService);
			PersistentProperty<?> idProp = incoming.getPersistentEntity().getIdProperty();
			incomingWrapper.setProperty(idProp, conversionService.convert(id, idProp.getType()));
			return createNewEntity(repoRequest, incoming);
		}

		domainObjectMerger.merge(incoming.getContent(), domainObj);

		publisher.publishEvent(new BeforeSaveEvent(incoming.getContent()));
		Object obj = repoMethodInvoker.save(domainObj);
		publisher.publishEvent(new AfterSaveEvent(obj));

		if (config.isReturnBodyOnUpdate()) {
			return ControllerUtils.toResponseEntity(null, perAssembler.toResource(obj), HttpStatus.OK);
		} else {
			return ControllerUtils.toResponseEntity(null, null, HttpStatus.NO_CONTENT);
		}
	}

	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> deleteEntity(final RepositoryRestRequest repoRequest, @PathVariable final String id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {
		final RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasFindOne()
				&& !(repoMethodInvoker.hasDeleteOne() || repoMethodInvoker.hasDeleteOneById())) {
			throw new HttpRequestMethodNotSupportedException("DELETE");
		}
		ResourceMapping methodMapping = repoRequest.getRepositoryResourceMapping().getResourceMappingFor("delete");
		if (null != methodMapping && !methodMapping.isExported()) {
			throw new HttpRequestMethodNotSupportedException("DELETE");
		}

		final Object domainObj = converter.convert(id, STRING_TYPE,
				TypeDescriptor.valueOf(repoRequest.getPersistentEntity().getType()));
		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		publisher.publishEvent(new BeforeDeleteEvent(domainObj));
		TransactionCallbackWithoutResult callback = new TransactionCallbackWithoutResult() {
			@Override
			@SuppressWarnings({ "unchecked" })
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				if (repoMethodInvoker.hasDeleteOneById()) {
					Class<? extends Serializable> idType = (Class<? extends Serializable>) repoRequest.getPersistentEntity()
							.getIdProperty().getType();
					final Serializable idVal = conversionService.convert(id, idType);
					repoMethodInvoker.delete(idVal);
				} else if (repoMethodInvoker.hasDeleteOne()) {
					repoMethodInvoker.delete(domainObj);
				}
			}
		};

		// FIXME

		if (txOperations != null) {
			txOperations.execute(callback);
		} else {
			callback.doInTransaction(null);
		}

		publisher.publishEvent(new AfterDeleteEvent(domainObj));

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

}
