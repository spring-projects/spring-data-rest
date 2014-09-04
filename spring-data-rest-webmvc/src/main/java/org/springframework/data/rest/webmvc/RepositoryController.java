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

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for the root resource exposing links to the repository resources.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryRestController
public class RepositoryController extends AbstractRepositoryRestController {

	private final Repositories repositories;
	private final EntityLinks entityLinks;
	private final ResourceMappings mappings;

	/**
	 * Creates a new {@link RepositoryController} for the given {@link PagedResourcesAssembler}, {@link Repositories},
	 * {@link EntityLinks} and {@link ResourceMappings}.
	 * 
	 * @param assembler must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	@Autowired
	public RepositoryController(PagedResourcesAssembler<Object> assembler, Repositories repositories,
			EntityLinks entityLinks, ResourceMappings mappings) {

		super(assembler);

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.repositories = repositories;
		this.entityLinks = entityLinks;
		this.mappings = mappings;
	}

	/**
	 * <code>OPTIONS /</code>.
	 * 
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = "/", method = RequestMethod.OPTIONS)
	public HttpEntity<?> optionsForRepositories() {

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(Collections.singleton(HttpMethod.GET));

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * <code>HEAD /</code>
	 * 
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = "/", method = RequestMethod.HEAD)
	public ResponseEntity<?> headForRepositories() {
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	/**
	 * Lists all repositories exported by creating a link list pointing to resources exposing the repositories.
	 * 
	 * @return
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public HttpEntity<RepositoryLinksResource> listRepositories() {

		RepositoryLinksResource resource = new RepositoryLinksResource();

		for (Class<?> domainType : repositories) {

			ResourceMetadata metadata = mappings.getMappingFor(domainType);
			if (metadata.isExported()) {
				resource.add(entityLinks.linkToCollectionResource(domainType));
			}
		}

		return new ResponseEntity<RepositoryLinksResource>(resource, HttpStatus.OK);
	}
}
