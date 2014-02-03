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

import static org.springframework.data.rest.webmvc.ControllerUtils.*;

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
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.DomainClassConverter;
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
import org.springframework.data.rest.core.support.DomainObjectMerger;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
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

	private final EntityLinks entityLinks;
	private final PersistentEntityResourceAssembler<Object> perAssembler;
	private final RepositoryRestConfiguration config;
	private final DomainClassConverter<?> converter;
	private final ConversionService conversionService;
	private final DomainObjectMerger domainObjectMerger;

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
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public Resources<?> listEntities(final RepositoryRestRequest request, Pageable pageable, Sort sort)
			throws ResourceNotFoundException {

		List<Link> links = new ArrayList<Link>();
		Iterable<?> results;
		RepositoryInvoker repoMethodInvoker = request.getRepositoryInvoker();

		if (null == repoMethodInvoker) {
			throw new ResourceNotFoundException();
		}

		if (pageable != null) {
			results = repoMethodInvoker.invokeFindAll(pageable);
		} else {
			results = repoMethodInvoker.invokeFindAll(sort);
		}

		ResourceMetadata metadata = request.getResourceMetadata();
		SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();

		if (searchMappings.isExported()) {
			links.add(entityLinks.linkFor(metadata.getDomainType()).slash(searchMappings.getPath())
					.withRel(searchMappings.getRel()));
		}

		Resources<?> resources = resultToResources(results);
		resources.add(links);
		return resources;
	}

	@ResponseBody
	@SuppressWarnings({ "unchecked" })
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = {
			"application/x-spring-data-compact+json", "text/uri-list" })
	public Resources<?> listEntitiesCompact(final RepositoryRestRequest repoRequest, Pageable pageable, Sort sort) {

		Resources<?> resources = listEntities(repoRequest, pageable, sort);
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

	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, consumes = { "application/json" })
	public ResponseEntity<ResourceSupport> createNewEntity(RepositoryRestRequest repoRequest,
			PersistentEntityResource<?> incoming) {

		RepositoryInvoker invoker = repoRequest.getRepositoryInvoker();

		if (!invoker.exposesSave()) {
			throw new NoSuchMethodError();
		}

		publisher.publishEvent(new BeforeCreateEvent(incoming.getContent()));
		Object obj = invoker.invokeSave(incoming.getContent());
		publisher.publishEvent(new AfterCreateEvent(obj));

		Link selfLink = perAssembler.getSelfLinkFor(obj);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(selfLink.getHref()));

		PersistentEntityResource<Object> resource = config.isReturnBodyOnCreate() ? perAssembler.toResource(obj) : null;
		return ControllerUtils.toResponseEntity(HttpStatus.CREATED, headers, resource);
	}

	/**
	 * {@code GET / repository}/{id}}
	 * 
	 * @param repoRequest
	 * @param id
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Resource<?>> getSingleEntity(RepositoryRestRequest repoRequest, @PathVariable String id) {

		RepositoryInvoker repoMethodInvoker = repoRequest.getRepositoryInvoker();

		if (!repoMethodInvoker.exposesFindOne()) {
			return new ResponseEntity<Resource<?>>(HttpStatus.NOT_FOUND);
		}

		Object domainObj = repoMethodInvoker.invokeFindOne(id);

		if (domainObj == null) {
			return new ResponseEntity<Resource<?>>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Resource<?>>(perAssembler.toResource(domainObj), HttpStatus.OK);
	}

	/**
	 * {@code PUT / repository}/{id}} - Updates an existing entity or creates one at exactly that place.
	 * 
	 * @param request
	 * @param incoming
	 * @param id
	 * @return
	 */
	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PUT, consumes = { "application/json" })
	public ResponseEntity<? extends ResourceSupport> updateEntity(RepositoryRestRequest request,
			PersistentEntityResource<Object> incoming, @PathVariable String id) {

		RepositoryInvoker invoker = request.getRepositoryInvoker();
		if (!invoker.exposesSave() || !invoker.exposesFindOne()) {
			return new ResponseEntity<Resource<?>>(HttpStatus.METHOD_NOT_ALLOWED);
		}

		Object domainObj = converter.convert(id, STRING_TYPE,
				TypeDescriptor.valueOf(request.getPersistentEntity().getType()));
		if (null == domainObj) {
			BeanWrapper<?, Object> incomingWrapper = BeanWrapper.create(incoming.getContent(), conversionService);
			PersistentProperty<?> idProp = incoming.getPersistentEntity().getIdProperty();
			incomingWrapper.setProperty(idProp, conversionService.convert(id, idProp.getType()));
			return createNewEntity(request, incoming);
		}

		domainObjectMerger.merge(incoming.getContent(), domainObj);

		publisher.publishEvent(new BeforeSaveEvent(incoming.getContent()));
		Object obj = invoker.invokeSave(domainObj);
		publisher.publishEvent(new AfterSaveEvent(obj));

		Link selfLink = perAssembler.getSelfLinkFor(obj);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(selfLink.getHref()));

		if (config.isReturnBodyOnUpdate()) {
			return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, perAssembler.toResource(obj));
		} else {
			return ControllerUtils.toResponseEntity(HttpStatus.NO_CONTENT, headers, null);
		}
	}

	@RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteEntity(final RepositoryRestRequest repoRequest, @PathVariable final String id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		RepositoryInvoker invoker = repoRequest.getRepositoryInvoker();

		if (!invoker.exposesDelete() || !invoker.exposesFindOne()) {
			throw new HttpRequestMethodNotSupportedException(RequestMethod.DELETE.toString());
		}

		// TODO: re-enable not exposing delete method if hidden

		// ResourceMapping methodMapping = repoRequest.getRepositoryResourceMapping().getResourceMappingFor("delete");
		// if (null != methodMapping && !methodMapping.isExported()) {
		// throw new HttpRequestMethodNotSupportedException("DELETE");
		// }

		Object domainObj = invoker.invokeFindOne(id);

		publisher.publishEvent(new BeforeDeleteEvent(domainObj));
		invoker.invokeDelete(id);
		publisher.publishEvent(new AfterDeleteEvent(domainObj));

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

}
