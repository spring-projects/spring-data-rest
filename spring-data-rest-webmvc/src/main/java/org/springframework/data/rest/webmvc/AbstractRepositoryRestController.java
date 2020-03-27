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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.util.Assert;
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

	/**
	 * Creates a new {@link AbstractRepositoryRestController} for the given {@link PagedResourcesAssembler} and
	 * {@link AuditableBeanWrapperFactory}.
	 *
	 * @param pagedResourcesAssembler must not be {@literal null}.
	 */
	public AbstractRepositoryRestController(PagedResourcesAssembler<Object> pagedResourcesAssembler) {

		Assert.notNull(pagedResourcesAssembler, "PagedResourcesAssembler must not be null!");

		this.pagedResourcesAssembler = pagedResourcesAssembler;
	}

	protected Link resourceLink(RootResourceInformation resourceLink, EntityModel resource) {

		ResourceMetadata repoMapping = resourceLink.getResourceMetadata();

		Link selfLink = resource.getRequiredLink(IanaLinkRelations.SELF);
		LinkRelation rel = repoMapping.getItemResourceRel();

		return Link.of(selfLink.getHref(), rel);
	}

	@SuppressWarnings({ "unchecked" })
	protected CollectionModel<?> toCollectionModel(Iterable<?> source, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Optional<Link> baseLink) {

		if (source instanceof Page) {
			Page<Object> page = (Page<Object>) source;
			return entitiesToResources(page, assembler, domainType, baseLink);
		} else if (source instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) source, assembler, domainType);
		} else {
			return CollectionModel.empty();
		}
	}

	protected CollectionModel<?> entitiesToResources(Page<Object> page, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Optional<Link> baseLink) {

		if (page.getContent().isEmpty()) {
			return baseLink.<PagedModel<?>> map(it -> pagedResourcesAssembler.toEmptyModel(page, domainType, it))//
					.orElseGet(() -> pagedResourcesAssembler.toEmptyModel(page, domainType));
		}

		return baseLink.map(it -> pagedResourcesAssembler.toModel(page, assembler, it))//
				.orElseGet(() -> pagedResourcesAssembler.toModel(page, assembler));
	}

	protected CollectionModel<?> entitiesToResources(Iterable<Object> entities,
			PersistentEntityResourceAssembler assembler, Class<?> domainType) {

		if (!entities.iterator().hasNext()) {

			List<Object> content = Arrays.<Object> asList(WRAPPERS.emptyCollectionOf(domainType));
			return CollectionModel.of(content, getDefaultSelfLink());
		}

		List<EntityModel<Object>> resources = new ArrayList<EntityModel<Object>>();

		for (Object obj : entities) {
			resources.add(obj == null ? null : assembler.toModel(obj));
		}

		return CollectionModel.of(resources, getDefaultSelfLink());
	}

	protected Link getDefaultSelfLink() {
		return Link.of(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
	}
}
