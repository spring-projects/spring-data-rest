package org.springframework.data.rest.webmvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link RequestMappingHandlerAdapter} implementation that adds a couple argument resolvers for controller method
 * parameters used in the REST exporter controller. Also only looks for handler methods in the Spring Data REST
 * provided controller classes to help isolate this handler adapter from other handler adapters the user might have
 * configured in their Spring MVC context.
 *
 * @author Jon Brisbin
 */
public class RepositoryRestHandlerAdapter extends ResourceProcessorInvokingHandlerAdapter {

  @Autowired
  private List<HandlerMethodArgumentResolver> argumentResolvers;

  @Override public void afterPropertiesSet() {
    setCustomArgumentResolvers(argumentResolvers);
    super.afterPropertiesSet();
  }

  @Override public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override protected boolean supportsInternal(HandlerMethod handlerMethod) {
    Class<?> controllerType = handlerMethod.getBeanType();
    return super.supportsInternal(handlerMethod)
        && (RepositoryController.class.isAssignableFrom(controllerType)
        || RepositoryEntityController.class.isAssignableFrom(controllerType)
        || RepositoryPropertyReferenceController.class.isAssignableFrom(controllerType)
        || RepositorySearchController.class.isAssignableFrom(controllerType));
  }

}
