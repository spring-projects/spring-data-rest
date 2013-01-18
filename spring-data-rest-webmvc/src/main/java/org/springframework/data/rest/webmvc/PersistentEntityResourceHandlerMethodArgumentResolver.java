package org.springframework.data.rest.webmvc;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityResourceHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Autowired
  private       RepositoryRestRequestHandlerMethodArgumentResolver repoRequestResolver;
  private final List<HttpMessageConverter<?>>                      messageConverters;

  public PersistentEntityResourceHandlerMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  @Override public boolean supportsParameter(MethodParameter parameter) {
    return PersistentEntityResource.class.isAssignableFrom(parameter.getParameterType());
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    RepositoryRestRequest repoRequest = (RepositoryRestRequest)repoRequestResolver.resolveArgument(parameter,
                                                                                                   mavContainer,
                                                                                                   webRequest,
                                                                                                   binderFactory);

    final ServletServerHttpRequest request = new ServletServerHttpRequest(webRequest.getNativeRequest(HttpServletRequest.class));
    for(HttpMessageConverter converter : messageConverters) {
      Class<?> domainType = repoRequest.getPersistentEntity().getType();
      if(!converter.canRead(domainType, request.getHeaders().getContentType())) {
        continue;
      }

      Object obj = converter.read(domainType, request);
      return new PersistentEntityResource(repoRequest.getPersistentEntity(),
                                          obj);
    }

    return null;
  }

}
