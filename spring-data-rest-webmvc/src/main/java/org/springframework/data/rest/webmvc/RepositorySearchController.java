/*
 * Copyright 2013-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.MethodResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to lookup and execute searches on a given repository.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryRestController
class RepositorySearchController extends AbstractRepositoryRestController {

	private static final String SEARCH = "/search";
	private static final String BASE_MAPPING = "/{repository}" + SEARCH;
	private static final String PARAMETER_NAME_TEMPALTE_PATTERN = "{?%s}";

	private final EntityLinks entityLinks;
	private final ResourceMappings mappings;
	private final PagedResourcesAssembler<Object> assembler;

	/**
	 * Creates a new {@link RepositorySearchController} using the given {@link PagedResourcesAssembler},
	 * {@link PersistentEntityResourceAssembler}, {@link EntityLinks} and {@link ResourceMappings}.
	 * 
	 * @param assembler must not be {@literal null}.
	 * @param perAssembler must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	@Autowired
	public RepositorySearchController(PagedResourcesAssembler<Object> assembler,
			PersistentEntityResourceAssembler<Object> perAssembler, EntityLinks entityLinks, ResourceMappings mappings) {

		super(assembler, perAssembler);

		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.entityLinks = entityLinks;
		this.mappings = mappings;
		this.assembler = assembler;
	}

	/**
	 * Exposes links to the individual search resources exposed by the backing repository.
	 * 
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public ResourceSupport listSearches(RepositoryRestRequest request) {

		SearchResourceMappings resourceMappings = request.getSearchMappings();

		if (!resourceMappings.isExported()) {
			throw new ResourceNotFoundException();
		}

		Links queryMethodLinks = getSearchLinks(request.getDomainType());

		if (queryMethodLinks.isEmpty()) {
			throw new ResourceNotFoundException();
		}

		ResourceSupport result = new ResourceSupport();
		result.add(queryMethodLinks);

		return result;
	}

	/**
	 * Executes the search with the given name.
	 * 
	 * @param request
	 * @param repository
	 * @param search
	 * @param pageable
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING + "/{search}", method = RequestMethod.GET)
	public ResponseEntity<Resources<?>> executeSearch(RepositoryRestRequest request, @PathVariable String search,
			Pageable pageable) {

		Method method = checkExecutability(request, search);
		Resources<?> resources = executeQueryMethod(request, method, pageable);

		return new ResponseEntity<Resources<?>>(resources, HttpStatus.OK);
	}

	/**
	 * Executes a query method and exposes the results in compact form.
	 * 
	 * @param repoRequest
	 * @param repository
	 * @param method
	 * @param pageable
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING + "/{method}", method = RequestMethod.GET, //
			produces = { "application/x-spring-data-compact+json" })
	public ResourceSupport executeSearchCompact(RepositoryRestRequest repoRequest, @PathVariable String repository,
			@PathVariable String search, Pageable pageable) {

		Method method = checkExecutability(repoRequest, search);
		ResourceSupport resource = executeQueryMethod(repoRequest, method, pageable);

		List<Link> links = new ArrayList<Link>();

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

	/**
	 * Checks that the given request is actually executable. Will reject execution if we don't find a search with the
	 * given name.
	 * 
	 * @param request
	 * @param searchName
	 * @return
	 */
	private Method checkExecutability(RepositoryRestRequest request, String searchName) {

		ResourceMetadata metadata = request.getResourceMetadata();
		SearchResourceMappings searchMapping = metadata.getSearchResourceMappings();

		if (!searchMapping.isExported()) {
			throw new ResourceNotFoundException();
		}

		Method method = searchMapping.getMappedMethod(searchName);

		if (method == null) {
			throw new ResourceNotFoundException();
		}

		return method;
	}

	/**
	 * @param repoRequest
	 * @param method
	 * @param pageable
	 * @return
	 */
	private Resources<?> executeQueryMethod(final RepositoryRestRequest repoRequest, Method method, Pageable pageable) {

		RepositoryInvoker repoMethodInvoker = repoRequest.getRepositoryInvoker();
		Map<String, String[]> parameters = repoRequest.getRequest().getParameterMap();
		Object result = repoMethodInvoker.invokeQueryMethod(method, parameters, pageable, null);

		return resultToResources(result);
	}

	/**
	 * Returns {@link Links} to the individual searches exposed.
	 * 
	 * @param domainType the domain type we want to obtain the search links for.
	 * @return
	 */
	private Links getSearchLinks(Class<?> domainType) {

		List<Link> links = new ArrayList<Link>();

		SearchResourceMappings searchMappings = mappings.getSearchResourceMappings(domainType);
		LinkBuilder builder = entityLinks.linkFor(domainType).slash(searchMappings.getPath());

		for (MethodResourceMapping mapping : searchMappings) {

			if (!mapping.isExported()) {
				continue;
			}

			String parameterTemplateVariable = getParameterTemplateVariable(mapping.getParameterNames());
			String href = builder.slash(mapping.getPath()).toString().concat(parameterTemplateVariable);

			Link link = new Link(href, mapping.getRel());

			if (mapping.isPagingResource()) {
				link = assembler.appendPaginationParameterTemplates(link);
			}

			links.add(link);
		}

		return new Links(links);
	}

	private static String getParameterTemplateVariable(Collection<String> parameters) {
		String parameterString = StringUtils.collectionToCommaDelimitedString(parameters);
		return parameters.isEmpty() ? "" : String.format(PARAMETER_NAME_TEMPALTE_PATTERN, parameterString);
	}
}
