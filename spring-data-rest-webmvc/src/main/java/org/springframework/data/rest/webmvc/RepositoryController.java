package org.springframework.data.rest.webmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.hateoas.EntityLinks;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.data.rest.repository.support.ResourceMappingUtils.getResourceMapping;

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
