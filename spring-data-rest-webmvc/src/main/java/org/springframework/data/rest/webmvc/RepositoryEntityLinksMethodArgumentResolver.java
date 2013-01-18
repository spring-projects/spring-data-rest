package org.springframework.data.rest.webmvc;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.repository.support.RepositoryEntityLinks;
import org.springframework.data.rest.repository.support.RepositoryInformationSupport;
import org.springframework.hateoas.EntityLinks;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 */
public class RepositoryEntityLinksMethodArgumentResolver
    extends RepositoryInformationSupport
    implements HandlerMethodArgumentResolver {

  @Autowired
  private BaseUriMethodArgumentResolver baseUriResolver;

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return EntityLinks.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory)
      throws Exception {
    URI baseUri = (URI)baseUriResolver.resolveArgument(parameter,
                                                       mavContainer,
                                                       webRequest,
                                                       binderFactory);
    return new RepositoryEntityLinks(baseUri, repositories, config);
  }

}
