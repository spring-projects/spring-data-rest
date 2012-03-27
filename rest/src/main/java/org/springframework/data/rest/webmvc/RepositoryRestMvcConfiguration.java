package org.springframework.data.rest.webmvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@Configuration
public class RepositoryRestMvcConfiguration {

  @Autowired
  RepositoryRestConfiguration repositoryRestConfiguration;
  RepositoryRestController repositoryRestController;

  @Bean ContentNegotiatingViewResolver contentNegotiatingViewResolver() {
    ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();
    Map<String, String> jsonTypes = new HashMap<String, String>() {{
      put("json", "application/json");
      put("sdjson", "application/x-spring-data+json");
      put("urilist", "text/uri-list");
    }};

    viewResolver.setMediaTypes(jsonTypes);
    viewResolver.setDefaultViews(
        Arrays.asList((View) new JsonView("application/json"),
                      (View) new JsonView("application/x-spring-data+json"),
                      (View) new UriListView())
    );
    return viewResolver;
  }

  @Bean RepositoryRestController repositoryRestController() throws Exception {
    if (null == repositoryRestController) {
      this.repositoryRestController = new RepositoryRestController()
          .baseUri(repositoryRestConfiguration.baseUri())
          .repositoryMetadata(repositoryRestConfiguration.jpaRepositoryMetadata())
          .conversionService(repositoryRestConfiguration.conversionService())
          .httpMessageConverters(repositoryRestConfiguration.httpMessageConverters())
          .jsonMediaType("application/json");
    }
    return repositoryRestController;
  }

  @Bean RequestMappingHandlerMapping handlerMapping() {
    return new RequestMappingHandlerMapping();
  }

  @Bean RequestMappingHandlerAdapter handlerAdapter() {
    RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
    handlerAdapter.setCustomArgumentResolvers(
        Arrays.asList((HandlerMethodArgumentResolver) new ServerHttpRequestMethodArgumentResolver())
    );
    return handlerAdapter;
  }

  @Bean ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
    return new ExceptionHandlerExceptionResolver();
  }

  @Bean DefaultHandlerExceptionResolver handlerExceptionResolver() {
    return new DefaultHandlerExceptionResolver();
  }

  @Bean ResponseStatusExceptionResolver responseStatusExceptionResolver() {
    return new ResponseStatusExceptionResolver();
  }

}
