/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryRestController
public class RepositoryController extends AbstractRepositoryRestController {

	private final Repositories repositories;
	private final EntityLinks entityLinks;
	private final ResourceMappings mappings;

	@Autowired
	public RepositoryController(PagedResourcesAssembler<Object> assembler,
			PersistentEntityResourceAssembler<Object> perAssembler, Repositories repositories, EntityLinks entityLinks,
			ResourceMappings mappings) {

		super(assembler, perAssembler);

		this.repositories = repositories;
		this.entityLinks = entityLinks;
		this.mappings = mappings;
	}

	/**
	 * Lists all repositories exported by creating a link list pointing to resources exposing the repositories.
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public RepositoryLinksResource listRepositories() {

		RepositoryLinksResource resource = new RepositoryLinksResource();

		for (Class<?> domainType : repositories) {

			ResourceMetadata metadata = mappings.getMappingFor(domainType);
			if (metadata.isExported()) {
				resource.add(entityLinks.linkToCollectionResource(domainType));
			}
		}

		return resource;
	}
}
