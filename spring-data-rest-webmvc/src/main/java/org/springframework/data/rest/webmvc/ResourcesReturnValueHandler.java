package org.springframework.data.rest.webmvc;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.core.ResourceSet;
import org.springframework.data.rest.repository.RepositoryExporterSupport;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodReturnValueHandler} implementation that applies user-defined post-processors to the
 * representation being sent back to the client.
 *
 * @author Jon Brisbin
 */
public class ResourcesReturnValueHandler
    extends RepositoryExporterSupport<ResourcesReturnValueHandler>
    implements HandlerMethodReturnValueHandler {

  private RepositoryRestConfiguration config = RepositoryRestConfiguration.DEFAULT;

  public ResourcesReturnValueHandler(RepositoryRestConfiguration config) {
    if(null != config) {
      this.config = config;
    } else {
      this.config = config;
    }
  }

  @Override public boolean supportsReturnType(MethodParameter returnType) {
    return ResourceSet.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public void handleReturnValue(Object returnValue,
                                MethodParameter returnType,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest) throws Exception {
    ResourceSet resources = (ResourceSet)returnValue;

  }

}
