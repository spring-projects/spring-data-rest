/*
 * Copyright 2012-2015 the original author or authors.
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

import static org.springframework.data.rest.webmvc.ControllerUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Thibaud Lepretre
 */
@SuppressWarnings({ "rawtypes" })
class AbstractRepositoryRestController {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	private final PagedResourcesAssembler<Object> pagedResourcesAssembler;
	private final AuditableBeanWrapperFactory auditableBeanWrapperFactory;

	/**
	 * Creates a new {@link AbstractRepositoryRestController} for the given {@link PagedResourcesAssembler} and
	 * {@link AuditableBeanWrapperFactory}.
	 * 
	 * @param pagedResourcesAssembler must not be {@literal null}.
	 * @param auditableBeanWrapperFactory must not be {@literal null}.
	 */
	public AbstractRepositoryRestController(PagedResourcesAssembler<Object> pagedResourcesAssembler,
			AuditableBeanWrapperFactory auditableBeanWrapperFactory) {

		Assert.notNull(pagedResourcesAssembler, "PagedResourcesAssembler must not be null!");
		Assert.notNull(auditableBeanWrapperFactory, "AuditableBeanWrapperFactory must not be null!");

		this.pagedResourcesAssembler = pagedResourcesAssembler;
		this.auditableBeanWrapperFactory = auditableBeanWrapperFactory;
	}

	protected Link resourceLink(RootResourceInformation resourceLink, Resource resource) {

		ResourceMetadata repoMapping = resourceLink.getResourceMetadata();

		Link selfLink = resource.getLink("self");
		String rel = repoMapping.getItemResourceRel();

		return new Link(selfLink.getHref(), rel);
	}

	@SuppressWarnings({ "unchecked" })
	protected Resources<?> toResources(Iterable<?> source, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Link baseLink) {

		if (source instanceof Page) {
			Page<Object> page = (Page<Object>) source;
			return entitiesToResources(page, assembler, domainType, baseLink);
		} else if (source instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) source, assembler, domainType);
		} else {
			return new Resources(EMPTY_RESOURCE_LIST);
		}
	}

	/**
	 * Turns the given source into a {@link ResourceSupport} if needed and possible. Uses the given
	 * {@link PersistentEntityResourceAssembler} for the actual conversion.
	 * 
	 * @param source can be must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param domainType the domain type in case the source is an empty iterable, must not be {@literal null}.
	 * @param baseLink can be {@literal null}.
	 * @return
	 */
	protected Object toResource(Object source, PersistentEntityResourceAssembler assembler, Class<?> domainType,
			Link baseLink) {

		if (source instanceof Iterable) {
			return toResources((Iterable<?>) source, assembler, domainType, baseLink);
		} else if (source == null) {
			throw new ResourceNotFoundException();
		} else if (ClassUtils.isPrimitiveOrWrapper(source.getClass())) {
			return source;
		}

		return assembler.toFullResource(source);
	}

	protected Resources<?> entitiesToResources(Page<Object> page, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Link baseLink) {

		if (page.getContent().isEmpty()) {
			return pagedResourcesAssembler.toEmptyResource(page, domainType, baseLink);
		}

		return baseLink == null ? pagedResourcesAssembler.toResource(page, assembler) : pagedResourcesAssembler.toResource(
				page, assembler, baseLink);
	}

	protected Resources<?> entitiesToResources(Iterable<Object> entities, PersistentEntityResourceAssembler assembler,
			Class<?> domainType) {

		if (!entities.iterator().hasNext()) {

			List<Object> content = Arrays.<Object> asList(WRAPPERS.emptyCollectionOf(domainType));
			return new Resources<Object>(content, getDefaultSelfLink());
		}

		List<Resource<Object>> resources = new ArrayList<Resource<Object>>();

		for (Object obj : entities) {
			resources.add(obj == null ? null : assembler.toResource(obj));
		}

		return new Resources<Resource<Object>>(resources, getDefaultSelfLink());
	}

	/**
	 * Returns the default headers to be returned for the given {@link PersistentEntityResource}. Will set {@link ETag}
	 * and {@code Last-Modified} headers if applicable.
	 * 
	 * @param resource can be {@literal null}.
	 * @return
	 */
	protected HttpHeaders prepareHeaders(PersistentEntityResource resource) {
		return resource == null ? new HttpHeaders() : prepareHeaders(resource.getPersistentEntity(), resource.getContent());
	}

	/**
	 * Retruns the default headers to be returned for the given {@link PersistentEntity} and value. Will set {@link ETag}
	 * and {@code Last-Modified} headers if applicable.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 */
	protected HttpHeaders prepareHeaders(PersistentEntity<?, ?> entity, Object value) {

		// Add ETag
		HttpHeaders headers = ETag.from(entity, value).addTo(new HttpHeaders());

		// Add Last-Modified
		AuditableBeanWrapper wrapper = getAuditableBeanWrapper(value);

		if (wrapper == null) {
			return headers;
		}

		Calendar lastModifiedDate = wrapper.getLastModifiedDate();

		if (lastModifiedDate != null) {
			headers.setLastModified(lastModifiedDate.getTimeInMillis());
		}

		return headers;
	}

	/**
	 * Returns the {@link AuditableBeanWrapper} for the given source.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	protected AuditableBeanWrapper getAuditableBeanWrapper(Object source) {
		return auditableBeanWrapperFactory.getBeanWrapperFor(source);
	}

	protected Link getDefaultSelfLink() {
		return new Link(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
	}
}
