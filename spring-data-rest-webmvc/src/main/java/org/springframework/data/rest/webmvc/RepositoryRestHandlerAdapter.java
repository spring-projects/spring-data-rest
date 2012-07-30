package org.springframework.data.rest.webmvc;

import java.util.Arrays;

import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestHandlerAdapter extends RequestMappingHandlerAdapter {

  public RepositoryRestHandlerAdapter(RepositoryRestConfiguration config) {
    setCustomArgumentResolvers(Arrays.asList(
        new ServerHttpRequestMethodArgumentResolver(),
        new PagingAndSortingMethodArgumentResolver(config)
    ));
  }

  @Override public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override protected boolean supportsInternal(HandlerMethod handlerMethod) {
    return super.supportsInternal(handlerMethod)
        && RepositoryRestController.class.isAssignableFrom(handlerMethod.getBeanType());
  }

}
