/*
 * Copyright 2012-2020 the original author or authors.
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

import static java.util.stream.Collectors.*;
import static org.springframework.data.rest.webmvc.RestMediaTypes.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Ľubomír Varga
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
	public ResponseEntity<RepresentationModel<?>> followPropertyReference(final RootResourceInformation repoRequest,
			@BackendId Serializable id, final @PathVariable String property,
			final PersistentEntityResourceAssembler assembler) throws Exception {

		HttpHeaders headers = new HttpHeaders();

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> prop.mapValue(it -> {

			if (prop.property.isCollectionLike()) {

				return toCollectionModel((Iterable<?>) it, assembler, prop.propertyType, Optional.empty());

			} else if (prop.property.isMap()) {

				return ((Map<Object, Object>) it).entrySet().stream() //
						.collect(collectingAndThen(toMap(Map.Entry::getKey, entry -> assembler.toModel(entry.getValue())),
								MapModel::new));

			} else {

				PersistentEntityResource resource = assembler.toModel(it);
				headers.set("Content-Location", resource.getRequiredLink(IanaLinkRelations.SELF).getHref());
				return resource;
			}

		}).orElseThrow(ResourceNotFoundException::new);

		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, //
				doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET));
	}

	@RequestMapping(value = BASE_MAPPING, method = DELETE)
	public ResponseEntity<? extends RepresentationModel<?>> deletePropertyReference(RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property) throws Exception {

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> prop.mapValue(it -> {

			if (prop.property.isCollectionLike() || prop.property.isMap()) {
				throw HttpRequestMethodNotSupportedException.forRejectedMethod(HttpMethod.DELETE)
						.withAllowedMethods(HttpMethod.GET, HttpMethod.HEAD);
			} else {
				prop.wipeValue();
			}

			publisher.publishEvent(new BeforeLinkDeleteEvent(prop.accessor.getBean(), prop.propertyValue));
			Object result = repoRequest.getInvoker().invokeSave(prop.accessor.getBean());
			publisher.publishEvent(new AfterLinkDeleteEvent(result, prop.propertyValue));

			return (RepresentationModel<?>) null;

		}).orElse(null);

		doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.DELETE);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = GET)
	public ResponseEntity<RepresentationModel<?>> followPropertyReference(RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property, @PathVariable String propertyId,
			PersistentEntityResourceAssembler assembler) throws Exception {

		HttpHeaders headers = new HttpHeaders();

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> prop.mapValue(it -> {

			if (prop.property.isCollectionLike()) {

				for (Object obj : (Iterable<?>) it) {

					IdentifierAccessor accessor1 = prop.entity.getIdentifierAccessor(obj);
					if (propertyId.equals(accessor1.getIdentifier().toString())) {

						PersistentEntityResource resource1 = assembler.toModel(obj);
						headers.set("Content-Location", resource1.getRequiredLink(IanaLinkRelations.SELF).getHref());
						return resource1;
					}
				}

			} else if (prop.property.isMap()) {

				for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) it).entrySet()) {

					IdentifierAccessor accessor2 = prop.entity.getIdentifierAccessor(entry.getValue());
					if (propertyId.equals(accessor2.getIdentifier().toString())) {

						PersistentEntityResource resource2 = assembler.toModel(entry.getValue());
						headers.set("Content-Location", resource2.getRequiredLink(IanaLinkRelations.SELF).getHref());
						return resource2;
					}
				}

			} else {
				return EntityModel.of(prop.propertyValue);
			}

			throw new ResourceNotFoundException();

		}).orElseThrow(ResourceNotFoundException::new);

		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers, //
				doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET));
	}

	@RequestMapping(value = BASE_MAPPING, method = GET, produces = TEXT_URI_LIST_VALUE)
	public ResponseEntity<RepresentationModel<?>> followPropertyReferenceCompact(RootResourceInformation repoRequest,
			@BackendId Serializable id, @PathVariable String property, @RequestHeader HttpHeaders requestHeaders,
			PersistentEntityResourceAssembler assembler) throws Exception {

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> prop.mapValue(it -> {

			if (prop.property.isCollectionLike()) {

				Links links = ((Collection<?>) it).stream() //
						.map(assembler::getExpandedSelfLink) //
						.collect(Links.collector());

				return new RepresentationModel<>(links.toList());

			} else if (prop.property.isMap()) {
				throw new UnsupportedMediaTypeStatusException("Cannot produce compact representation of map property!");
			}

			return new RepresentationModel<>(assembler.getExpandedSelfLink(it));

		}).orElse(new RepresentationModel<>());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(TEXT_URI_LIST);

		return ControllerUtils.toResponseEntity(HttpStatus.OK, headers,
				doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET));
	}

	@RequestMapping(value = BASE_MAPPING, method = { PATCH, PUT, POST }, //
			consumes = { MediaType.APPLICATION_JSON_VALUE, SPRING_DATA_COMPACT_JSON_VALUE, TEXT_URI_LIST_VALUE })
	public ResponseEntity<? extends RepresentationModel<?>> createPropertyReference(
			RootResourceInformation resourceInformation, HttpMethod requestMethod,
			@RequestBody(required = false) CollectionModel<Object> incoming, @BackendId Serializable id,
			@PathVariable String property) throws Exception {

		CollectionModel<Object> source = incoming == null ? CollectionModel.empty() : incoming;
		RepositoryInvoker invoker = resourceInformation.getInvoker();

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> {

			Class<?> propertyType = prop.property.getType();

			if (prop.property.isCollectionLike()) {

				Collection<Object> collection = AUGMENTING_METHODS.contains(requestMethod) //
						? (Collection<Object>) prop.propertyValue //
						: CollectionFactory.createCollection(propertyType, 0);

				// Add to the existing collection
				for (Link l1 : source.getLinks()) {
					collection.add(loadPropertyValue(prop.propertyType, l1));
				}

				prop.accessor.setProperty(prop.property, collection);

			} else if (prop.property.isMap()) {

				Map<LinkRelation, Object> map = AUGMENTING_METHODS.contains(requestMethod) //
						? (Map<LinkRelation, Object>) prop.propertyValue //
						: CollectionFactory.<LinkRelation, Object> createMap(propertyType, 0);

				// Add to the existing collection
				for (Link l2 : source.getLinks()) {
					map.put(l2.getRel(), loadPropertyValue(prop.propertyType, l2));
				}

				prop.accessor.setProperty(prop.property, map);

			} else {

				if (HttpMethod.PATCH.equals(requestMethod)) {
					throw HttpRequestMethodNotSupportedException.forRejectedMethod(HttpMethod.PATCH)//
							.withAllowedMethods(HttpMethod.PATCH)//
							.withMessage(
									"Cannot PATCH a reference to this singular property since the property type is not a List or a Map.");
				}

				if (!source.getLinks().hasSingleLink()) {
					throw new IllegalArgumentException(
							"Must send only 1 link to update a property reference that isn't a List or a Map.");
				}

				prop.accessor.setProperty(prop.property,
						loadPropertyValue(prop.propertyType, source.getLinks().toList().get(0)));
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
	public ResponseEntity<RepresentationModel<?>> deletePropertyReferenceId(RootResourceInformation repoRequest,
			@BackendId Serializable backendId, @PathVariable String property, @PathVariable String propertyId)
			throws Exception {

		Function<ReferencedProperty, RepresentationModel<?>> handler = prop -> prop.mapValue(it -> {

			if (prop.property.isCollectionLike()) {

				Collection<Object> coll = (Collection<Object>) it;
				Iterator<Object> iterator = coll.iterator();

				while (iterator.hasNext()) {

					Object obj = iterator.next();

					Optional.ofNullable(prop.entity.getIdentifierAccessor(obj).getIdentifier())//
							.map(Object::toString)//
							.filter(id -> propertyId.equals(id))//
							.ifPresent(__ -> iterator.remove());
				}

			} else if (prop.property.isMap()) {

				Map<Object, Object> m = (Map<Object, Object>) it;
				Iterator<Entry<Object, Object>> iterator = m.entrySet().iterator();

				while (iterator.hasNext()) {

					Object key = iterator.next().getKey();

					Optional.ofNullable(prop.entity.getIdentifierAccessor(m.get(key)).getIdentifier())//
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

			return (RepresentationModel<?>) null;

		}).orElse(null);

		doWithReferencedProperty(repoRequest, backendId, property, handler, HttpMethod.DELETE);

		return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
	}

	private Object loadPropertyValue(Class<?> type, Link link) {

		String href = link.expand().getHref();
		String id = href.substring(href.lastIndexOf('/') + 1);

		RepositoryInvoker invoker = repositoryInvokerFactory.getInvokerFor(type);

		return invoker.invokeFindById(id).orElse(null);
	}

	private Optional<RepresentationModel<?>> doWithReferencedProperty(RootResourceInformation resourceInformation,
			Serializable id, String propertyPath, Function<ReferencedProperty, RepresentationModel<?>> handler,
			HttpMethod method) throws Exception {

		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		PropertyAwareResourceMapping mapping = metadata.getProperty(propertyPath);

		if (mapping == null || !mapping.isExported()) {
			throw new ResourceNotFoundException();
		}

		PersistentProperty<?> property = mapping.getProperty();
		resourceInformation.verifySupportedMethod(method, property);

		RepositoryInvoker invoker = resourceInformation.getInvoker();
		Optional<Object> domainObj = invoker.invokeFindById(id);

		domainObj.orElseThrow(() -> new ResourceNotFoundException());

		return domainObj.map(it -> {

			PersistentPropertyAccessor<?> accessor = property.getOwner().getPropertyAccessor(it);
			return handler.apply(new ReferencedProperty(property, accessor.getProperty(property), accessor));
		});
	}

	private class ReferencedProperty {

		final PersistentEntity<?, ?> entity;
		final PersistentProperty<?> property;
		final Class<?> propertyType;
		final Object propertyValue;
		final PersistentPropertyAccessor<?> accessor;

		private ReferencedProperty(PersistentProperty<?> property, Object propertyValue,
				PersistentPropertyAccessor<?> accessor) {

			this.property = property;
			this.propertyValue = propertyValue;
			this.accessor = accessor;
			this.propertyType = property.getActualType();
			this.entity = repositories.getPersistentEntity(propertyType);
		}

		public void wipeValue() {
			accessor.setProperty(property, null);
		}

		public <T> Optional<T> mapValue(Function<Object, T> function) {
			return Optional.ofNullable(propertyValue).map(function);
		}
	}

	@ExceptionHandler
	public ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException exception) {
		return exception.toResponse();
	}

	/**
	 * Custom {@link RepresentationModel} to be used with maps as {@link EntityModel} doesn't properly unwrap {@link Map}s
	 * due to some limitation in Jackson.
	 *
	 * @author Oliver Drotbohm
	 * @see https://github.com/FasterXML/jackson-databind/issues/171
	 */
	private static class MapModel extends RepresentationModel<MapModel> {

		private Map<? extends Object, ? extends Object> content;

		public MapModel(Map<? extends Object, ? extends Object> content, Link... links) {

			super(Arrays.asList(links));

			this.content = content;
		}

		@JsonAnyGetter
		public Map<? extends Object, ? extends Object> getContent() {
			return content;
		}
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
