package org.springframework.data.rest.webmvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.Ordered;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.jpa.JpaRepositoryExporter;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Configuration
@ImportResource("classpath*:META-INF/spring-data-rest/**/*-export.xml")
public class RepositoryRestMvcConfiguration {

  @Autowired(required = false)
  JpaRepositoryExporter customJpaRepositoryExporter;

  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  @Bean PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  @Bean JpaRepositoryExporter jpaRepositoryExporter() {
    if (null == customJpaRepositoryExporter) {
      return new JpaRepositoryExporter();
    } else {
      return customJpaRepositoryExporter;
    }
  }

  @Bean ValidatingRepositoryEventListener validatingRepositoryEventListener() {
    if (null == validatingRepositoryEventListener) {
      return new ValidatingRepositoryEventListener();
    } else {
      return validatingRepositoryEventListener;
    }
  }

  @Bean JsonView jsonView() {
    return new JsonView("application/json");
  }

  @Bean UriListView urilistView() {
    return new UriListView();
  }

  @Bean ContentNegotiatingViewResolver contentNegotiatingViewResolver() {
    ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();
    viewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);

    Map<String, String> mediaTypes = new HashMap<String, String>() {{
      put("json", "application/json");
      put("urilist", "text/uri-list");
    }};
    viewResolver.setMediaTypes(mediaTypes);

    RepositoryRestViewResolver jsonvr = new RepositoryRestViewResolver(jsonView());
    RepositoryRestViewResolver urilistvr = new RepositoryRestViewResolver(urilistView());
    viewResolver.setViewResolvers(Arrays.<ViewResolver>asList(jsonvr, urilistvr));

    return viewResolver;
  }

  @Bean RepositoryRestController repositoryRestController() throws Exception {
    return new RepositoryRestController();
  }

  @Bean RepositoryExporterHandlerAdapter repositoryExporterHandlerAdapter() {
    return new RepositoryExporterHandlerAdapter();
  }

  @Bean RepositoryExporterHandlerMapping repositoryExporterHandlerMapping() {
    return new RepositoryExporterHandlerMapping();
  }

}
