/*
 * Copyright 2013 the original author or authors.
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

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;
import static org.springframework.data.rest.webmvc.ControllerUtils.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.invoke.RepositoryMethod;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.repository.support.ResourceMappingUtils;
import org.springframework.data.rest.webmvc.support.BaseUriLinkBuilder;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RestController
@SuppressWarnings("deprecation")
class RepositorySearchController extends AbstractRepositoryRestController {

	private static final String BASE_MAPPING = "/{repository}/search";

	private final Repositories repositories;
	private final RepositoryRestConfiguration config;

	@Autowired
	public RepositorySearchController(Repositories repositories, RepositoryRestConfiguration config,
			PagedResourcesAssembler<Object> assembler, PersistentEntityResourceAssembler<Object> perAssembler) {

		super(assembler, perAssembler);

		this.repositories = repositories;
		this.config = config;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, produces = { "application/json",
			"application/x-spring-data-compact+json" })
	@ResponseBody
	public Resource<?> list(RepositoryRestRequest repoRequest) throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();
		links.addAll(queryMethodLinks(repoRequest.getBaseUri(), repoRequest.getPersistentEntity().getType()));
		if (links.isEmpty()) {
			throw new ResourceNotFoundException();
		}
		return new Resource<Object>(Collections.emptyList(), links);
	}

	protected List<Link> queryMethodLinks(URI baseUri, Class<?> domainType) {
		List<Link> links = new ArrayList<Link>();
		RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(domainType);
		ResourceMapping repoMapping = ResourceMappingUtils.merge(repoInfo.getRepositoryInterface(),
				config.getResourceMappingForRepository(repoInfo.getRepositoryInterface()));
		for (Method method : repoInfo.getQueryMethods()) {
			LinkBuilder linkBuilder = BaseUriLinkBuilder.create(buildUri(baseUri, repoMapping.getPath(), "search"));
			ResourceMapping methodMapping = ResourceMappingUtils.merge(method,
					repoMapping.getResourceMappingFor(method.getName()));
			if (!methodMapping.isExported()) {
				continue;
			}
			links
					.add(linkBuilder.slash(methodMapping.getPath()).withRel(repoMapping.getRel() + "." + methodMapping.getRel()));
		}
		return links;
	}

	@RequestMapping(value = BASE_MAPPING + "/{method}", method = RequestMethod.GET, produces = { "application/json",
			"application/x-spring-data-verbose+json" })
	@ResponseBody
	public ResourceSupport query(final RepositoryRestRequest repoRequest, @PathVariable String repository,
			@PathVariable String method, Pageable pageable) throws ResourceNotFoundException {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (repoMethodInvoker.getQueryMethods().isEmpty()) {
			throw new ResourceNotFoundException();
		}

		ResourceMapping repoMapping = repoRequest.getRepositoryResourceMapping();
		String methodName = repoMapping.getNameForPath(method);
		RepositoryMethod repoMethod = repoMethodInvoker.getQueryMethods().get(methodName);
		if (null == repoMethod) {
			for (RepositoryMethod queryMethod : repoMethodInvoker.getQueryMethods().values()) {
				String path = findPath(queryMethod.getMethod());
				if (path.equals(method)) {
					repoMethod = queryMethod;
					break;
				}
			}
			if (null == repoMethod) {
				throw new ResourceNotFoundException();
			}
		}

		Map<String, String[]> rawParameters = repoRequest.getRequest().getParameterMap();
		Object result = repoMethodInvoker.invokeQueryMethod(repoMethod, pageable, rawParameters);

		Link baseLink = linkTo(methodOn(RepositorySearchController.class). //
				queryCompact(repoRequest, repository, methodName, pageable)).withSelfRel();

		return resultToResources(result, baseLink);
	}

	@RequestMapping(value = BASE_MAPPING + "/{method}", method = RequestMethod.GET,
			produces = { "application/x-spring-data-compact+json" })
	@ResponseBody
	public ResourceSupport queryCompact(RepositoryRestRequest repoRequest, @PathVariable String repository,
			@PathVariable String method, Pageable pageable) throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();

		ResourceSupport resource = query(repoRequest, repository, method, pageable);
		links.addAll(resource.getLinks());

		if (resource instanceof Resources && ((Resources<?>) resource).getContent() != null) {
			for (Object obj : ((Resources<?>) resource).getContent()) {
				if (null != obj && obj instanceof Resource) {
					Resource<?> res = (Resource<?>) obj;
					links.add(resourceLink(repoRequest, res));
				}
			}
		} else if (resource instanceof Resource) {
			Resource<?> res = (Resource<?>) resource;
			links.add(resourceLink(repoRequest, res));
		}

		return new Resource<Object>(EMPTY_RESOURCE_LIST, links);
	}

}
