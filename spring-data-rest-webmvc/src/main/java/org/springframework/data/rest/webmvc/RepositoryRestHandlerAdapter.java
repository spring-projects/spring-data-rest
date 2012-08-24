package org.springframework.data.rest.webmvc;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link RequestMappingHandlerAdapter} implementation that adds a couple argument resolvers for controller method
 * parameters used in the REST exporter controller. Also only looks for handler methods in the {@link
 * RepositoryRestController} class to help isolate this handler adapter from other handler adapters the user might have
 * configured in their Spring MVC context.
 *
 * @author Jon Brisbin
 */
public class RepositoryRestHandlerAdapter extends ResourceProcessorInvokingHandlerAdapter {

  @Autowired
  private ResourcesReturnValueHandler resourcesReturnValueHandler;

  public RepositoryRestHandlerAdapter(RepositoryRestConfiguration config) {
    setCustomArgumentResolvers(Arrays.asList(
        new ServerHttpRequestMethodArgumentResolver(),
        new PagingAndSortingMethodArgumentResolver(config)
    ));
    setCustomReturnValueHandlers(Arrays.<HandlerMethodReturnValueHandler>asList(
        resourcesReturnValueHandler
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
