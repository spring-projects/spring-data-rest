package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.jpa.JpaRepositoryExporter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Configuration
@EnableWebMvc
@ImportResource("classpath*:META-INF/spring-data-rest/**/*-export.xml")
public class RepositoryRestMvcConfiguration extends WebMvcConfigurerAdapter {

  ContentNegotiatingViewResolver viewResolver;
  RepositoryRestController repositoryRestController;

  @Autowired
  EntityManagerFactory entityManagerFactory;

  @Autowired(required = false)
  JpaRepositoryExporter customJpaRepositoryExporter;

  @Autowired(required = false)
  List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<HttpMessageConverter<?>>();

  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new ServerHttpRequestMethodArgumentResolver());
    argumentResolvers.add(new PagingAndSortingMethodArgumentResolver());
  }

  @Override public void addInterceptors(InterceptorRegistry registry) {
    registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor());
  }

  @Bean List<HttpMessageConverter<?>> httpMessageConverters() {
    Assert.notNull(httpMessageConverters);

    if (httpMessageConverters.isEmpty()) {
      MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
      json.setSupportedMediaTypes(
          Arrays.asList(MediaType.APPLICATION_JSON, MediaType.valueOf("application/x-spring-data+json"))
      );
      httpMessageConverters.add(json);
    }

    return httpMessageConverters;
  }

  @Bean PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  @Bean OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
    OpenEntityManagerInViewInterceptor oemiv = new OpenEntityManagerInViewInterceptor();
    oemiv.setEntityManagerFactory(entityManagerFactory);
    return oemiv;
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
    if (null == viewResolver) {
      viewResolver = new ContentNegotiatingViewResolver();

      Map<String, String> mediaTypes = new HashMap<String, String>() {{
        put("json", "application/json");
        put("urilist", "text/uri-list");
      }};
      viewResolver.setMediaTypes(mediaTypes);

      RepositoryRestViewResolver jsonvr = new RepositoryRestViewResolver(jsonView());
      RepositoryRestViewResolver urilistvr = new RepositoryRestViewResolver(urilistView());
      viewResolver.setViewResolvers(Arrays.<ViewResolver>asList(jsonvr, urilistvr));
    }
    return viewResolver;
  }

  @Bean RepositoryRestController repositoryRestController() throws Exception {
    if (null == repositoryRestController) {
      this.repositoryRestController = new RepositoryRestController()
          .repositoryExporters(Arrays.<RepositoryExporter>asList(jpaRepositoryExporter()))
          .httpMessageConverters(httpMessageConverters())
          .jsonMediaType("application/json");
    }
    return repositoryRestController;
  }

}
