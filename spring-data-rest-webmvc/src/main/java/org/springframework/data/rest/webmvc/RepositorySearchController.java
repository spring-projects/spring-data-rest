/*
 * Copyright 2013-2020 the original author or authors.
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

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.MethodResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.core.AnnotationAttribute;
import org.springframework.hateoas.server.core.MethodParameters;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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

	private final RepositoryEntityLinks entityLinks;
	private final ResourceMappings mappings;
	private ResourceStatus resourceStatus;

	/**
	 * Creates a new {@link RepositorySearchController} using the given {@link PagedResourcesAssembler},
	 * {@link EntityLinks} and {@link ResourceMappings}.
	 *
	 * @param assembler must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	@Autowired
	public RepositorySearchController(PagedResourcesAssembler<Object> assembler, RepositoryEntityLinks entityLinks,
			ResourceMappings mappings, HttpHeadersPreparer headersPreparer) {

		super(assembler);

		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.entityLinks = entityLinks;
		this.mappings = mappings;
		this.resourceStatus = ResourceStatus.of(headersPreparer);
	}

	/**
	 * <code>OPTIONS /{repository}/search</code>.
	 *
	 * @param resourceInformation
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.OPTIONS)
	public HttpEntity<?> optionsForSearches(RootResourceInformation resourceInformation) {

		verifySearchesExposed(resourceInformation);

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(Collections.singleton(HttpMethod.GET));

		return ResponseEntity.ok().headers(headers).build();
	}

	/**
	 * <code>HEAD /{repository}/search</code> - Checks whether the search resource is present.
	 *
	 * @param resourceInformation
	 * @return
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.HEAD)
	public HttpEntity<?> headForSearches(RootResourceInformation resourceInformation) {

		verifySearchesExposed(resourceInformation);

		return ResponseEntity.noContent().build();
	}

	/**
	 * <code>GET /{repository}/search</code> - Exposes links to the individual search resources exposed by the backing
	 * repository.
	 *
	 * @param resourceInformation
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public RepositorySearchesResource listSearches(RootResourceInformation resourceInformation) {

		verifySearchesExposed(resourceInformation);

		Links queryMethodLinks = entityLinks.linksToSearchResources(resourceInformation.getDomainType());

		if (queryMethodLinks.isEmpty()) {
			throw new ResourceNotFoundException();
		}

		return new RepositorySearchesResource(resourceInformation.getDomainType()) //
				.add(queryMethodLinks) //
				.add(getDefaultSelfLink());
	}

	/**
	 * Executes the search with the given name.
	 *
	 * @param resourceInformation
	 * @param parameters
	 * @param search
	 * @param pageable
	 * @param sort
	 * @param assembler
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING + "/{search}", method = RequestMethod.GET)
	public ResponseEntity<?> executeSearch(RootResourceInformation resourceInformation,
			@RequestParam MultiValueMap<String, Object> parameters, @PathVariable String search, DefaultedPageable pageable,
			Sort sort, PersistentEntityResourceAssembler assembler, @RequestHeader HttpHeaders headers) {

		Method method = checkExecutability(resourceInformation, search);
		Optional<Object> result = executeQueryMethod(resourceInformation.getInvoker(), parameters, method, pageable, sort,
				assembler);

		SearchResourceMappings searchMappings = resourceInformation.getSearchMappings();
		MethodResourceMapping methodMapping = searchMappings.getExportedMethodMappingForPath(search);
		Class<?> domainType = methodMapping.getReturnedDomainType();

		return toModel(result, assembler, domainType, Optional.empty(), headers, resourceInformation);
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
	protected ResponseEntity<?> toModel(Optional<Object> source, final PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Optional<Link> baseLink, HttpHeaders headers, RootResourceInformation information) {

		return source.map(it -> {

			if (it instanceof Iterable) {
				return ResponseEntity.ok(toCollectionModel((Iterable<?>) it, assembler, domainType, baseLink));
			} else if (ClassUtils.isPrimitiveOrWrapper(it.getClass())) {
				return ResponseEntity.ok(it);
			}

			PersistentEntity<?, ?> entity = information.getPersistentEntity();

			// Returned value is not of the aggregates type - probably some projection
			if (!entity.getType().isInstance(it)) {
				return ResponseEntity.ok(it);
			}

			return resourceStatus.getStatusAndHeaders(headers, it, entity).toResponseEntity(//
					() -> assembler.toFullResource(it));

		}).orElseThrow(() -> new ResourceNotFoundException());
	}

	/**
	 * Executes a query method and exposes the results in compact form.
	 *
	 * @param resourceInformation
	 * @param parameters
	 * @param repository
	 * @param search
	 * @param pageable
	 * @param sort
	 * @param assembler
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = BASE_MAPPING + "/{search}", method = RequestMethod.GET, //
			produces = { "application/x-spring-data-compact+json" })
	public RepresentationModel<?> executeSearchCompact(RootResourceInformation resourceInformation,
			@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, Object> parameters,
			@PathVariable String repository, @PathVariable String search, DefaultedPageable pageable, Sort sort,
			PersistentEntityResourceAssembler assembler) {

		Method method = checkExecutability(resourceInformation, search);
		Optional<Object> result = executeQueryMethod(resourceInformation.getInvoker(), parameters, method, pageable, sort,
				assembler);
		ResourceMetadata metadata = resourceInformation.getResourceMetadata();
		ResponseEntity<?> entity = toModel(result, assembler, metadata.getDomainType(), Optional.empty(), headers,
				resourceInformation);
		Object resource = entity.getBody();

		List<Link> links = new ArrayList<Link>();

		if (resource instanceof CollectionModel && ((CollectionModel<?>) resource).getContent() != null) {

			for (Object obj : ((CollectionModel<?>) resource).getContent()) {
				if (null != obj && obj instanceof EntityModel) {
					EntityModel<?> res = (EntityModel<?>) obj;
					links.add(resourceLink(resourceInformation, res));
				}
			}

		} else if (resource instanceof EntityModel) {

			EntityModel<?> res = (EntityModel<?>) resource;
			links.add(resourceLink(resourceInformation, res));
		}

		return CollectionModel.empty(links);
	}

	/**
	 * <code>OPTIONS /{repository}/search/{search}</code>.
	 *
	 * @param information
	 * @param search
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING + "/{search}", method = RequestMethod.OPTIONS)
	public ResponseEntity<Object> optionsForSearch(RootResourceInformation information, @PathVariable String search) {

		checkExecutability(information, search);

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(Collections.singleton(HttpMethod.GET));

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * Handles a {@code HEAD} request for individual searches.
	 *
	 * @param information
	 * @param search
	 * @return
	 * @since 2.2
	 */
	@RequestMapping(value = BASE_MAPPING + "/{search}", method = RequestMethod.HEAD)
	public ResponseEntity<Object> headForSearch(RootResourceInformation information, @PathVariable String search) {

		checkExecutability(information, search);
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	/**
	 * Checks that the given request is actually executable. Will reject execution if we don't find a search with the
	 * given name.
	 *
	 * @param resourceInformation
	 * @param searchName
	 * @return
	 */
	private Method checkExecutability(RootResourceInformation resourceInformation, String searchName) {

		SearchResourceMappings searchMapping = verifySearchesExposed(resourceInformation);

		Method method = searchMapping.getMappedMethod(searchName);

		if (method == null) {
			throw new ResourceNotFoundException();
		}

		return method;
	}

	/**
	 * @param invoker
	 * @param request
	 * @param method
	 * @param pageable
	 * @return
	 */
	private Optional<Object> executeQueryMethod(final RepositoryInvoker invoker,
			@RequestParam MultiValueMap<String, Object> parameters, Method method, DefaultedPageable pageable, Sort sort,
			PersistentEntityResourceAssembler assembler) {

		MultiValueMap<String, Object> result = new LinkedMultiValueMap<String, Object>(parameters);
		MethodParameters methodParameters = new MethodParameters(method, new AnnotationAttribute(Param.class));
		List<MethodParameter> parameterList = methodParameters.getParameters();
		List<TypeInformation<?>> parameterTypeInformations = ClassTypeInformation.from(method.getDeclaringClass())
				.getParameterTypes(method);

		parameters.entrySet().forEach(entry ->

		methodParameters.getParameter(entry.getKey()).ifPresent(parameter -> {

			int parameterIndex = parameterList.indexOf(parameter);
			TypeInformation<?> domainType = parameterTypeInformations.get(parameterIndex).getActualType();

			ResourceMetadata metadata = mappings.getMetadataFor(domainType.getType());

			if (metadata != null && metadata.isExported()) {
				result.put(parameter.getParameterName(), prepareUris(entry.getValue()));
			}
		}));

		return invoker.invokeQueryMethod(method, result, pageable.getPageable(), sort);
	}

	/**
	 * Verifies that the given {@link RootResourceInformation} has searches exposed.
	 *
	 * @param resourceInformation
	 */
	private static SearchResourceMappings verifySearchesExposed(RootResourceInformation resourceInformation) {

		SearchResourceMappings resourceMappings = resourceInformation.getSearchMappings();

		if (!resourceMappings.isExported()) {
			throw new ResourceNotFoundException();
		}

		return resourceMappings;
	}

	/**
	 * Tries to turn all elements of the given {@link List} into URIs and falls back to keeping the original element if
	 * the conversion fails.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	private static List<Object> prepareUris(List<Object> source) {

		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}

		List<Object> result = new ArrayList<Object>(source.size());

		for (Object element : source) {

			try {
				result.add(new URI(element.toString()));
			} catch (URISyntaxException o_O) {
				result.add(element);
			}
		}

		return result;
	}
}
