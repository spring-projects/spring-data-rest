package org.springframework.data.rest.webmvc;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.support.PagingAndSorting;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestRequestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Autowired
  private RepositoryRestConfiguration                        config;
  @Autowired
  private Repositories                                       repositories;
  @Autowired
  private RepositoryInformationHandlerMethodArgumentResolver repoInfoResolver;
  @Autowired
  private PagingAndSortingMethodArgumentResolver             pagingAndSortingResolver;
  @Autowired
  private BaseUriMethodArgumentResolver                      baseUriResolver;

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return RepositoryRestRequest.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    PagingAndSorting pagingAndSorting = (PagingAndSorting)pagingAndSortingResolver.resolveArgument(parameter,
                                                                                                   mavContainer,
                                                                                                   webRequest,
                                                                                                   binderFactory);
    RepositoryInformation repoInfo = (RepositoryInformation)repoInfoResolver.resolveArgument(parameter,
                                                                                             mavContainer,
                                                                                             webRequest,
                                                                                             binderFactory);
    URI baseUri = (URI)baseUriResolver.resolveArgument(parameter,
                                                       mavContainer,
                                                       webRequest,
                                                       binderFactory);

    return new RepositoryRestRequest(config,
                                     repositories,
                                     webRequest.getNativeRequest(HttpServletRequest.class),
                                     pagingAndSorting,
                                     baseUri,
                                     repoInfo);
  }

}
