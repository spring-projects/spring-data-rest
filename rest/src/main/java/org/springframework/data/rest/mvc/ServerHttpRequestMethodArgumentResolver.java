package org.springframework.data.rest.mvc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class ServerHttpRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return ClassUtils.isAssignable(parameter.getParameterType(), ServerHttpRequest.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    return new ServletServerHttpRequest((HttpServletRequest) webRequest.getNativeRequest());
  }

}
