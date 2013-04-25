package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.PagingAndSorting;
import org.springframework.data.rest.repository.invoke.RepositoryMethod;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 */
@Controller
@RequestMapping("/{repository}/search")
public class RepositorySearchController extends AbstractRepositoryRestController {

	public RepositorySearchController(Repositories repositories,
	                                  RepositoryRestConfiguration config,
	                                  DomainClassConverter domainClassConverter,
	                                  ConversionService conversionService,
	                                  EntityLinks entityLinks) {
		super(repositories,
		      config,
		      domainClassConverter,
		      conversionService,
		      entityLinks);
	}

	@RequestMapping(
			method = RequestMethod.GET,
			produces = {
					"application/json",
					"application/x-spring-data-compact+json"
			}
	)
	@ResponseBody
	public Resource<?> list(RepositoryRestRequest repoRequest) throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();
		links.addAll(queryMethodLinks(repoRequest.getBaseUri(),
		                              repoRequest.getPersistentEntity().getType()));
		if(links.isEmpty()) {
			throw new ResourceNotFoundException();
		}
		return new Resource<Object>(Collections.emptyList(), links);
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			value = "/{method}",
			method = RequestMethod.GET,
			produces = {
					"application/json",
					"application/x-spring-data-verbose+json"
			}
	)
	@ResponseBody
	public ResourceSupport query(final RepositoryRestRequest repoRequest,
	                             @PathVariable String repository,
	                             @PathVariable String method)
			throws ResourceNotFoundException {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if(repoMethodInvoker.getQueryMethods().isEmpty()) {
			throw new ResourceNotFoundException();
		}

		ResourceMapping repoMapping = repoRequest.getRepositoryResourceMapping();
		String methodName = repoMapping.getNameForPath(method);
		RepositoryMethod repoMethod = repoMethodInvoker.getQueryMethods().get(methodName);
		if(null == repoMethod) {
			for(RepositoryMethod queryMethod : repoMethodInvoker.getQueryMethods().values()) {
				String path = findPath(queryMethod.getMethod());
				if(path.equals(method)) {
					repoMethod = queryMethod;
					break;
				}
			}
			if(null == repoMethod) {
				throw new ResourceNotFoundException();
			}
		}

		PagingAndSorting pageSort = repoRequest.getPagingAndSorting();
		List<MethodParameter> methodParams = repoMethod.getParameters();
		Object[] paramValues = new Object[methodParams.size()];
		if(!methodParams.isEmpty()) {
			for(int i = 0; i < paramValues.length; i++) {
				MethodParameter param = methodParams.get(i);
				if(Pageable.class.isAssignableFrom(param.getParameterType())) {
					paramValues[i] = new PageRequest(pageSort.getPageNumber(),
					                                 pageSort.getPageSize(),
					                                 pageSort.getSort());
				} else if(Sort.class.isAssignableFrom(param.getParameterType())) {
					paramValues[i] = pageSort.getSort();
				} else {
					String paramName = repoMethod.getParameterNames().get(i);
					String[] queryParamVals = repoRequest.getRequest().getParameterValues(paramName);
					if(null == queryParamVals) {
						if(paramName.startsWith("arg")) {
							throw new IllegalArgumentException("No @Param annotation found on query method "
									                                   + repoMethod.getMethod().getName()
									                                   + " for parameter " + param.getParameterName());
						} else {
							throw new IllegalArgumentException("No query parameter specified for "
									                                   + repoMethod.getMethod().getName() + " param '"
									                                   + paramName + "'");
						}
					}
					paramValues[i] = methodParameterConversionService.convert(queryParamVals, param);
				}
			}
		}

		Object result = repoMethodInvoker.invokeQueryMethod(repoMethod, paramValues);
		Link prevLink = null;
		Link nextLink = null;
		if(result instanceof Page) {
			if(((Page)result).hasPreviousPage() && pageSort.getPageNumber() > 0) {
				prevLink = searchLink(repoRequest, 0, method, "page.previous");
			}
			if(((Page)result).hasNextPage()) {
				nextLink = searchLink(repoRequest, 1, method, "page.next");
			}
		}
		return resultToResources(repoRequest, result, new ArrayList<Link>(), prevLink, nextLink);
	}

	@RequestMapping(
			value = "/{method}",
			method = RequestMethod.GET,
			produces = {
					"application/x-spring-data-compact+json"
			}
	)
	@ResponseBody
	public ResourceSupport queryCompact(RepositoryRestRequest repoRequest,
	                                    @PathVariable String repository,
	                                    @PathVariable String method)
			throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();

		ResourceSupport resource = query(repoRequest, repository, method);
		links.addAll(resource.getLinks());

		if(resource instanceof Resources && ((Resources)resource).getContent() != null) {
			for(Object obj : ((Resources)resource).getContent()) {
				if(null != obj && obj instanceof Resource) {
					Resource res = (Resource)obj;
					links.add(resourceLink(repoRequest, res));
				}
			}
		} else if(resource instanceof Resource) {
			Resource res = (Resource)resource;
			links.add(resourceLink(repoRequest, res));
		}


		return new Resource<Object>(EMPTY_RESOURCE_LIST, links);
	}

}
