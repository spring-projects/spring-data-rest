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
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.data.rest.repository.support.ResourceMappingUtils.getResourceMapping;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RestController
@SuppressWarnings("deprecation")
public class RepositoryController extends AbstractRepositoryRestController {

	private final Repositories repositories;
	private final RepositoryRestConfiguration config;
	private final EntityLinks entityLinks;

	@Autowired
	public RepositoryController(Repositories repositories, RepositoryRestConfiguration config, EntityLinks entityLinks,
			PagedResourcesAssembler<Object> assembler, PersistentEntityResourceAssembler<Object> perAssembler) {
		
		super(assembler, perAssembler);

		this.repositories = repositories;
		this.config = config;
		this.entityLinks = entityLinks;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET, //
			produces = { "application/json", "application/x-spring-data-compact+json" })
	@ResponseBody
	public RepositoryLinksResource listRepositories() throws ResourceNotFoundException {
		RepositoryLinksResource resource = new RepositoryLinksResource();
		for (Class<?> domainType : repositories) {
			ResourceMapping repoMapping = getResourceMapping(config, repositories.getRepositoryInformationFor(domainType));
			if (repoMapping.isExported()) {
				resource.add(entityLinks.linkToCollectionResource(domainType));
			}
		}
		return resource;
	}
}
