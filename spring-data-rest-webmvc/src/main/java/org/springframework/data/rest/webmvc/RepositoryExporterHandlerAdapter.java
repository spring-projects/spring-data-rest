package org.springframework.data.rest.webmvc;

import java.util.Arrays;

import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * @author Jon Brisbin
 */
public class RepositoryExporterHandlerAdapter extends RequestMappingHandlerAdapter {

  public RepositoryExporterHandlerAdapter() {
    setCustomArgumentResolvers(Arrays.asList(
        new ServerHttpRequestMethodArgumentResolver(),
        new PagingAndSortingMethodArgumentResolver()
    ));

    // Add JSON converter for special Spring Data media type
    MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
    json.setSupportedMediaTypes(
        Arrays.asList(MediaType.APPLICATION_JSON, MediaType.valueOf("application/x-spring-data+json"))
    );
    getMessageConverters().add(json);
  }

  @Override public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override protected boolean supportsInternal(HandlerMethod handlerMethod) {
    return super.supportsInternal(handlerMethod)
        && RepositoryRestController.class.isAssignableFrom(handlerMethod.getBeanType());
  }

}
