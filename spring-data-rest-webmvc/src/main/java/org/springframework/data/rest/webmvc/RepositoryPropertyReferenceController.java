/*
 * Copyright 2012-2015 the original author or authors.
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
import static org.springframework.data.rest.webmvc.RestMediaTypes.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.event.AfterLinkDeleteEvent;
import org.springframework.data.rest.core.event.AfterLinkSaveEvent;
import org.springframework.data.rest.core.event.BeforeLinkDeleteEvent;
import org.springframework.data.rest.core.event.BeforeLinkSaveEvent;
import org.springframework.data.rest.core.mapping.PropertyAwareResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RepositoryRestController
@SuppressWarnings({ "unchecked" })
class RepositoryPropertyReferenceController extends AbstractRepositoryRestController
		implements ApplicationEventPublisherAware {

	private static final String BASE_MAPPING = "/{repository}/{id}/{property}";
	private static final Collection<HttpMethod> AUGMENTING_METHODS = Arrays.asList(HttpMethod.PATCH, HttpMethod.POST);

	private final Repositories repositories;
	private final RepositoryInvokerFactory repositoryInvokerFactory;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RepositoryPropertyReferenceController(Repositories repositories,
			RepositoryInvokerFactory repositoryInvokerFactory, PagedResourcesAssembler<Object> assembler) {

		super(assembler);

		this.repositories = repositories;
		this.repositoryInvokerFactory = repositoryInvokerFactory;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@RequestMapping(value = BASE_MAPPING, method = GET)
	public ResponseEntity<ResourceSupport> followPropertyReference(final RootResourceInformation repoRequest,
			@BackendId Serializable id, final @PathVariable String property,
			final PersistentEntityResourceAssembler assembler) throws Exception {

		final HttpHeaders headers = new HttpHeaders();

		Function<ReferencedProperty, ResourceSupport> handler = prop -> prop.propertyValue.map(it -> {

			if (prop.property.isCollectionLike()) {

				return toResources((Iterable<?>) it, assembler, prop.propertyType, null);

			} else if (prop.property.isMap()) {

				Map<Object, Resource<?>> resources = new HashMap<Object, Resource<?>>();

				for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) it).entrySet()) {
					resources.put(entry.getKey(), assembler.toResource(entry.getValue()));
				}

				return new Resource<Object>(resources);

			} else {

				PersistentEntityResource resource = assembler.toResource(it);
				headers.set("Content-Location", resource.getId().getHref());
				return resource;
			}

		}).orElseThrow(() -> new ResourceNotFoundException());

		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, //
				doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET));
	}

	@RequestMapping(value = BASE_MAPPING, method = DELETE)
	public ResponseEntity<? extends ResourceSupport> deletePropertyReference(final RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property) throws Exception {

		Function<ReferencedProperty, ResourceSupport> handler = prop -> prop.propertyValue.map(it -> {

			if (prop.property.isCollectionLike() || prop.property.isMap()) {
				throw HttpRequestMethodNotSupportedException.forRejectedMethod(HttpMethod.DELETE)
						.withAllowedMethods(HttpMethod.GET, HttpMethod.HEAD);
			} else {
				prop.wipeValue();
			}

			publisher.publishEvent(new BeforeLinkDeleteEvent(prop.accessor.getBean(), prop.propertyValue));
			Object result = repoRequest.getInvoker().invokeSave(prop.accessor.getBean());
			publisher.publishEvent(new AfterLinkDeleteEvent(result, prop.propertyValue));

			return (ResourceSupport) null;

		}).orElse(null);

		doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.DELETE);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = GET)
	public ResponseEntity<ResourceSupport> followPropertyReference(final RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property, final @PathVariable String propertyId,
			final PersistentEntityResourceAssembler assembler) throws Exception {

		final HttpHeaders headers = new HttpHeaders();

		Function<ReferencedProperty, ResourceSupport> handler = prop -> prop.propertyValue.map(it -> {

			if (prop.property.isCollectionLike()) {

				for (Object obj : (Iterable<?>) it) {

					IdentifierAccessor accessor1 = prop.entity.getIdentifierAccessor(obj);
					if (propertyId.equals(accessor1.getIdentifier().toString())) {

						PersistentEntityResource resource1 = assembler.toResource(obj);
						headers.set("Content-Location", resource1.getId().getHref());
						return resource1;
					}
				}

			} else if (prop.property.isMap()) {

				for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) it).entrySet()) {

					IdentifierAccessor accessor2 = prop.entity.getIdentifierAccessor(entry.getValue());
					if (propertyId.equals(accessor2.getIdentifier().toString())) {

						PersistentEntityResource resource2 = assembler.toResource(entry.getValue());
						headers.set("Content-Location", resource2.getId().getHref());
						return resource2;
					}
				}

			} else {
				return new Resource<Object>(prop.propertyValue);
			}

			throw new ResourceNotFoundException();

		}).orElseThrow(() -> new ResourceNotFoundException());

		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, //
				doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET));
	}

	@RequestMapping(value = BASE_MAPPING, method = GET,
			produces = { SPRING_DATA_COMPACT_JSON_VALUE, TEXT_URI_LIST_VALUE })
	public ResponseEntity<ResourceSupport> followPropertyReferenceCompact(RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property, PersistentEntityResourceAssembler assembler)
			throws Exception {

		ResponseEntity<ResourceSupport> response = followPropertyReference(repoRequest, id, property, assembler);

		if (response.getStatusCode() != HttpStatus.OK) {
			return response;
		}

		ResourceMetadata repoMapping = repoRequest.getResourceMetadata();
		PersistentProperty<?> persistentProp = repoRequest.getPersistentEntity().getRequiredPersistentProperty(property);
		ResourceMapping propertyMapping = repoMapping.getMappingFor(persistentProp);

		ResourceSupport resource = response.getBody();

		List<Link> links = new ArrayList<Link>();

		ControllerLinkBuilder linkBuilder = linkTo(methodOn(RepositoryPropertyReferenceController.class)
				.followPropertyReference(repoRequest, id, property, assembler));

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

	@RequestMapping(value = BASE_MAPPING, method = { PATCH, PUT, POST }, //
			consumes = { MediaType.APPLICATION_JSON_VALUE, SPRING_DATA_COMPACT_JSON_VALUE, TEXT_URI_LIST_VALUE })
	public ResponseEntity<? extends ResourceSupport> createPropertyReference(
			final RootResourceInformation resourceInformation, final HttpMethod requestMethod,
			final @RequestBody(required = false) Resources<Object> incoming, @BackendId Serializable id,
			@PathVariable String property) throws Exception {

		final Resources<Object> source = incoming == null ? new Resources<Object>(Collections.emptyList()) : incoming;
		final RepositoryInvoker invoker = resourceInformation.getInvoker();

		Function<ReferencedProperty, ResourceSupport> handler = prop -> {

			Class<?> propertyType = prop.property.getType();

			if (prop.property.isCollectionLike()) {

				Collection<Object> collection = AUGMENTING_METHODS.contains(requestMethod)
						? (Collection<Object>) prop.propertyValue.orElse(null)
						: CollectionFactory.createCollection(propertyType, 0);

				// Add to the existing collection
				for (Link l1 : source.getLinks()) {
					collection.add(loadPropertyValue(prop.propertyType, l1).orElse(null));
				}

				prop.accessor.setProperty(prop.property, Optional.of(collection));

			} else if (prop.property.isMap()) {

				Map<String, Object> map = AUGMENTING_METHODS.contains(requestMethod)
						? (Map<String, Object>) prop.propertyValue.orElse(null)
						: CollectionFactory.<String, Object> createMap(propertyType, 0);

				// Add to the existing collection
				for (Link l2 : source.getLinks()) {
					map.put(l2.getRel(), loadPropertyValue(prop.propertyType, l2).orElse(null));
				}

				prop.accessor.setProperty(prop.property, Optional.of(map));

			} else {

				if (HttpMethod.PATCH.equals(requestMethod)) {
					throw HttpRequestMethodNotSupportedException.forRejectedMethod(HttpMethod.PATCH)//
							.withAllowedMethods(HttpMethod.PATCH)//
							.withMessage(
									"Cannot PATCH a reference to this singular property since the property type is not a List or a Map.");
				}

				if (source.getLinks().size() != 1) {
					throw new IllegalArgumentException(
							"Must send only 1 link to update a property reference that isn't a List or a Map.");
				}

				Optional<Object> propVal = loadPropertyValue(prop.propertyType, source.getLinks().get(0));
				prop.accessor.setProperty(prop.property, propVal);
			}

			publisher.publishEvent(new BeforeLinkSaveEvent(prop.accessor.getBean(), prop.propertyValue));
			Object result = invoker.invokeSave(prop.accessor.getBean());
			publisher.publishEvent(new AfterLinkSaveEvent(result, prop.propertyValue));

			return null;
		};

		doWithReferencedProperty(resourceInformation, id, property, handler, requestMethod);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = DELETE)
	public ResponseEntity<ResourceSupport> deletePropertyReferenceId(final RootResourceInformation repoRequest,
			@BackendId Serializable backendId, @PathVariable String property, final @PathVariable String propertyId)
			throws Exception {

		Function<ReferencedProperty, ResourceSupport> handler = prop -> prop.propertyValue.map(it -> {

			if (prop.property.isCollectionLike()) {

				Collection<Object> coll = (Collection<Object>) it;
				Iterator<Object> iterator = coll.iterator();

				while (iterator.hasNext()) {

					Object obj = iterator.next();

					prop.entity.getIdentifierAccessor(obj).getIdentifier()//
							.map(Object::toString)//
							.filter(id -> propertyId.equals(id))//
							.ifPresent(__ -> iterator.remove());
				}

			} else if (prop.property.isMap()) {

				Map<Object, Object> m = (Map<Object, Object>) it;
				Iterator<Entry<Object, Object>> iterator = m.entrySet().iterator();

				while (iterator.hasNext()) {

					Object key = iterator.next().getKey();

					prop.entity.getIdentifierAccessor(m.get(key)).getIdentifier()//
							.map(Object::toString)//
							.filter(id -> propertyId.equals(id))//
							.ifPresent(__ -> iterator.remove());
				}

			} else {
				prop.wipeValue();
			}

			publisher.publishEvent(new BeforeLinkDeleteEvent(prop.accessor.getBean(), it));
			Object result = repoRequest.getInvoker().invokeSave(prop.accessor.getBean());
			publisher.publishEvent(new AfterLinkDeleteEvent(result, it));

			return (ResourceSupport) null;

		}).orElse(null);

		doWithReferencedProperty(repoRequest, backendId, property, handler, HttpMethod.DELETE);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	private Optional<Object> loadPropertyValue(Class<?> type, Link link) {

		String href = link.expand().getHref();
		String id = href.substring(href.lastIndexOf('/') + 1);

		RepositoryInvoker invoker = repositoryInvokerFactory.getInvokerFor(type);

		return invoker.invokeFindOne(id);
	}

	private Optional<ResourceSupport> doWithReferencedProperty(RootResourceInformation resourceInformation,
			Serializable id, String propertyPath, Function<ReferencedProperty, ResourceSupport> handler, HttpMethod method)
			throws Exception {

		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		PropertyAwareResourceMapping mapping = metadata.getProperty(propertyPath);

		if (mapping == null || !mapping.isExported()) {
			throw new ResourceNotFoundException();
		}

		PersistentProperty<?> property = mapping.getProperty();
		resourceInformation.verifySupportedMethod(method, property);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Optional<Object> domainObj = invoker.invokeFindOne(id);

		domainObj.orElseThrow(() -> new ResourceNotFoundException());

		return domainObj.map(it -> {

			PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(it);
			return handler.apply(new ReferencedProperty(property, accessor.getProperty(property), accessor));
		});
	}

	private class ReferencedProperty {

		final PersistentEntity<?, ?> entity;
		final PersistentProperty<?> property;
		final Class<?> propertyType;
		final Optional<Object> propertyValue;
		final PersistentPropertyAccessor accessor;

		private ReferencedProperty(PersistentProperty<?> property, Optional<Object> propertyValue,
				PersistentPropertyAccessor wrapper) {

			this.property = property;
			this.propertyValue = propertyValue;
			this.accessor = wrapper;
			this.propertyType = property.getActualType();
			this.entity = repositories.getPersistentEntity(propertyType);
		}

		public void writeValue() {
			accessor.setProperty(property, propertyValue);
		}

		public void wipeValue() {
			accessor.setProperty(property, Optional.empty());
		}
	}

	@ExceptionHandler
	public ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException exception) {
		return exception.toResponse();
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static class HttpRequestMethodNotSupportedException extends RuntimeException {

		private static final long serialVersionUID = 3704212056962845475L;

		private final HttpMethod rejectedMethod;
		private final HttpMethod[] allowedMethods;
		private final String message;

		public static HttpRequestMethodNotSupportedException forRejectedMethod(HttpMethod method) {
			return new HttpRequestMethodNotSupportedException(method, new HttpMethod[0], null);
		}

		public HttpRequestMethodNotSupportedException withAllowedMethods(HttpMethod... methods) {
			return new HttpRequestMethodNotSupportedException(this.rejectedMethod, methods.clone(), null);
		}

		public HttpRequestMethodNotSupportedException withMessage(String message, Object... parameters) {
			return new HttpRequestMethodNotSupportedException(this.rejectedMethod, this.allowedMethods,
					String.format(message, parameters));
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Throwable#getMessage()
		 */
		@Override
		public String getMessage() {
			return message;
		}

		public ResponseEntity<Void> toResponse() {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).allow(allowedMethods).build();
		}
	}
}
