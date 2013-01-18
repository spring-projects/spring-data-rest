package org.springframework.data.rest.webmvc;

import static java.util.Collections.*;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.support.RepositoryEntityLinks;
import org.springframework.data.rest.webmvc.support.JsonpResponse;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
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

  public RepositoryController(Repositories repositories,
                              RepositoryRestConfiguration config,
                              DomainClassConverter domainClassConverter,
                              ConversionService conversionService) {
    super(repositories, config, domainClassConverter, conversionService);
  }

  @RequestMapping(
      method = RequestMethod.GET,
      produces = {
          "application/json",
          "application/x-spring-data-compact+json"
      }
  )
  @ResponseBody
  public Resource<?> listRepositories(RepositoryRestRequest repoRequest)
      throws ResourceNotFoundException {
    EntityLinks linkBuilder = new RepositoryEntityLinks(repoRequest.getBaseUri(),
                                                        repositories,
                                                        config);
    Resource<?> links = new Resource<Object>(emptyList());
    for(Class<?> domainType : repositories) {
      links.add(linkBuilder.linkToCollectionResource(domainType));
    }
    return links;
  }

  @RequestMapping(
      method = RequestMethod.GET,
      produces = {
          "application/javascript"
      }
  )
  @ResponseBody
  public JsonpResponse<? extends Resource<?>> jsonpListRepositories(RepositoryRestRequest repoRequest)
      throws ResourceNotFoundException {
    return jsonpWrapResponse(repoRequest, listRepositories(repoRequest), HttpStatus.OK);
  }

}
