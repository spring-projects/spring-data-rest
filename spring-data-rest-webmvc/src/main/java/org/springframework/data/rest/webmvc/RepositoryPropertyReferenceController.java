/*
 * Copyright 2012-2014 the original author or authors.
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
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.event.AfterLinkDeleteEvent;
import org.springframework.data.rest.core.event.AfterLinkSaveEvent;
import org.springframework.data.rest.core.event.BeforeLinkDeleteEvent;
import org.springframework.data.rest.core.event.BeforeLinkSaveEvent;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.util.Function;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RepositoryRestController
@SuppressWarnings({ "unchecked" })
public class RepositoryPropertyReferenceController extends AbstractRepositoryRestController implements
		ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}/{id}/{property}";

	private final Repositories repositories;
	private final PersistentEntityResourceAssembler<Object> perAssembler;
	private final DomainClassConverter<?> converter;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RepositoryPropertyReferenceController(Repositories repositories, DomainClassConverter<?> domainClassConverter,
			PagedResourcesAssembler<Object> assembler, PersistentEntityResourceAssembler<Object> perAssembler) {

		super(assembler, perAssembler);

		this.repositories = repositories;
		this.perAssembler = perAssembler;
		this.converter = domainClassConverter;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<ResourceSupport> followPropertyReference(final RepositoryRestRequest repoRequest,
			@PathVariable String id, @PathVariable String property) throws ResourceNotFoundException, NoSuchMethodException {

		final HttpHeaders headers = new HttpHeaders();

		Function<ReferencedProperty, ResourceSupport> handler = new Function<ReferencedProperty, ResourceSupport>() {

			@Override
			public ResourceSupport apply(ReferencedProperty prop) {

				if (null == prop.propertyValue) {
					throw new ResourceNotFoundException();
				}

				if (prop.property.isCollectionLike()) {

					List<Resource<?>> resources = new ArrayList<Resource<?>>();

					for (Object obj : (Iterable<Object>) prop.propertyValue) {
						resources.add(perAssembler.toResource(obj));
					}

					return new Resources<Resource<?>>(resources);

				} else if (prop.property.isMap()) {

					Map<Object, Resource<?>> resources = new HashMap<Object, Resource<?>>();

					for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) prop.propertyValue).entrySet()) {
						resources.put(entry.getKey(), perAssembler.toResource(entry.getValue()));
					}

					return new Resource<Object>(resources);

				} else {

					PersistentEntityResource<Object> resource = perAssembler.toResource(prop.propertyValue);
					headers.set("Content-Location", resource.getId().getHref());
					return resource;
				}
			}
		};

		ResourceSupport responseResource = doWithReferencedProperty(repoRequest, id, property, handler);
		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, responseResource);
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE)
	public ResponseEntity<? extends ResourceSupport> deletePropertyReference(final RepositoryRestRequest repoRequest,
			@PathVariable String id, @PathVariable String property) throws ResourceNotFoundException, NoSuchMethodException,
			HttpRequestMethodNotSupportedException {

		final RepositoryInvoker repoMethodInvoker = repoRequest.getRepositoryInvoker();

		if (!repoMethodInvoker.exposesDelete()) {
			return new ResponseEntity<Resource<?>>(HttpStatus.METHOD_NOT_ALLOWED);
		}

		Function<ReferencedProperty, ResourceSupport> handler = new Function<ReferencedProperty, ResourceSupport>() {

			@Override
			public Resource<?> apply(ReferencedProperty prop) {

				if (null == prop.propertyValue) {
					return null;
				}

				if (prop.property.isCollectionLike()) {
					throw new IllegalArgumentException(new HttpRequestMethodNotSupportedException("DELETE"));
				} else if (prop.property.isMap()) {
					throw new IllegalArgumentException(new HttpRequestMethodNotSupportedException("DELETE"));
				} else {
					prop.wrapper.setProperty(prop.property, null);
				}

				publisher.publishEvent(new BeforeLinkDeleteEvent(prop.wrapper.getBean(), prop.propertyValue));
				Object result = repoMethodInvoker.invokeSave(prop.wrapper.getBean());
				publisher.publishEvent(new AfterLinkDeleteEvent(result, prop.propertyValue));

				return null;
			}
		};

		try {
			doWithReferencedProperty(repoRequest, id, property, handler);
		} catch (IllegalArgumentException iae) {
			if (iae.getCause() instanceof HttpRequestMethodNotSupportedException) {
				throw (HttpRequestMethodNotSupportedException) iae.getCause();
			}
		}

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = RequestMethod.GET, produces = { "application/json",
			"application/x-spring-data-verbose+json", "application/x-spring-data-compact+json", "text/uri-list" })
	public ResponseEntity<ResourceSupport> followPropertyReference(final RepositoryRestRequest repoRequest,
			@PathVariable String id, @PathVariable String property, final @PathVariable String propertyId)
			throws ResourceNotFoundException, NoSuchMethodException {

		final HttpHeaders headers = new HttpHeaders();
		Function<ReferencedProperty, ResourceSupport> handler = new Function<ReferencedProperty, ResourceSupport>() {

			@Override
			public ResourceSupport apply(ReferencedProperty prop) {

				if (null == prop.propertyValue) {
					throw new ResourceNotFoundException();
				}
				if (prop.property.isCollectionLike()) {
					for (Object obj : (Iterable<?>) prop.propertyValue) {

						BeanWrapper<?, Object> propValWrapper = BeanWrapper.create(obj, null);
						String sId = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();

						if (propertyId.equals(sId)) {

							PersistentEntityResource<Object> resource = perAssembler.toResource(obj);
							headers.set("Content-Location", resource.getId().getHref());
							return resource;
						}
					}
				} else if (prop.property.isMap()) {
					for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) prop.propertyValue).entrySet()) {

						BeanWrapper<?, Object> propValWrapper = BeanWrapper.create(entry.getValue(), null);
						String sId = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();

						if (propertyId.equals(sId)) {

							PersistentEntityResource<Object> resource = perAssembler.toResource(entry.getValue());
							headers.set("Content-Location", resource.getId().getHref());
							return resource;
						}
					}
				} else {
					return new Resource<Object>(prop.propertyValue);
				}
				throw new IllegalArgumentException(new ResourceNotFoundException());
			}
		};

		ResourceSupport responseResource = doWithReferencedProperty(repoRequest, id, property, handler);
		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, responseResource);
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = {
			"application/x-spring-data-compact+json", "text/uri-list" })
	public ResponseEntity<ResourceSupport> followPropertyReferenceCompact(RepositoryRestRequest repoRequest,
			@PathVariable String id, @PathVariable String property) throws ResourceNotFoundException, NoSuchMethodException {

		ResponseEntity<ResourceSupport> response = followPropertyReference(repoRequest, id, property);

		if (response.getStatusCode() != HttpStatus.OK) {
			return response;
		}

		ResourceMetadata repoMapping = repoRequest.getResourceMetadata();
		PersistentProperty<?> persistentProp = repoRequest.getPersistentEntity().getPersistentProperty(property);
		ResourceMapping propertyMapping = repoMapping.getMappingFor(persistentProp);

		ResourceSupport resource = response.getBody();

		List<Link> links = new ArrayList<Link>();

		ControllerLinkBuilder linkBuilder = linkTo(methodOn(RepositoryPropertyReferenceController.class)
				.followPropertyReference(repoRequest, id, property));

		if (resource instanceof Resource) {

			Object content = ((Resource<?>) resource).getContent();
			if (content instanceof Iterable) {

				for (Resource<?> res : (Iterable<Resource<?>>) content) {
					links.add(linkBuilder.withRel(propertyMapping.getRel()));
				}

			} else if (content instanceof Map) {

				Map<Object, Resource<?>> map = (Map<Object, Resource<?>>) content;

				for (Entry<Object, Resource<?>> entry : map.entrySet()) {
					Link l = new Link(entry.getValue().getLink("self").getHref(), entry.getKey().toString());
					links.add(l);
				}
			}

		} else {
			links.add(linkBuilder.withRel(propertyMapping.getRel()));
		}

		return ControllerUtils.toResponseEntity(HttpStatus.OK, null, new Resource<Object>(EMPTY_RESOURCE_LIST, links));
	}

	@RequestMapping(value = BASE_MAPPING, //
			method = { RequestMethod.POST, RequestMethod.PUT }, //
			consumes = { "application/json", "application/x-spring-data-compact+json", "text/uri-list" })
	@ResponseBody
	public ResponseEntity<? extends ResourceSupport> createPropertyReference(final RepositoryRestRequest repoRequest,
			final @RequestBody Resources<Object> incoming, @PathVariable String id, @PathVariable String property,
			final UriComponentsBuilder builder) throws NoSuchMethodException {

		final RepositoryInvoker invoker = repoRequest.getRepositoryInvoker();

		if (!invoker.exposesSave()) {
			return new ResponseEntity<Resource<?>>(HttpStatus.METHOD_NOT_ALLOWED);
		}

		Function<ReferencedProperty, ResourceSupport> handler = new Function<ReferencedProperty, ResourceSupport>() {

			@Override
			public ResourceSupport apply(ReferencedProperty prop) {

				Class<?> propertyType = prop.property.getType();

				if (prop.property.isCollectionLike()) {

					Collection<Object> coll = CollectionFactory.createCollection(propertyType, 0);

					// Either load the exist collection to add to it (POST)
					if (HttpMethod.POST.equals(repoRequest.getRequestMethod())) {
						coll = (Collection<Object>) prop.propertyValue;
					}

					// Add to the existing collection
					for (Link l : incoming.getLinks()) {
						Object propVal = loadPropertyValue(prop.propertyType, l.getHref());
						coll.add(propVal);
					}

					prop.wrapper.setProperty(prop.property, coll);

				} else if (prop.property.isMap()) {

					Map<String, Object> m = CollectionFactory.createMap(propertyType, 0);

					// Either load the exist collection to add to it (POST)
					if (HttpMethod.POST.equals(repoRequest.getRequestMethod())) {
						m = (Map<String, Object>) prop.propertyValue;
					}

					// Add to the existing collection
					for (Link l : incoming.getLinks()) {
						Object propVal = loadPropertyValue(prop.propertyType, l.getHref());
						m.put(l.getRel(), propVal);
					}

					prop.wrapper.setProperty(prop.property, m);

				} else {

					if (HttpMethod.POST.equals(repoRequest.getRequestMethod())) {
						throw new IllegalStateException(
								"Cannot POST a reference to this singular property since the property type is not a List or a Map.");
					}

					if (incoming.getLinks().size() != 1) {
						throw new IllegalArgumentException(
								"Must send only 1 link to update a property reference that isn't a List or a Map.");
					}

					Object propVal = loadPropertyValue(prop.propertyType, incoming.getLinks().get(0).getHref());
					prop.wrapper.setProperty(prop.property, propVal);
				}

				publisher.publishEvent(new BeforeLinkSaveEvent(prop.wrapper.getBean(), prop.propertyValue));
				Object result = invoker.invokeSave(prop.wrapper.getBean());
				publisher.publishEvent(new AfterLinkSaveEvent(result, prop.propertyValue));

				return null;
			}
		};

		doWithReferencedProperty(repoRequest, id, property, handler);

		HttpHeaders headers = new HttpHeaders();
		headers.set("Location", builder.build().toUriString());

		return ControllerUtils.toEmptyResponse(HttpStatus.CREATED, headers);
	}

	@RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<ResourceSupport> deletePropertyReferenceId(final RepositoryRestRequest repoRequest,
			@PathVariable String id, @PathVariable String property, final @PathVariable String propertyId)
			throws NoSuchMethodException {

		final RepositoryInvoker invoker = repoRequest.getRepositoryInvoker();

		if (!invoker.exposesDelete()) {
			throw new NoSuchMethodError();
		}

		Function<ReferencedProperty, ResourceSupport> handler = new Function<ReferencedProperty, ResourceSupport>() {

			@Override
			public ResourceSupport apply(ReferencedProperty prop) {

				if (null == prop.propertyValue) {
					return null;
				}

				if (prop.property.isCollectionLike()) {
					Collection<Object> coll = (Collection<Object>) prop.propertyValue;
					Iterator<Object> itr = coll.iterator();
					while (itr.hasNext()) {
						Object obj = itr.next();
						BeanWrapper<?, Object> propValWrapper = BeanWrapper.create(obj, null);
						String s = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();
						if (propertyId.equals(s)) {
							itr.remove();
						}
					}
				} else if (prop.property.isMap()) {
					Map<Object, Object> m = (Map<Object, Object>) prop.propertyValue;
					Iterator<Object> itr = m.keySet().iterator();
					while (itr.hasNext()) {
						Object key = itr.next();
						BeanWrapper<?, Object> propValWrapper = BeanWrapper.create(m.get(key), null);
						String s = propValWrapper.getProperty(prop.entity.getIdProperty()).toString();
						if (propertyId.equals(s)) {
							itr.remove();
						}
					}
				} else {
					prop.wrapper.setProperty(prop.property, null);
				}

				publisher.publishEvent(new BeforeLinkDeleteEvent(prop.wrapper.getBean(), prop.propertyValue));
				Object result = invoker.invokeSave(prop.wrapper.getBean());
				publisher.publishEvent(new AfterLinkDeleteEvent(result, prop.propertyValue));

				return null;
			}
		};

		doWithReferencedProperty(repoRequest, id, property, handler);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	private Object loadPropertyValue(Class<?> type, String href) {
		String id = href.substring(href.lastIndexOf('/') + 1);
		return converter.convert(id, STRING_TYPE, TypeDescriptor.valueOf(type));
	}

	private ResourceSupport doWithReferencedProperty(RepositoryRestRequest repoRequest, String id, String propertyPath,
			Function<ReferencedProperty, ResourceSupport> handler) throws NoSuchMethodException {

		RepositoryInvoker invoker = repoRequest.getRepositoryInvoker();

		if (!invoker.exposesFindOne()) {
			throw new NoSuchMethodException();
		}

		Object domainObj = invoker.invokeFindOne(id);

		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		PersistentProperty<?> prop = repoRequest.getPersistentEntity().getPersistentProperty(propertyPath);
		if (null == prop) {
			throw new ResourceNotFoundException();
		}

		BeanWrapper<?, Object> wrapper = BeanWrapper.create(domainObj, null);
		Object propVal = wrapper.getProperty(prop);

		return handler.apply(new ReferencedProperty(prop, propVal, wrapper));
	}

	private class ReferencedProperty {

		final PersistentEntity<?, ?> entity;
		final PersistentProperty<?> property;
		final Class<?> propertyType;
		final Object propertyValue;
		final BeanWrapper<?, ?> wrapper;

		private ReferencedProperty(PersistentProperty<?> property, Object propertyValue, BeanWrapper<?, ?> wrapper) {

			this.property = property;
			this.propertyValue = propertyValue;
			this.wrapper = wrapper;
			this.propertyType = property.getActualType();
			this.entity = repositories.getPersistentEntity(propertyType);
		}
	}
}
