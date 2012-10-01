package org.springframework.data.rest.webmvc;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.json.JsonSchemaController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
public class BaseUriMethodArgumentResolver implements HandlerMethodArgumentResolver {

  private RepositoryRestConfiguration config;

  public BaseUriMethodArgumentResolver(RepositoryRestConfiguration config) {
    this.config = config;
  }

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return (RepositoryRestController.class.isAssignableFrom(parameter.getDeclaringClass())
        || JsonSchemaController.class.isAssignableFrom(parameter.getDeclaringClass()))
        && parameter.getParameterType() == URI.class;
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);

    // Use configured URI if there is one or set the current one as the default if not.
    if(null == config.getBaseUri()) {
      URI baseUri = ServletUriComponentsBuilder.fromServletMapping(servletRequest).build().toUri();
      config.setBaseUri(baseUri);
    }

    return config.getBaseUri();
  }

}
