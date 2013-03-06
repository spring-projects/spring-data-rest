package org.springframework.data.rest.webmvc;

import static java.util.Collections.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jon Brisbin
 */
@Controller
@RequestMapping("/")
public class RepositoryController extends AbstractRepositoryRestController {

	@Autowired
	public RepositoryController(Repositories repositories,
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
	public Resource<?> listRepositories()
			throws ResourceNotFoundException {
		Resource<?> links = new Resource<Object>(emptyList());
		for(Class<?> domainType : repositories) {
			links.add(entityLinks.linkToCollectionResource(domainType));
		}
		return links;
	}

}
