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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Thibaud Lepretre
 */
@SuppressWarnings({ "rawtypes" })
class AbstractRepositoryRestController {

	private final PagedResourcesAssembler<Object> pagedResourcesAssembler;

	/**
	 * Creates a new {@link AbstractRepositoryRestController} for the given {@link PagedResourcesAssembler}.
	 * 
	 * @param pagedResourcesAssembler must not be {@literal null}.
	 */
	public AbstractRepositoryRestController(PagedResourcesAssembler<Object> pagedResourcesAssembler) {

		Assert.notNull(pagedResourcesAssembler, "PagedResourcesAssembler must not be null!");
		this.pagedResourcesAssembler = pagedResourcesAssembler;
	}

	protected Link resourceLink(RootResourceInformation resourceLink, Resource resource) {

		ResourceMetadata repoMapping = resourceLink.getResourceMetadata();

		Link selfLink = resource.getLink("self");
		String rel = repoMapping.getItemResourceRel();

		return new Link(selfLink.getHref(), rel);
	}

	@SuppressWarnings({ "unchecked" })
	protected Resources resultToResources(Object result, PersistentEntityResourceAssembler assembler, Link baseLink) {

		if (result instanceof Page) {
			Page<Object> page = (Page<Object>) result;
			return entitiesToResources(page, assembler, baseLink);
		} else if (result instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) result, assembler);
		} else if (null == result) {
			return new Resources(EMPTY_RESOURCE_LIST);
		} else {
			Resource<Object> resource = assembler.toFullResource(result);
			return new Resources(Collections.singletonList(resource));
		}
	}

	protected Resources<? extends Resource<Object>> entitiesToResources(Page<Object> page,
			PersistentEntityResourceAssembler assembler, Link baseLink) {
		return baseLink == null ? pagedResourcesAssembler.toResource(page, assembler) : pagedResourcesAssembler.toResource(
				page, assembler, baseLink);
	}

	protected Resources<Resource<Object>> entitiesToResources(Iterable<Object> entities,
			PersistentEntityResourceAssembler assembler) {

		List<Resource<Object>> resources = new ArrayList<Resource<Object>>();

		for (Object obj : entities) {
			resources.add(obj == null ? null : assembler.toResource(obj));
		}

		return new Resources<Resource<Object>>(resources);
	}
}
