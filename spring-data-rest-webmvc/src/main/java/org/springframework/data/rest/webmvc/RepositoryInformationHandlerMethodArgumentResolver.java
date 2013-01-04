package org.springframework.data.rest.webmvc;

import static org.springframework.util.ClassUtils.*;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.repository.support.RepositoryInformationSupport;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Jon Brisbin
 */
public class RepositoryInformationHandlerMethodArgumentResolver
    extends RepositoryInformationSupport
    implements HandlerMethodArgumentResolver {

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return isAssignable(parameter.getParameterType(), RepositoryInformation.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    String requestUri = new UrlPathHelper().getLookupPathForRequest(request);
    if(requestUri.startsWith("/")) {
      requestUri = requestUri.substring(1);
    }

    String[] parts = requestUri.split("/");
    if(parts.length == 0) {
      // Root request
      return null;
    }

    return findRepositoryInfoFor(parts[0]);
  }


}
